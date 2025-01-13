package tactician_v3;

import battlecode.common.*;

import java.util.Arrays;

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

    enum State {
            WANDERING, BUILDING, REFILLING, RETURNING_TO_WORK
    };

    public static MapLocation origin, workLoc, refillLoc, ruinLoc;
    public static int ruinTypeID = -1;

    public static int prevDir;

    public static int[][] qClearArr = new int[60][60];
    static int qClearTime = 0;

    static State curState = State.WANDERING;

    public static void updateNearby(RobotController rc) throws GameActionException {
        // Receive the origin
        if (origin == null) {
            for (Message msg : rc.readMessages(-1)) {
                final int data = msg.getBytes();
                origin = new MapLocation(data & 63, (data >> 6) & 63);
                break;
            }
        }

        // Broadcast the origin
        if (origin != null) {
            rc.setIndicatorDot(origin, 255, 162, 0);
            for (RobotInfo r : rc.senseNearbyRobots(rc.getLocation(), 9, rc.getTeam())) {
                if (r.type.isTowerType() && rc.canSendMessage(r.getLocation())) {
                    rc.sendMessage(r.getLocation(), origin.x | (origin.y << 6));
                }
            }
        }

        // Maintain the closest paint tower for refills
        for (RobotInfo tile : rc.senseNearbyRobots()) {
            if (tile.team == rc.getTeam() && tile.type.paintPerTurn > 0) {
                refillLoc = tile.getLocation();
                break;
            }
        }
        if (refillLoc != null) {
            rc.setIndicatorDot(refillLoc, 54, 230, 23);
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

    static void tryPaintingDefaults(RobotController rc) throws GameActionException {
        // Use a quick-clearing array to set banned tiles, from unfinished ruins
        qClearTime++;
        for (MapLocation loc : rc.senseNearbyRuins(-1)) {
            if (!rc.canSenseRobotAtLocation(loc)) {
                for (int i = 5; --i >= 0; ) {
                    for (int j = 5; --j >= 0; ) {
                        int x = loc.x + i - 2, y = loc.y + j - 2;
                        if (x >= 0 && x < 60 && y >= 0 && y < 60) {
                            qClearArr[x][y] = qClearTime;
                        }
                    }
                }
            }
        }

        MapLocation placed = null;
        for (MapInfo tile : rc.senseNearbyMapInfos(rc.getLocation(), 4)) {
            final MapLocation loc = tile.getMapLocation();

            if (qClearArr[loc.x][loc.y] < qClearTime && isPaintable(rc, loc) && tile.getPaint() != defaultPattern(loc)) {
                rc.attack(loc, defaultPattern(loc) == PaintType.ALLY_SECONDARY);
                placed = loc;
                break;
            }

            if (placed != null) {
                break;
            }
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

    static void tryAttackingTowers(RobotController rc) throws GameActionException {
        for (RobotInfo r : rc.senseNearbyRobots(rc.getLocation(), 4, rc.getTeam().opponent())) {
            if (r.type.isTowerType()) {
                rc.attack(r.getLocation());
                break;
            }
        }
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        switch (curState) {
            case WANDERING:
                rc.setIndicatorString("Pawn: wandering");

                workLoc = rc.getLocation();

                if (rc.isMovementReady()) {
                    double[] moveScore = new double[9];
                    Arrays.fill(moveScore, 0);

                    // Inertia
                    moveScore[prevDir] += 5;

                    for (MapInfo tile : rc.senseNearbyMapInfos()) {
                        final MapLocation loc = tile.getMapLocation();
                        final int dir = rc.getLocation().directionTo(loc).ordinal();
                        final double dist = rc.getLocation().distanceSquaredTo(loc);

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
                                    case UnitType.SOLDIER -> moveScore[dir] += rc.getRoundNum() < 400 ? -10 : -0.5;
                                    case UnitType.MOPPER -> moveScore[dir] += 2.0;
                                }
                            }
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

                // Attack nearby enemy towers
                if (rc.isActionReady() && rc.getPaint() >= 75) {
                    tryAttackingTowers(rc);
                }

                // Work on resource patterns
                if (rc.isActionReady() && rc.getPaint() >= 75) {
                    tryPaintingDefaults(rc);
                }

                // Look for ruins to work on
                if (rc.getNumberTowers() < 25) {
                    for (MapLocation loc : rc.senseNearbyRuins(-1)) {
                        if (!rc.canSenseRobotAtLocation(loc)) {
                            // We found a ruin <tile> at <loc>, do stuff with it

                            // TODO: guide this using origin loc, not round number
                            final double coinProb = Math.pow(0.998, rc.getRoundNum());
                            ruinLoc = loc;
                            ruinTypeID = FastMath.fakefloat() < coinProb ? 1 : 0;
                            break;
                        }
                    }
                }

                // TRANSITIONS
                if (rc.getPaint() < 100 && refillLoc != null) {
                    curState = State.REFILLING;
                }
                if (ruinLoc != null && ruinTypeID != -1) {
                    curState = State.BUILDING;
                }
                break;
            case BUILDING:
                rc.setIndicatorString("Pawn: building a tower");

                assert ruinLoc != null && ruinTypeID != -1;
                rc.setIndicatorDot(ruinLoc, 255, 0, 0);

                // Move closer, circle around
                if (rc.isMovementReady()) {
                    rc.move(GameUtils.greedyPath(rc, rc.getLocation(), ruinLoc));
                }

                // Fill in any spots in the pattern with the appropriate paint.
                boolean filled = false;
                for (int i = 5; --i >= 0; ) {
                    for (int j = 5; --j >= 0; ) {
                        final MapLocation loc = new MapLocation(ruinLoc.x + i - 2, ruinLoc.y + j - 2);
                        if (isPaintable(rc, loc) && rc.senseMapInfo(loc).getPaint() != (PTRNS[ruinTypeID][i][j] == 1 ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY)) {
                            rc.attack(loc, PTRNS[ruinTypeID][i][j] == 1);
                            filled = true;
                            break;
                        }
                    }
                    if (filled) {
                        break;
                    }
                }

                // Complete the ruin if we can.
                final UnitType type = ruinTypeID == 1 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER;
                if (rc.canCompleteTowerPattern(type, ruinLoc)) {
                    rc.completeTowerPattern(type, ruinLoc);
                }

                // TRANSITIONS
                if (rc.getPaint() < 50 && refillLoc != null) {
                    curState = State.REFILLING;
                }
                if (rc.getNumberTowers() == 25 || rc.canSenseRobotAtLocation(ruinLoc)) {
                    ruinLoc = null;
                    ruinTypeID = -1;
                    curState = State.WANDERING;
                }
                break;
            case REFILLING:
                rc.setIndicatorString("Pawn: refilling");
                // TODO: be even more incentivized to save paint (stay on our own color, etc) while refilling

                // Move closer
                if (rc.isMovementReady()) {
                    rc.move(GameUtils.greedyPath(rc, rc.getLocation(), refillLoc));
                }

                // Withdraw paint
                for (RobotInfo r : rc.senseNearbyRobots(rc.getLocation(), 2, rc.getTeam())) {
                    if (r.type.paintPerTurn > 0 && rc.getPaint() + r.getPaintAmount() >= 175) {
                        final int amount = -Math.min(r.getPaintAmount(), 200 - rc.getPaint());
                        if (rc.canTransferPaint(r.getLocation(), amount)) {
                            rc.transferPaint(r.getLocation(), amount);
                        }
                    }
                }

                // TRANSITIONS
                if (rc.getPaint() >= 175) {
                    curState = State.RETURNING_TO_WORK;
                }
                break;
            case RETURNING_TO_WORK:
                rc.setIndicatorString("Pawn: returning to last work location");
                // TODO: guess where the frontier has moved to while we were away

                // Move closer
                if (rc.isMovementReady()) {
                    rc.move(GameUtils.greedyPath(rc, rc.getLocation(), workLoc));
                }

                // Work on resource patterns
                if (rc.isActionReady() && rc.getPaint() >= 75) {
                    tryPaintingDefaults(rc);
                }

                // TRANSITIONS
                if (rc.getLocation().isWithinDistanceSquared(workLoc, 4)) {
                    curState = State.WANDERING;
                }
                break;
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();
        updateNearby(rc);
        makeAction(rc);
        assert rc.getRoundNum() == curRound : "Soldier: Bytecode limit exceeded";
    }
}
