package tactician_v3;

import battlecode.common.*;

public class Settler {
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

    public static int prevDir;
    public static int prevType = -1;
    public static MapLocation prevRuinLoc;

    static boolean isPaintable(RobotController rc, MapLocation loc) throws GameActionException {
        if (!rc.canSenseLocation(loc)) {
            return false;
        }
        final MapInfo tile = rc.senseMapInfo(loc);
        return rc.canAttack(loc) && !tile.hasRuin() && !tile.getPaint().isEnemy();
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        double[] moveScore = new double[9];
        for (int i = 9; --i >= 0; ) {
            moveScore[i] = 0;
        }

        // Chain refuel from built towers
        if (prevRuinLoc != null && rc.canSenseRobotAtLocation(prevRuinLoc)) {
            final RobotInfo r = rc.senseRobotAtLocation(prevRuinLoc);
            final int amount = Math.min(r.getPaintAmount(), 200 - rc.getPaint());
            if (rc.canTransferPaint(prevRuinLoc, -amount)) {
                rc.transferPaint(prevRuinLoc, -amount);
            }
        }

        MapLocation ruinFocus = null;
        for (MapInfo tile : rc.senseNearbyMapInfos()) {
            // Ruins are our top priority
            if (rc.getNumberTowers() < 25 && tile.hasRuin()) {
                final MapLocation loc = tile.getMapLocation();
                if (prevRuinLoc == null || loc.equals(prevRuinLoc)) {
                    // Skip if there are already many workers there
                    int cnt = 0;
                    for (RobotInfo r : rc.senseNearbyRobots(loc, 4, rc.getTeam())) {
                        if (r.type == UnitType.SOLDIER) {
                            cnt++;
                        }
                    }
                    if (cnt >= 1) {
                        continue;
                    }

                    // Skip if touched by opponent
                    boolean bad = false;
                    for (int i = 5; --i >= 0; ) {
                        for (int j = 5; --j >= 0; ) {
                            final MapLocation nearbyLoc = new MapLocation(loc.x + i - 2, loc.y + j - 2);
                            if (rc.canSenseLocation(nearbyLoc) && rc.senseMapInfo(nearbyLoc).getPaint().isEnemy()) {
                                bad = true;
                                break;
                            }
                        }
                        if (bad)
                            break;
                    }
                    if (bad) {
                        continue;
                    }

                    // Unfinished ruin
                    if (!rc.canSenseRobotAtLocation(loc)) {
                        // Decide which type of tower to build
                        final double chanceOfMoney = (rc.getNumberTowers() <= 2) ? (1.0) : (0.5 - 0.001 * rc.getRoundNum());
                        final int typeId = (prevType == -1 ? ((((FastMath.rand256() / 256.0) <= (chanceOfMoney))) ? 1 : 0) : prevType);
                        final UnitType type = (typeId == 1 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER);

                        // Fill in any spots in the pattern with the appropriate paint.
                        boolean filled = false;
                        for (int i = 5; --i >= 0; ) {
                            for (int j = 5; --j >= 0; ) {
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

                        ruinFocus = loc;
                        prevType = typeId;

                        // Complete the ruin if we can.
                        if (rc.canCompleteTowerPattern(type, loc)) {
                            rc.completeTowerPattern(type, loc);
                            rc.setTimelineMarker("Tower built", 0, 255, 0);
                            System.out.println("Built a tower at " + loc + "!");
                            prevType = -1;
                        }

                        // Move closer
                        Direction dirToRuin = GameUtils.greedyPath(rc, rc.getLocation(), loc);
                        if (rc.canMove(dirToRuin)) {
                            rc.move(dirToRuin);
                        }
                        if (rc.canMove(GameUtils.greedyPath(rc, rc.getLocation(), loc).rotateLeft())) {
                            rc.move(dirToRuin);
                        }
                    } else {
                        // Finished ruin, make improvements while passing by
                        if (rc.canUpgradeTower(loc) && rc.getNumberTowers() >= 5 && rc.getChips() >= 1300) {
                            rc.upgradeTower(loc);
                        }
                    }
                }
            }
        }
        prevRuinLoc = ruinFocus;

        if (rc.isMovementReady()) {
            // Inertia
            moveScore[prevDir] += 5;
            moveScore[(prevDir + 1) % 8] += 1;
            moveScore[(prevDir + 7) % 8] += 1;

            // Move towards the center, ig
            final MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
            moveScore[GameUtils.greedyPath(rc, rc.getLocation(), center).ordinal()] += 2;

            for (MapInfo tile : rc.senseNearbyMapInfos()) {
                final MapLocation loc = tile.getMapLocation();
                final int dir = rc.getLocation().directionTo(loc).ordinal();

                if (tile.getPaint() == PaintType.EMPTY) {
                    moveScore[dir]++;
                }

                if (tile.getPaint().isAlly()) {
                    moveScore[dir] -= 0.5;
                }
            }

            // TODO: Add probabilistic choice to avoid collisions?
            int bestDir = -1;
            for (int i = 8; --i >= 0; ) {
                if (rc.canMove(DIRS[i]) && (bestDir == -1 || moveScore[i] > moveScore[bestDir])) {
                    bestDir = i;
                }
            }
            if (bestDir != -1) {
                rc.move(DIRS[bestDir]);
                prevDir = bestDir;
            }
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();
        makeAction(rc);
        assert rc.getRoundNum() == curRound : "Soldier: Bytecode limit exceeded";
    }
}