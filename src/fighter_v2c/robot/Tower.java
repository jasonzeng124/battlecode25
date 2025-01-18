package fighter_v2c.robot;

import battlecode.common.*;
import fighter_v2c.util.FastRand;
import fighter_v2c.util.FastIntSet;
import fighter_v2c.util.FastIterableLocSet;
import fighter_v2c.util.FastLocSet;

public class Tower extends Robot {
    public Tower(RobotController rc) {
        super(rc);
    }

    int numUsage = 0;

    @Override
    public void function() throws GameActionException {
        rc.attack(null);
        for (RobotInfo rob : rc.senseNearbyRobots(rc.getLocation(), -1, oppTeam)) {
            if (rc.canAttack(rob.getLocation())) {
                rc.attack(rob.getLocation());
            }
        }
        numUsage += rc.senseNearbyRobots(rc.getLocation(), -1, myTeam).length;
        numUsage = (numUsage * 49)/50;
        addIndicatorField("Numusage: " + numUsage);
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
