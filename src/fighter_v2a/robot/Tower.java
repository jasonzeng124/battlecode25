package fighter_v2a.robot;

import battlecode.common.*;
import fighter_v2a.util.FastRand;
import fighter_v2a.util.FastIntSet;
import fighter_v2a.util.FastIterableLocSet;
import fighter_v2a.util.FastLocSet;

public class Tower extends Robot {
    public Tower(RobotController rc) {
        super(rc);
    }

    @Override
    public void function() throws GameActionException {
        rc.attack(null);
        for (RobotInfo rob : rc.senseNearbyRobots(rc.getLocation(), -1, rc.getTeam().opponent())) {
            if (rc.canAttack(rob.getLocation())) {
                rc.attack(rob.getLocation());
            }
        }

        if (rc.isActionReady() && (rc.getRoundNum() < 50 || (rc.getChips() >= 1300 && rc.getPaint() >= 500))) {
            for (MapInfo tile : rc.senseNearbyMapInfos(rc.getLocation(), 2)) {
                MapLocation loc = tile.getMapLocation();
//                UnitType type = rc.getRoundNum() >= 50 && FastRand.next256() < 32 ? UnitType.MOPPER : UnitType.SOLDIER;
                UnitType type = UnitType.SOLDIER;

                if (rc.canBuildRobot(type, loc)) {
                    rc.buildRobot(type, loc);
                    return;
                }
            }
        }
    }
}
