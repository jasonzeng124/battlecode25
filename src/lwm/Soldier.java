package lwm;

import battlecode.common.*;

public class Soldier {

    private static int rnd;
    private static Mode mode = null;
    private static MapLocation botLoc;
    private static MapLocation tgtLoc = null;
    private static Direction prvDir = null;
    private static MapInfo[] tiles;

    private static void build(RobotController rc) throws GameActionException {

        if (rc.senseRobotAtLocation(tgtLoc) != null) {
            mode = Mode.WALK;
            return;
        }

        final Direction dir = botLoc.directionTo(tgtLoc);

        if (rc.canMove(dir)) {
            rc.move(dir);
        } else if (rc.canMove(dir.rotateLeft())) {
            rc.move(dir.rotateLeft());
        }

        if (rc.senseMapInfo(tgtLoc.subtract(dir)).getMark() == PaintType.EMPTY) {
            final int tp = Util.getChoice(Constants.towerProbs[rnd / 100]);
            UnitType twr;
            if (tp == 0) {
                twr = UnitType.LEVEL_ONE_PAINT_TOWER;
            } else if (tp == 1) {
                twr = UnitType.LEVEL_ONE_MONEY_TOWER;
            } else {
                twr = UnitType.LEVEL_ONE_DEFENSE_TOWER;
            }
            if (rc.canMarkTowerPattern(twr, tgtLoc)) {
                rc.markTowerPattern(twr, tgtLoc);
            }
        }

        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, tgtLoc)) {
            rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, tgtLoc);
        }

        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, tgtLoc)) {
            rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, tgtLoc);
        }

        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, tgtLoc)) {
            rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, tgtLoc);
        }

    }

    private static void scan(RobotController rc) throws GameActionException {

        MapLocation ruinLoc = null;

        for (MapInfo tile : tiles) {
            final MapLocation loc = tile.getMapLocation();
            if (
                tile.hasRuin() && rc.senseRobotAtLocation(loc) == null && rc.senseMapInfo(
                    loc.subtract(botLoc.directionTo(loc))
                ).getMark() == PaintType.EMPTY && (
                    ruinLoc == null ||
                    botLoc.distanceSquaredTo(loc) < botLoc.distanceSquaredTo(ruinLoc)
                )
            ) {
                ruinLoc = loc;
            }
        }

        if (ruinLoc == null) {
            mode = Mode.WALK;
            return;
        }

        tgtLoc = ruinLoc;

        mode = Mode.BUILD;

    }

    private static void update(RobotController rc) {

        rnd = rc.getRoundNum() - 1;

        botLoc = rc.getLocation();

        tiles = rc.senseNearbyMapInfos();

    }

    private static void walk(RobotController rc) throws GameActionException {

        int[] probs = {20, 20, 20, 20, 20, 20, 20, 20};

        if (prvDir != null && prvDir != Direction.CENTER) {
            final int idx = Util.getIdx(prvDir);
            probs[idx] += 8;
            probs[(idx + 7) % 8] += 4;
            probs[(idx + 1) % 8] += 4;
            probs[(idx + 4) % 8] -= 8;
            probs[(idx + 3) % 8] -= 4;
            probs[(idx + 5) % 8] -= 4;
        }

        for (int i = 0; i < 8; ++i) {
            final MapLocation loc = botLoc.add(Util.dirs[i]);
            if (rc.onTheMap(loc)) {
                final PaintType paint = rc.senseMapInfo(botLoc.add(Util.dirs[i])).getPaint();
                if (paint == PaintType.EMPTY) {
                    probs[i] -= 4;
                } else if (paint.isEnemy()) {
                    probs[i] -= 8;
                }
            } else {
                probs[i] = 0;
            }
        }

        final Direction dir = Util.dirs[Util.getChoice(probs)];

        if (rc.canMove(dir)) {
            rc.move(dir);
            prvDir = dir;
        } else {
            prvDir = Direction.CENTER;
        }

    }

    public static void run(RobotController rc) throws GameActionException {

        update(rc);

        if (tgtLoc == null) {
            scan(rc);
        }

        if (mode == Mode.BUILD) {
            build(rc);
        } else {
            walk(rc);
        }

        for (MapInfo tile : rc.senseNearbyMapInfos(9)) {
            final MapLocation loc = tile.getMapLocation();
            final PaintType mark = tile.getMark();
            final PaintType paint = tile.getPaint();
            if (mark != PaintType.EMPTY && paint != mark && rc.canAttack(loc)) {
                rc.attack(loc, mark == PaintType.ALLY_SECONDARY);
            }
        }

        for (MapInfo tile : rc.senseNearbyMapInfos(9)) {
            final MapLocation loc = tile.getMapLocation();
            if (tile.getMark() == PaintType.EMPTY && rnd >= 200 && rc.canAttack(loc)) {
                final boolean tp = Util.getBoost(loc);
                if (tile.getPaint() != (!tp ? PaintType.ALLY_PRIMARY : PaintType.ALLY_SECONDARY)) {
                    rc.attack(loc, tp);
                }
            }
        }

        Robot.run(rc);

    }

}
