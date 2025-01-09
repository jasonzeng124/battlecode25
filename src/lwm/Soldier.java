package lwm;

import battlecode.common.*;

public class Soldier {

    public static void run(RobotController rc) throws GameActionException {

        boolean move = true;
        MapLocation ruinLoc = null;

        for (MapInfo x : rc.senseNearbyMapInfos()) {
            if (x.hasRuin()) {
                ruinLoc = x.getMapLocation();
            }
        }

        if (ruinLoc != null && rc.senseRobotAtLocation(ruinLoc) == null) {
            final Direction dir = rc.getLocation().directionTo(ruinLoc);
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            if (rc.senseMapInfo(ruinLoc.subtract(dir)).getMark() == PaintType.EMPTY) {
                final UnitType tp = (
                    !Util.rand.nextBoolean() ?
                    UnitType.LEVEL_ONE_PAINT_TOWER : UnitType.LEVEL_ONE_MONEY_TOWER
                );
                if (rc.canMarkTowerPattern(tp, ruinLoc)) {
                    rc.markTowerPattern(tp, ruinLoc);
                }
            }
            for (MapInfo tile : rc.senseNearbyMapInfos(ruinLoc, 8)) {
                final MapLocation loc = tile.getMapLocation();
                final PaintType mark = tile.getMark();
                if (mark != tile.getPaint() && mark != PaintType.EMPTY && rc.canAttack(loc)) {
                    rc.attack(loc, mark == PaintType.ALLY_SECONDARY);
                    move = false;
                }
            }
            if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruinLoc)) {
                rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruinLoc);
            } else if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, ruinLoc)) {
                rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, ruinLoc);
            }
        }

        if (rc.isMovementReady() && move) {
            final Direction dir = Util.dirs[Util.rand.nextInt(8)];
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }

        if (rc.getRoundNum() >= 100) {
            for (MapInfo tile : rc.senseNearbyMapInfos(2)) {
                final boolean tp = Util.getBoost(tile.getMapLocation());
                if (
                    tile.getMark() == PaintType.EMPTY &&
                    tile.getPaint() != (!tp ? PaintType.ALLY_PRIMARY : PaintType.ALLY_SECONDARY)
                ) {
                    final MapLocation loc = tile.getMapLocation();
                    if (rc.canAttack(loc)) {
                        rc.attack(loc, Util.getBoost(tile.getMapLocation()));
                    }
                }
            }
            for (MapInfo tile : rc.senseNearbyMapInfos(2)) {
                final MapLocation loc = tile.getMapLocation();
                if (loc.x % 5 == 2 && loc.y % 5 == 2 && rc.canCompleteResourcePattern(loc)) {
                    rc.completeResourcePattern(loc);
                }
            }
        }

    }

}
