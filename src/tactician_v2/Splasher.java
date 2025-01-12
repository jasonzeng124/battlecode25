package tactician_v2;

import battlecode.common.*;

public class Splasher {
    private static final Direction[] DIRS = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.CENTER};

    enum State {
        WORKING, GOING_HOME, GOING_BACK
    }

    ;

    public static MapInfo[] nearbyTiles;
    public static RobotInfo[] nearbyRobots;
    public static MapLocation origin, myLoc, spawnLoc, workLoc, closestPT;
    private static Direction prvDir = null;
    private static State curState = State.WORKING;

    public static void updateNearby(RobotController rc) throws GameActionException {
        if (origin == null) {
            for (Message msg : rc.readMessages(-1)) {
                final int data = msg.getBytes();
                origin = new MapLocation(data & 63, (data >> 6) & 63);
                break;
            }
        }
        if (spawnLoc == null) {
            spawnLoc = rc.getLocation();
        }
        myLoc = rc.getLocation();
        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo tile : rc.senseNearbyRobots()) {
            // Maintain the closest paint tower for refills
            if (tile.team == rc.getTeam() && tile.type.paintPerTurn > 0) {
                closestPT = tile.getLocation();
            }
        }
    }

    public static boolean allyPaintTower(RobotController rc, MapLocation loc) throws GameActionException {
        if (!rc.canSenseRobotAtLocation(loc) || rc.senseRobotAtLocation(loc).getTeam() != rc.getTeam()) {
            return false;
        }
        final UnitType tp = rc.senseRobotAtLocation(loc).type;
        return (tp == UnitType.LEVEL_ONE_PAINT_TOWER || tp == UnitType.LEVEL_TWO_PAINT_TOWER || tp == UnitType.LEVEL_THREE_PAINT_TOWER);
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();

        // Withdraw paint
        if (rc.isActionReady() && rc.getPaint() < 100) {
            for (RobotInfo robot : nearbyRobots) {
                MapLocation loc = robot.getLocation();
                if (rc.getLocation().isWithinDistanceSquared(loc, 2) && robot.type.paintPerTurn > 0 && robot.getPaintAmount() >= 150) {
                    int delta = -1 * java.lang.Math.min(robot.paintAmount, 300 - rc.getPaint());
                    if (delta < 0) {
                        rc.transferPaint(loc, delta);
                    }
                }
            }
        }

        // Try to splash something
        if (rc.isActionReady()) {
            MapLocation loc = null;
            int scr = 0;
            for (MapInfo tile : nearbyTiles) {
                final MapLocation tgt = tile.getMapLocation();
                if (!rc.canAttack(tgt)) {
                    continue;
                }
                int curScr = 0;
                for (MapInfo curTile : rc.senseNearbyMapInfos(tgt, 4)) {
                    switch (curTile.getPaint()) {
                        case ALLY_PRIMARY, ALLY_SECONDARY -> curScr -= 5;
                        case EMPTY -> curScr += 2;
                        case ENEMY_PRIMARY, ENEMY_SECONDARY -> curScr += 3;
                    }
                }
                if (curScr > scr) {
                    loc = tgt;
                    scr = curScr;
                }
            }
            if (loc != null && scr >= 10) {
                rc.attack(loc);
            }
        }

        // Handle unit states, which determine movement
        switch (curState) {
            case WORKING:
                workLoc = myLoc;
                if (rc.isMovementReady()) {
                    double[] moveScore = new double[9];
                    for (int i = 0; i < 9; i++) {
                        moveScore[i] = 0;
                    }

                    // Move away from the origin
                    if (origin != null) {
                        final MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
                        moveScore[origin.directionTo(center).ordinal()] += 3;
                    }

                    for (MapInfo tile : nearbyTiles) {
                        final MapLocation loc = tile.getMapLocation();
                        final int dir = myLoc.directionTo(loc).ordinal();
                        final double dist = myLoc.distanceSquaredTo(loc);

                        // Get close to enemy paint, but not onto it
                        if (tile.getPaint().isEnemy()) {
                            moveScore[dir] += dist <= 2 ? -20 : +1;
                        }

                        // Get close to empty paint
                        if (tile.getPaint() == PaintType.EMPTY) {
                            moveScore[dir] += dist <= 2 ? -10 : +2;
                        }
                    }

                    // Inertia
                    if (prvDir != null) {
                        moveScore[prvDir.ordinal()] += 5;
                        moveScore[(prvDir.ordinal() + 1) % 8] += 2;
                        moveScore[(prvDir.ordinal() + 7) % 8] += 2;
                    }

                    // TODO: Add probabilistic choice to avoid collisions?
                    int bestDir = -1;
                    for (int i = 9; --i >= 0; ) {
                        if (rc.canMove(DIRS[i]) && (bestDir == -1 || moveScore[i] > moveScore[bestDir])) {
                            bestDir = i;
                        }
                    }
                    if (bestDir != -1) {
                        rc.move(DIRS[bestDir]);
                        prvDir = DIRS[bestDir];
                    }
                }
                if (closestPT != null && rc.getPaint() < 100) {
                    curState = State.GOING_HOME;
                }
                break;
            case GOING_HOME:
                if (rc.isMovementReady()) {
                    rc.move(GameUtils.greedyPath(rc, myLoc, closestPT));
                }
                if (rc.getPaint() >= 200) {
                    curState = State.GOING_BACK;
                }
                break;
            case GOING_BACK:
                if (rc.isMovementReady()) {
                    rc.move(GameUtils.greedyPath(rc, myLoc, workLoc));
                }
                if (myLoc.isWithinDistanceSquared(workLoc, 4)) {
                    curState = State.WORKING;
                }
                break;
        }

        assert rc.getRoundNum() == curRound : "Mopper: Bytecode limit exceeded";

    }

    public static void run(RobotController rc) throws GameActionException {
        updateNearby(rc);
        makeAction(rc);
    }
}
