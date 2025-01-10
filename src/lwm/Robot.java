package lwm;

import battlecode.common.*;

public class Robot {

    public static void run(RobotController rc) throws GameActionException {

        for (MapInfo tile : rc.senseNearbyMapInfos(4)) {
            final MapLocation loc = tile.getMapLocation();
            if (tile.hasRuin()) {
                if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, loc)) {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, loc);
                }
                if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, loc)) {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, loc);
                }
                if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, loc)) {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_DEFENSE_TOWER, loc);
                }
            }
            final RobotInfo bot = rc.senseRobotAtLocation(loc);
            if (bot != null && bot.getTeam() == rc.getTeam()) {
                final UnitType tp = bot.getType();
                if (tp == UnitType.SOLDIER || tp == UnitType.MOPPER || tp == UnitType.SPLASHER) {
                    continue;
                }
                if (rc.canUpgradeTower(loc)) {
                    rc.upgradeTower(loc);
                }
                for (MapInfo curTile : rc.senseNearbyMapInfos(rc.getLocation(), 4)) {
                    final MapLocation curLoc = curTile.getMapLocation();
                    if (loc.distanceSquaredTo(curLoc) <= 8 && rc.canRemoveMark(curLoc)) {
                        rc.removeMark(curLoc);
                    }
                }
            }
            if ((loc.x * 3 + loc.y) % 10 == 0 && rc.canCompleteResourcePattern(loc)) {
                rc.completeResourcePattern(loc);
            }
        }

    }

}
