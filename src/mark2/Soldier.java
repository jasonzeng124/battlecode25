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

    public static MapInfo[] nearbyTiles;

    public static MapLocation myLoc, closestPT;

    public static int prevDir;

    public static void updateNearby(RobotController rc) throws GameActionException {
        myLoc = rc.getLocation();
        nearbyTiles = rc.senseNearbyMapInfos();

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

        double[] moveScore = new double[9];
        for (int i = 0; i < 9; i++) {
            moveScore[i] = 0;
        }

        for (MapInfo tile : nearbyTiles) {
            if (tile.hasRuin()) {
                final MapLocation targetLoc = tile.getMapLocation();
                final Direction dir = rc.getLocation().directionTo(targetLoc);

                // Unfinished ruin
                if (!rc.canSenseRobotAtLocation(tile.getMapLocation())) {
                    // Decide which type of tower to build
                    UnitType type = (targetLoc.x + targetLoc.y) % 2 == 0 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER;

                    // Mark the pattern we need to draw to build a tower here if we haven't already.
                    MapLocation markLoc = tile.getMapLocation().subtract(dir);
                    if (rc.senseMapInfo(markLoc).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(type, targetLoc)) {
                        rc.markTowerPattern(type, targetLoc);
                        System.out.println("Trying to build a tower at " + targetLoc);
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
                        System.out.println("Built a tower at " + targetLoc + "!");
                    }

                    // Move closer. Turn clockwise around the ruin to view the whole area
                    int idx = dir.ordinal();
                    while (idx < 16 && !rc.canMove(DIRS[idx % 8])) {
                        idx++;
                    }
                    idx %= 8;
                    moveScore[idx] += 15;
                } else {
                    // Finished ruin, make improvements while passing by
                    if (rc.canUpgradeTower(targetLoc)) {
                        rc.upgradeTower(targetLoc);
                    }
                }
            }
        }

        // Mark resource patterns on clear areas
        boolean areaClear = true;
        for (int dx = -2; dx <= +2; dx++) {
            for (int dy = -2; dy <= +2; dy++) {
                final MapLocation loc = VMath.addVec(myLoc, new MapLocation(dx, dy));
                if (!(rc.canSenseLocation(loc) && rc.sensePassability(loc) && rc.senseMapInfo(loc).getMark() == PaintType.EMPTY)) {
                    areaClear = false;
                    break;
                }
            }
        }
        if (areaClear && rc.canMarkResourcePattern(myLoc)) {
            rc.markResourcePattern(myLoc);
        }
        if (rc.canCompleteResourcePattern(myLoc)) {
            rc.completeResourcePattern(myLoc);
        }

        // Withdraw paint
        if (rc.isActionReady() && rc.getPaint() < 150) {
            for (RobotInfo robot : rc.senseNearbyRobots()) {
                MapLocation loc = robot.getLocation();
                if (rc.getLocation().isWithinDistanceSquared(loc, 2) && GameUtils.hasAllyPaintTower(rc, loc) && robot.getPaintAmount() >= 50) {
                    int delta = -1 * java.lang.Math.min(robot.paintAmount, 200 - rc.getPaint());
                    if (delta < 0) {
                        rc.transferPaint(loc, delta);
                    }
                }
            }
        }

        // Try to paint, prioritizing our current tile first and everything else randomly
        if (rc.isActionReady() && rc.getPaint() >= 75) {
            if (isPaintable(rc, myLoc)) {
                rc.attack(myLoc);
            }
            for (MapInfo tile : nearbyTiles) {
                MapLocation target = tile.getMapLocation();
                if (isPaintable(rc, target)) {
                    rc.attack(target, tile.getMark() == PaintType.ALLY_SECONDARY);
                }
            }
        }

        if (rc.isMovementReady()) {
            // Inertia
            moveScore[prevDir] += 5;

            // Try to move towards a paint tower if we're low
            if (closestPT != null && rc.getPaint() < 50) {
                moveScore[myLoc.directionTo(closestPT).ordinal()] += 50;
            }

            for (MapInfo tile : nearbyTiles) {
                final MapLocation loc = tile.getMapLocation();
                final int dir = myLoc.directionTo(loc).ordinal();
                final double dist = myLoc.distanceSquaredTo(loc);

                // Get close to enemy paint, but not onto it
                if (GameUtils.isEnemyTile(tile)) {
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
                            case UnitType.SOLDIER -> moveScore[dir] += curRound < 400 ? -10 : -0.5;
                            case UnitType.MOPPER -> moveScore[dir] += 2.0;
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
