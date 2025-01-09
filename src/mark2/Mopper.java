package mark2;

import battlecode.common.*;
import battlecode.schema.RobotType;

import java.util.ArrayList;
import java.util.Random;

public class Mopper {
    static final double INF = 100000.0;

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

    public static boolean initialized = false;

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
        if (rc.getLocation().isWithinDistanceSquared(loc, 2) && rc.canAttack(loc)) {
            MapInfo mi = rc.senseMapInfo(loc);
            return !mi.getPaint().isAlly();
        }
        return false;
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        if (rc.isActionReady()) {
            // Withdraw paint
            for (RobotInfo robot : nearbyRobots) {
                MapLocation loc = robot.getLocation();
                if (rc.getLocation().isWithinDistanceSquared(loc, 2) && allyPaintTower(rc, loc)) {
                    int delta = -1 * java.lang.Math.min(robot.paintAmount, 200 - rc.getPaint());
                    if (delta < 0) {
                        rc.transferPaint(loc, delta);
                    }
                }
            }

            // Mop a nearby tile
            for (MapInfo tile : nearbyTiles) {
                MapLocation target = tile.getMapLocation();
                if (isMoppable(rc, target)) {
                    rc.attack(target);
                    break;
                }
            }
        }

        if (rc.isMovementReady()) {
            double[] moveScore = new double[9];
            for (int i = 0; i < 9; i++) {
                moveScore[i] = 0;
            }

            // Try not to stand still, especially if we're on enemy paint
            moveScore[8] = -5;
            if (!rc.senseMapInfo(myLoc).getPaint().isAlly()) {
                moveScore[8] -= 10;
            }

            // Try to move towards a paint tower if we're low
            if (closestPT != null && rc.getPaint() < 50) {
                moveScore[myLoc.directionTo(closestPT).ordinal()] += 5;
            }

            for (MapInfo tile : nearbyTiles) {
                final MapLocation loc = tile.getMapLocation();
                final int dir = myLoc.directionTo(loc).ordinal();
                final double dist = myLoc.distanceSquaredTo(loc);

                // Get close to enemy paint, but not onto it
                if (!tile.getPaint().isAlly()) {
                    moveScore[dir] += dist <= 2 ? -5 : +1;
                }

                if (rc.canSenseRobotAtLocation(loc)) {
                    final RobotInfo r = rc.senseRobotAtLocation(loc);
                    if (r.getTeam() != rc.getTeam()) {
                        switch (r.getType()) {
                            // Moppers don't want to be in direct danger
                            case UnitType.LEVEL_ONE_MONEY_TOWER, UnitType.LEVEL_ONE_PAINT_TOWER -> moveScore[dir] -= 5;
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
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        updateNearby(rc);
        makeAction(rc);
    }
}
