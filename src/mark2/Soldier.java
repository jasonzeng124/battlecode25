package mark2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

public class Soldier {
    static final int BOARD_RAD = 65;
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

    public static MapLocation myLoc, closestPT;

    public static Direction curDir;

    public static boolean isPaintable(RobotController rc, MapLocation loc) throws GameActionException {
        if (rc.canAttack(loc)) {
            MapInfo mi = rc.senseMapInfo(loc);
            if (mi.getPaint() == PaintType.EMPTY || (mi.getPaint().isAlly() && mi.getMark() != PaintType.EMPTY && mi.getPaint() != mi.getMark())) {
                if (mi.hasRuin()) {
                    return (rc.canSenseRobotAtLocation(loc) && (!rc.senseRobotAtLocation(loc).getTeam().isPlayer()));
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean shouldWorkHere(RobotController rc, MapInfo tile) throws GameActionException {
        return (tile.hasRuin() && !rc.canSenseRobotAtLocation(tile.getMapLocation())) || rc.canUpgradeTower(tile.getMapLocation());
    }

    public static boolean shouldMove(RobotController rc, Direction dir) throws GameActionException {
        return rc.canMove(dir) && (rc.getPaint() >= 100 || !rc.senseMapInfo(myLoc).getPaint().isAlly() || rc.senseMapInfo(myLoc.add(dir)).getPaint().isAlly());
    }

    public static boolean allyPaintTower(RobotController rc, MapLocation loc) throws GameActionException {
        if (!rc.canSenseRobotAtLocation(loc)) {
            return false;
        }
        final RobotInfo r = rc.senseRobotAtLocation(loc);
        return r.getTeam() == rc.getTeam() && (r.getType() == UnitType.LEVEL_ONE_PAINT_TOWER || r.getType() == UnitType.LEVEL_TWO_PAINT_TOWER || r.getType() == UnitType.LEVEL_THREE_PAINT_TOWER);
    }

    public static void updateNearby(RobotController rc) throws GameActionException {
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

    public static void makeAction(RobotController rc) throws GameActionException {
        // If there's a ruin to work on, prioritize that
        for (MapInfo tile : nearbyTiles) {
            if (shouldWorkHere(rc, tile)) {
                final MapLocation targetLoc = tile.getMapLocation();
                final Direction dir = rc.getLocation().directionTo(targetLoc);

                // Move close to the ruin while working on it
                if (rc.isMovementReady() && rc.canMove(dir)) {
                    rc.move(dir);
                }

                // Chance to build a money tower
                UnitType type = (targetLoc.x + targetLoc.y) % 2 == 0 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER;

                // Mark the pattern we need to draw to build a tower here if we haven't already.
                MapLocation shouldBeMarked = tile.getMapLocation().subtract(dir);
                if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(type, targetLoc)) {
                    rc.markTowerPattern(type, targetLoc);
                    System.out.println("Trying to build a tower at " + targetLoc);
                }

                if (rc.isActionReady()) {
                    // Fill in any spots in the pattern with the appropriate paint.
                    for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)) {
                        if (patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY) {
                            boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                            if (rc.canAttack(patternTile.getMapLocation())) {
                                rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                            }
                        }
                    }

                    // Complete the ruin if we can.
                    if (rc.canCompleteTowerPattern(type, targetLoc)) {
                        rc.completeTowerPattern(type, targetLoc);
                        rc.setTimelineMarker("Tower built", 0, 255, 0);
                        System.out.println("Built a tower at " + targetLoc + "!");
                    }

                    // Upgrade the tower if we can.
                    if (rc.canUpgradeTower(targetLoc)) {
                        rc.upgradeTower(targetLoc);
                    }
                }
            }
        }

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

            // Try to paint, prioritizing our current tile first and everything else randomly
            if (isPaintable(rc, myLoc)) {
                rc.attack(myLoc);
            }
            for (MapInfo tile : nearbyTiles) {
                MapLocation target = tile.getMapLocation();
                if (isPaintable(rc, target)) {
                    rc.attack(target);
                }
            }
        }

        if (rc.isMovementReady()) {
            // When low, try to move towards a paint tower if possible
            if (rc.getPaint() < 100 && closestPT != null) {
                Direction dir = rc.getLocation().directionTo(closestPT);
                if (shouldMove(rc, dir)) {
                    rc.move(dir);
                }
            }

            // Default randomized move direction
            while (curDir == null || !shouldMove(rc, curDir)) {
                curDir = DIRS[QRand.randInt(8)];
            }
            rc.move(curDir);
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        updateNearby(rc);
        makeAction(rc);
    }
}
