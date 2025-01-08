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

    public static Direction curDir;

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
        boolean moved = false;

        // Mop a nearby tile
        for (MapInfo tile : nearbyTiles) {
            MapLocation target = tile.getMapLocation();
            if (isMoppable(rc, target)) {
                rc.attack(target);
                break;
            }
        }

        // Smart move direction
        double moveScore[] = new double[9];
        for (int i = 0; i < 9; i++) {
            moveScore[i] = 0;
        }
        moveScore[8] = -5;
        if (rc.getPaint() < 50) {
            moveScore[myLoc.directionTo(closestPT).ordinal()] += 5;
        }
        for (MapInfo tile : nearbyTiles) {
            final MapLocation loc = tile.getMapLocation();
            final int dir = myLoc.directionTo(loc).ordinal();
            final double dist = myLoc.distanceSquaredTo(loc);

            if (!tile.getPaint().isAlly()) {
                moveScore[dir] += dist <= 2 ? -10 : +1;
            }

            if (rc.canSenseRobotAtLocation(loc)) {
                final RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r.getTeam() != rc.getTeam()) {
                    switch (r.getType()) {
                        case UnitType.SOLDIER -> moveScore[dir] -= 1;
                        case UnitType.LEVEL_ONE_MONEY_TOWER, UnitType.LEVEL_ONE_PAINT_TOWER -> moveScore[dir] -= 5;
                    }
                }
            }
        }
        int bestDir = -1;
        for (int i = 0; i < 9; i++) {
            if (rc.canMove(DIRS[i]) && (bestDir == -1 || moveScore[i] > moveScore[bestDir])) {
                bestDir = i;
            }
        }
        if (!moved) {
            rc.move(DIRS[bestDir]);
            moved = true;
        }

        // Withdraw paint
        for (RobotInfo robot : nearbyRobots) {
            MapLocation loc = robot.getLocation();
            if (myLoc.isWithinDistanceSquared(loc, 2) && allyPaintTower(rc, loc)) {
                int delta = -1 * java.lang.Math.min(robot.paintAmount, 100 - rc.getPaint());
                if (delta < 0) {
                    rc.transferPaint(loc, delta);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        updateNearby(rc);
        makeAction(rc);
    }
}
