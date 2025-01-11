package tactician_v1;

import battlecode.common.*;

public class Splasher {

    private static final Direction[] DIRS = {
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

    static final int HOME_THRES = 20;

    public static MapInfo[] nearbyTiles;
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLoc, spawnLoc, closestPT;
    private static Direction prvDir = null;

    public static void updateNearby(RobotController rc) throws GameActionException {
        if (spawnLoc == null) {
            spawnLoc = rc.getLocation();
        }
        myLoc = rc.getLocation();
        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();
        for (MapInfo tile : nearbyTiles) {
            if (allyPaintTower(rc, tile.getMapLocation())) {
                closestPT = tile.getMapLocation();
            }
        }
    }

    public static boolean allyPaintTower(
        RobotController rc, MapLocation loc
    ) throws GameActionException {
        if (
            !rc.canSenseRobotAtLocation(loc) ||
            rc.senseRobotAtLocation(loc).getTeam() != rc.getTeam()
        ) {
            return false;
        }
        final UnitType tp = rc.senseRobotAtLocation(loc).type;
        return (
            tp == UnitType.LEVEL_ONE_PAINT_TOWER || tp == UnitType.LEVEL_TWO_PAINT_TOWER ||
            tp == UnitType.LEVEL_THREE_PAINT_TOWER
        );
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();

        // Withdraw paint
        if (rc.isActionReady() && rc.getPaint() < 50) {
            for (RobotInfo robot : nearbyRobots) {
                MapLocation loc = robot.getLocation();
                if (rc.getLocation().isWithinDistanceSquared(loc, 2) && GameUtils.hasAllyPaintTower(rc, loc) && robot.getPaintAmount() >= 50) {
                    int delta = -1 * java.lang.Math.min(robot.paintAmount, 100 - rc.getPaint());
                    if (delta < 0) {
                        rc.transferPaint(loc, delta);
                    }
                }
            }
        }

        // Try to mop something
        if (rc.isActionReady()) {
            MapLocation loc = null;
            int scr = 0;
            for (MapInfo tile : nearbyTiles) {
                final MapLocation tgt = tile.getMapLocation();
                if (!rc.canAttack(tgt)) {
                    continue;
                }
                int cnt = 0;
                int curScr = 0;
                for (MapInfo curTile : rc.senseNearbyMapInfos(tgt, 4)) {
                    final PaintType tp = curTile.getPaint();
                    if (tp == PaintType.ALLY_PRIMARY || tp == PaintType.ALLY_SECONDARY) {
                        curScr -= 15;
                        ++cnt;
                    } else if (tp == PaintType.EMPTY) {
                        curScr += 5;
                    } else {
                        curScr += 10;
                    }
                }
                if (cnt < 4) {
                    if (curScr > scr) {
                        loc = tgt;
                        scr = curScr;
                    }
                }
            }
            if (loc != null) {
                rc.attack(loc);
            }
        }

        if (rc.isMovementReady()) {
            double[] moveScore = new double[9];
            for (int i = 0; i < 9; i++) {
                moveScore[i] = 0;
            }

            // Try to move towards a paint tower if we're low
            if (closestPT != null && rc.getPaint() < HOME_THRES) {
                moveScore[GameUtils.greedyPath(rc, myLoc, closestPT).ordinal()] += 10;
            }

            // Try not to stand still if we're on enemy paint
            moveScore[8] = -1;
            if (GameUtils.hasEnemyTile(rc, myLoc)) {
                moveScore[8] -= 15;
            }

            for (MapInfo tile : nearbyTiles) {
                final MapLocation loc = tile.getMapLocation();
                final int dir = myLoc.directionTo(loc).ordinal();
                final double dist = myLoc.distanceSquaredTo(loc);

                // Get close to enemy paint, but not onto it
                if (GameUtils.isEnemyTile(tile)) {
                    moveScore[dir] += dist <= 2 ? -25 : +1.5;
                }

                if (rc.canSenseRobotAtLocation(loc)) {
                    final RobotInfo r = rc.senseRobotAtLocation(loc);
                    if (r.getTeam() == rc.getTeam()) {
                        switch (r.getType()) {
                            case UnitType.SOLDIER -> moveScore[dir] += 0.5;
                            case UnitType.MOPPER -> moveScore[dir] -= 1;
                            default -> moveScore[dir] += 0;
                        }
                    }
                }
            }

            moveScore[FastMath.rand256() % 9] += 12;

            if (prvDir != null) {
                moveScore[prvDir.ordinal()] += 10;
                moveScore[(prvDir.ordinal() + 1) % 8] += 8;
                moveScore[(prvDir.ordinal() + 7) % 8] += 8;
            }

            // TODO: Add probabilistic choice to avoid collisions?
            int bestDir = -1;
            for (int i = 0; i < 9; i++) {
                if (rc.canMove(DIRS[i]) && (bestDir == -1 || moveScore[i] > moveScore[bestDir])) {
                    bestDir = i;
                }
            }
            if (bestDir != -1) {
                rc.move(DIRS[bestDir]);
                prvDir = DIRS[bestDir];
            }
        }

        assert rc.getRoundNum() == curRound : "Mopper: Bytecode limit exceeded";

    }

    public static void run(RobotController rc) throws GameActionException {

        updateNearby(rc);

        makeAction(rc);

    }

}
