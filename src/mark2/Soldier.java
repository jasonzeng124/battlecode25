package mark2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

public class Soldier {
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

    public static int HOME_THRES = 50;
    public static int BUILD_THRES = 75;

    public static MapInfo[] nearbyTiles;
    public static RobotInfo[] nearbyRobots;

    public static MapLocation myLoc, closestPT;

    public static int prevDir;

    public static boolean shouldWorkHere(RobotController rc, MapInfo tile) throws GameActionException {
        return (tile.hasRuin() && !rc.canSenseRobotAtLocation(tile.getMapLocation())) || rc.canUpgradeTower(tile.getMapLocation());
    }

    public static void updateNearby(RobotController rc) throws GameActionException {
        myLoc = rc.getLocation();
        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();

        for (MapInfo tile : nearbyTiles) {
            // Maintain the closest paint tower for refills
            if (GameUtils.hasAllyPaintTower(rc, tile.getMapLocation())) {
                closestPT = tile.getMapLocation();
            }
        }
    }

    static boolean isPaintable(RobotController rc, MapLocation loc) throws GameActionException {
        if (!rc.canAttack(loc)) {
            return false;
        }

        final MapInfo mi = rc.senseMapInfo(loc);
        return mi.getPaint() == PaintType.EMPTY || (mi.getPaint().isAlly() && mi.getMark() != PaintType.EMPTY && mi.getPaint() != mi.getMark());
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();

        // If there's a ruin to work on, and we have resources, prioritize that
        if (rc.getPaint() >= BUILD_THRES) {
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
                    MapLocation markLoc = tile.getMapLocation().subtract(dir);
                    if (rc.senseMapInfo(markLoc).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(type, targetLoc)) {
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
        }

        if (rc.isActionReady()) {
            // Withdraw paint
            for (RobotInfo robot : nearbyRobots) {
                MapLocation loc = robot.getLocation();
                if (rc.getLocation().isWithinDistanceSquared(loc, 2) && GameUtils.hasAllyPaintTower(rc, loc) && robot.getPaintAmount() >= 50) {
                    int delta = -1 * java.lang.Math.min(robot.paintAmount, 200 - rc.getPaint());
                    if (delta < 0) {
                        rc.transferPaint(loc, delta);
                    }
                }
            }

            // Try to paint, prioritizing our current tile first and everything else randomly
            if (rc.getPaint() >= BUILD_THRES) {
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
        }

        if (rc.isMovementReady()) {
            double[] moveScore = new double[9];
            for (int i = 0; i < 9; i++) {
                moveScore[i] = 0;
            }

            // Inertia
            moveScore[prevDir] += 5;

            // Try to move towards a paint tower if we're low
            if (closestPT != null && rc.getPaint() < HOME_THRES) {
                moveScore[myLoc.directionTo(closestPT).ordinal()] += 50;
            }

            for (MapInfo tile : nearbyTiles) {
                final MapLocation loc = tile.getMapLocation();
                final int dir = myLoc.directionTo(loc).ordinal();
                final double dist = myLoc.distanceSquaredTo(loc);

                // Try not to walk on bare ground, not a hard rule
                moveScore[dir] += GameUtils.hasEnemyTile(rc, loc) ? -3 : 0;

                // Get close to enemy paint, but not onto it
                if (GameUtils.isEnemyTile(tile)) {
                    moveScore[dir] += dist <= 2 ? -20 : +1;
                }

                // Prioritize empty tiles!
                if (tile.getPaint() == PaintType.EMPTY) {
                    moveScore[dir] += dist <= 2 ? -10 : +2;
                }

                if (rc.canSenseRobotAtLocation(loc)) {
                    final RobotInfo r = rc.senseRobotAtLocation(loc);
                    if (r.getTeam() == rc.getTeam()) {
                        switch (r.getType()) {
                            case UnitType.SOLDIER -> moveScore[dir] -= 0.5;
                            case UnitType.MOPPER -> moveScore[dir] += 1.5;
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
