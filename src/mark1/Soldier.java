package mark1;

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

    public static MapLocation spawnLoc, closestPT;
    public static MapInfo[][] map = new MapInfo[2 * BOARD_RAD][2 * BOARD_RAD];

    public static Direction curDir;

    public static boolean isPaintable(RobotController rc, MapLocation loc) throws GameActionException {
        if (rc.canSenseLocation(loc)) {
            MapInfo mi = rc.senseMapInfo(loc);
            if (rc.canAttack(loc) && mi.getPaint() == PaintType.EMPTY) {
                if (rc.canSenseRobotAtLocation(loc)) {
                    RobotInfo ri = rc.senseRobotAtLocation(loc);
                    if (ri.team.isPlayer()) {
                        return switch (ri.getType()) {
                            case MOPPER, SOLDIER, SPLASHER -> true;
                            default -> false;
                        };
                    } else {
                        return true;
                    }
                } else {
                    return !mi.hasRuin() && !mi.isWall();
                }
            } else {
                return false;
            }
        }
        return false;
    }

    public static boolean unfinishedRuin(RobotController rc, MapInfo tile) throws GameActionException {
        return tile.hasRuin() && !rc.canSenseRobotAtLocation(tile.getMapLocation());
    }

    public static boolean allyPaintTower(RobotController rc, MapLocation loc) throws GameActionException {
        return rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).getTeam() == rc.getTeam() && rc.senseRobotAtLocation(loc).type == UnitType.LEVEL_ONE_PAINT_TOWER;
    }

    public static void updateNearby(RobotController rc) throws GameActionException {
        if (spawnLoc == null) {
            spawnLoc = rc.getLocation();
        }

        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();

        for (MapInfo tile : nearbyTiles) {
            // Maintain the closest paint tower for refills
            if (allyPaintTower(rc, tile.getMapLocation())) {
                closestPT = tile.getMapLocation();
            }

            // Update internal map with nearby info
            MapLocation loc = tile.getMapLocation();
            MapLocation diff = VMath.subVec(loc, spawnLoc);
            map[diff.x + BOARD_RAD][diff.y + BOARD_RAD] = tile;
        }
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        boolean moved = false, painted = false;

        // When low, try to move towards a paint tower if possible
        if (rc.getPaint() < 100 && closestPT != null) {
            Direction dir = rc.getLocation().directionTo(closestPT);
            if (rc.canMove(dir)) {
                rc.move(dir);
                moved = true;
            }
        }

        // If there's a ruin to work on, prioritize that
        for (MapInfo tile : nearbyTiles) {
            if (unfinishedRuin(rc, tile)) {
                // System.out.println("Working on a ruin");

                // Move close to the ruin while working on it
                MapLocation targetLoc = tile.getMapLocation();
                Direction dir = rc.getLocation().directionTo(targetLoc);
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }

                // Type of the ruin depends on its coords ig
                UnitType type = (targetLoc.x + targetLoc.y) % 4 == 0 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER;

                // Mark the pattern we need to draw to build a tower here if we haven't already.
                MapLocation shouldBeMarked = tile.getMapLocation().subtract(dir);
                if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(type, targetLoc)) {
                    rc.markTowerPattern(type, targetLoc);
                    // System.out.println("Trying to build a tower at " + targetLoc);
                }

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
                    // System.out.println("Built a tower at " + targetLoc + "!");
                }
            }
        }

        // Default randomized move direction
        while (curDir == null || !rc.canMove(curDir)) {
            curDir = DIRS[QRand.randInt(8)];
        }

        if (!moved) {
            rc.move(curDir);
        }

        // Try to paint, prioritizing our current tile first and everything else randomly
        if (isPaintable(rc, rc.getLocation())) {
            rc.attack(rc.getLocation());
            painted = true;
        }
        for (MapInfo tile : nearbyTiles) {
            MapLocation target = tile.getMapLocation();
            if (!painted && isPaintable(rc, target)) {
                rc.attack(target);
                painted = true;
            }
        }

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
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        updateNearby(rc);
        makeAction(rc);
    }
}
