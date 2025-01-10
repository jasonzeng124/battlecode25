package mark2;

import battlecode.common.*;

public class Mopper {
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

    static final int HOME_THRES = 20;

    public static MapInfo[] nearbyTiles;
    public static RobotInfo[] nearbyRobots;
    public static MapLocation myLoc, spawnLoc, closestPT;

    public static boolean allyPaintTower(RobotController rc, MapLocation loc) throws GameActionException {
        return rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).getTeam() == rc.getTeam() && rc.senseRobotAtLocation(loc).type == UnitType.LEVEL_ONE_PAINT_TOWER;
    }

    public static void updateNearby(RobotController rc) throws GameActionException {
        if (spawnLoc == null) {
            spawnLoc = rc.getLocation();
        }

        myLoc = rc.getLocation();
        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();

        for (MapInfo tile : nearbyTiles) {
            // Maintain the closest paint tower for refills
            if (allyPaintTower(rc, tile.getMapLocation())) {
                closestPT = tile.getMapLocation();
            }
        }
    }

    public static boolean isMoppable(RobotController rc, MapLocation loc) throws GameActionException {
        return rc.getLocation().isWithinDistanceSquared(loc, 2) && rc.canAttack(loc) && GameUtils.hasEnemyTile(rc, loc);
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();

        // Withdraw paint
        if (rc.isActionReady() && rc.getPaint() < 50) {
            for (RobotInfo robot : nearbyRobots) {
                MapLocation loc = robot.getLocation();
                if (rc.getLocation().isWithinDistanceSquared(loc, 2) && GameUtils.hasAllyPaintTower(rc, loc) && robot.getPaintAmount() >= 50) {
                    int delta = -1 * java.lang.Math.min(robot.paintAmount, 200 - rc.getPaint());
                    if (delta < 0) {
                        rc.transferPaint(loc, delta);
                    }
                }
            }
        }

        // Try to mop something
        if (rc.isActionReady() && rc.getPaint() >= 75) {
            for (MapInfo tile : nearbyTiles) {
                MapLocation target = tile.getMapLocation();
                if (isMoppable(rc, target)) {
                    rc.attack(target);
                }
            }
        }

        if (rc.isMovementReady()) {
            double[] moveScore = new double[9];
            for (int i = 0; i < 9; i++) {
                moveScore[i] = 0;
            }

            // Try to move towards a paint tower if we're low
            if (closestPT != null && rc.getPaint() < HOME_THRES) {
                moveScore[myLoc.directionTo(closestPT).ordinal()] += 10;
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
                        }
                    }
                }
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
            }
        }

        assert rc.getRoundNum() == curRound : "Mopper: Bytecode limit exceeded";
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        updateNearby(rc);
        makeAction(rc);
    }
}
