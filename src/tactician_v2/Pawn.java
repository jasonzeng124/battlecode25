package tactician_v2;

import battlecode.common.*;

public class Pawn {
    public static final Direction[] DIRS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
            Direction.CENTER
    };

    public static final Direction[] CARDINAL = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.CENTER
    };

    public static final byte PTRNS[][][] = {
            // Paint tower
            {
                    {1, 0, 0, 0, 1},
                    {0, 1, 0, 1, 0},
                    {0, 0, 0, 0, 0},
                    {0, 1, 0, 1, 0},
                    {1, 0, 0, 0, 1}
            },
            // Coin tower
            {
                    {0, 1, 1, 1, 0},
                    {1, 1, 0, 1, 1},
                    {1, 0, 0, 0, 1},
                    {1, 1, 0, 1, 1},
                    {0, 1, 1, 1, 0}
            }
    };

    public static MapInfo[] nearbyTiles;
    public static MapLocation origin, myLoc, closestPT;

    public static int prevDir;

    public static int[][] qClearArr = new int[60][60];
    static int qClearTime = 0;

    public static void updateNearby(RobotController rc) throws GameActionException {
        if (origin == null) {
            for (Message msg : rc.readMessages(-1)) {
                final int data = msg.getBytes();
                origin = new MapLocation(data & 63, (data >> 6) & 63);
                break;
            }
        }
        myLoc = rc.getLocation();
        nearbyTiles = rc.senseNearbyMapInfos();

        for (RobotInfo tile : rc.senseNearbyRobots()) {
            // Maintain the closest paint tower for refills
            if (tile.team == rc.getTeam() && tile.type.paintPerTurn > 0) {
                closestPT = tile.getLocation();
            }
        }
    }

    static boolean isPaintable(RobotController rc, MapLocation loc) throws GameActionException {
        if (!rc.canSenseLocation(loc)) {
            return false;
        }
        final MapInfo tile = rc.senseMapInfo(loc);
        return rc.canAttack(loc) && !tile.hasRuin() && !tile.getPaint().isEnemy();
    }

    public static PaintType defaultPattern(MapLocation loc) {
        return ((loc.x + loc.y) % 2 == 0) && (((3 * loc.x + loc.y) % 10) != 0) ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY;
    }

    static int getHash(MapLocation loc) {
        // Do something with this later
        return (loc.x + loc.y) * (loc.x + loc.y + 1) / 2 + loc.y;
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();

        double[] moveScore = new double[9];
        for (int i = 9; --i >= 0;) {
            moveScore[i] = 0;
        }

        for (MapInfo tile : nearbyTiles) {
            // Ruins are our top priority
            if (rc.getNumberTowers() < 25 && tile.hasRuin()) {
                final MapLocation loc = tile.getMapLocation();

                // Skip if there are already many workers there
                int cnt = 0;
                for (RobotInfo r : rc.senseNearbyRobots(loc, 4, rc.getTeam())) {
                    if (r.type == UnitType.SOLDIER) {
                        cnt++;
                    }
                }
                if (cnt >= 2) {
                    continue;
                }

                // Unfinished ruin
                if (!rc.canSenseRobotAtLocation(loc)) {
                    // Decide which type of tower to build
                    final int typeId = getHash(loc) % 4 == 0 ? 0 : 1;
                    final UnitType type = typeId == 1 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER;

                    // Fill in any spots in the pattern with the appropriate paint.
                    boolean filled = false;
                    for (int i = 5; --i >= 0;) {
                        for (int j = 5; --j >= 0;) {
                            final MapLocation nearbyLoc = new MapLocation(loc.x + i - 2, loc.y + j - 2);
                            if (isPaintable(rc, nearbyLoc) && rc.senseMapInfo(nearbyLoc).getPaint() != (PTRNS[typeId][i][j] == 1 ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY)) {
                                rc.attack(nearbyLoc, PTRNS[typeId][i][j] == 1);
                                rc.setIndicatorDot(nearbyLoc, 255, 0, 0);
                                filled = true;
                                break;
                            }
                        }
                        if (filled)
                            break;
                    }

                    // Complete the ruin if we can.
                    if (rc.canCompleteTowerPattern(type, loc)) {
                        rc.completeTowerPattern(type, loc);
                        rc.setTimelineMarker("Tower built", 0, 255, 0);
                        System.out.println("Built a tower at " + loc + "!");
                    }

                    // Move closer
                    moveScore[GameUtils.greedyPath(rc, myLoc, loc).ordinal()] += 15;
                } else {
                    // Finished ruin, make improvements while passing by
                    if (rc.canUpgradeTower(loc) && rc.getNumberTowers() >= 5 && rc.getChips() >= 1000) {
                        rc.upgradeTower(loc);
                    }

                    // Make sure it's surrounded by allied tiles so we can send messages
                    for (int i = 8; --i >= 0; ) {
                        final MapLocation nearbyLoc = loc.add(DIRS[i]);
                        if (isPaintable(rc, nearbyLoc) && rc.senseMapInfo(nearbyLoc).getPaint() == PaintType.EMPTY) {
                            rc.attack(nearbyLoc, defaultPattern(nearbyLoc) == PaintType.ALLY_SECONDARY);
                            break;
                        }
                    }
                }
            }
        }

        // Withdraw paint
        if (rc.isActionReady() && rc.getPaint() < 150) {
            for (RobotInfo robot : rc.senseNearbyRobots()) {
                final MapLocation loc = robot.getLocation();
                if (rc.getLocation().isWithinDistanceSquared(loc, 2) && robot.team == rc.getTeam() && robot.type.paintPerTurn > 0 && robot.getPaintAmount() >= 75) {
                    int delta = -1 * java.lang.Math.min(robot.paintAmount, 200 - rc.getPaint());
                    if (delta < 0) {
                        rc.transferPaint(loc, delta);
                    }
                }
            }
        }

        // Attack nearby enemy towers
        if (rc.isActionReady() && rc.getPaint() >= 75) {
            for (RobotInfo r : rc.senseNearbyRobots(myLoc, 9, rc.getTeam().opponent())) {
                if (r.type.isTowerType()) {
                    rc.attack(r.getLocation());
                    break;
                }
            }
        }

        // Paint nearby tiles with default color
        if (rc.isActionReady() && rc.getPaint() >= 75) {
            // Use a quick-clearing array to set banned tiles, from unfinished ruins
            qClearTime++;
            for (MapLocation loc : rc.senseNearbyRuins(-1)) {
                if (!rc.canSenseRobotAtLocation(loc)) {
                    for (int i = 5; --i >= 0;) {
                        for (int j = 5; --j >= 0;) {
                            int x = loc.x + i - 2, y = loc.y + j - 2;
                            if (x >= 0 && x < 60 && y >= 0 && y < 60) {
                                qClearArr[x][y] = qClearTime;
                            }
                        }
                    }
                }
            }

            MapLocation placed = null;
            for (MapInfo tile : nearbyTiles) {
                final MapLocation loc = tile.getMapLocation();
                if (myLoc.isWithinDistanceSquared(loc, 4)) {
                    rc.setIndicatorDot(loc, 0, 0, 255);

                    if (qClearArr[loc.x][loc.y] < qClearTime && isPaintable(rc, loc) && tile.getPaint() != defaultPattern(loc)) {
                        rc.attack(loc, defaultPattern(loc) == PaintType.ALLY_SECONDARY);
                        rc.setIndicatorDot(loc, 0, 255, 0);
                        placed = loc;
                        break;
                    }
                }
                if (placed != null)
                    break;
            }
            if (placed != null) {
                // Complete resource patterns
                for (MapInfo tile : rc.senseNearbyMapInfos(placed, 8)) {
                    final MapLocation loc = tile.getMapLocation();
                    if (((3 * loc.x + loc.y) % 10) == 0) {
                        if (rc.canCompleteResourcePattern(loc)) {
                            rc.completeResourcePattern(loc);
                        }
                    }
                }
            }
        }

        // Share the origin
        if (origin != null) {
            rc.setIndicatorDot(origin, 255, 162, 0);
            for (RobotInfo r : rc.senseNearbyRobots(myLoc, 9, rc.getTeam())) {
                if (r.type.isTowerType() && rc.canSendMessage(r.getLocation())) {
                    rc.sendMessage(r.getLocation(), origin.x | (origin.y << 6));
                }
            }
        }

        if (rc.isMovementReady()) {
            // Inertia
            moveScore[prevDir] += 5;

            // Try to move towards a paint tower if we're low
            if (closestPT != null && rc.getPaint() < 75) {
                moveScore[GameUtils.greedyPath(rc, myLoc, closestPT).ordinal()] += 50;
            }

            for (MapInfo tile : nearbyTiles) {
                final MapLocation loc = tile.getMapLocation();
                final int dir = myLoc.directionTo(loc).ordinal();
                final double dist = myLoc.distanceSquaredTo(loc);

                // Get close to enemy paint, but not onto it
                if (tile.getPaint().isEnemy()) {
                    moveScore[dir] += dist <= 2 ? -20 : +0.5;
                }

                // Prioritize empty tiles!
                if (tile.getPaint() == PaintType.EMPTY) {
                    moveScore[dir] += dist <= 2 ? -5 : +1.5;
                }

                if (rc.canSenseRobotAtLocation(loc)) {
                    final RobotInfo r = rc.senseRobotAtLocation(loc);
                    if (r.getTeam() == rc.getTeam()) {
                        switch (r.getType()) {
                            case UnitType.SOLDIER -> moveScore[dir] += curRound < 400 ? -10 : -0.5;
                            case UnitType.MOPPER -> moveScore[dir] += 2.0;
                        }
                    }
                }
            }

            // TODO: Add probabilistic choice to avoid collisions?
            int bestDir = -1;
            for (int i = 9; --i >= 0;) {
                if (rc.canMove(DIRS[i]) && (bestDir == -1 || moveScore[i] > moveScore[bestDir])) {
                    bestDir = i;
                }
            }
            if (bestDir != -1) {
                rc.move(DIRS[bestDir]);
                prevDir = bestDir;
            }
        }

        assert rc.getRoundNum() == curRound : "Soldier: Bytecode limit exceeded";
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        updateNearby(rc);
        makeAction(rc);
    }
}
