package lwm;

import battlecode.common.*;

public class Tower {

    public static void run(RobotController rc) throws GameActionException {

        if (rc.isActionReady() && rc.getRoundNum() % 2 == 0) {
            for (Direction x : Util.dirs) {
                final MapLocation loc = rc.getLocation().add(x);
                if (rc.canBuildRobot(UnitType.SOLDIER, loc)) {
                    rc.buildRobot(UnitType.SOLDIER, loc);
                }
            }
        }

    }

}
