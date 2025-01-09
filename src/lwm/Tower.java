package lwm;

import battlecode.common.*;

public class Tower {

    public static void run(RobotController rc) throws GameActionException {

        if (rc.isActionReady()) {
            for (Direction x : Util.dirs) {
                final MapLocation loc = rc.getLocation().add(x);
                if (rc.canBuildRobot(UnitType.SOLDIER, loc)) {
                    rc.buildRobot(UnitType.SOLDIER, loc);
                }
            }
        }

    }

}
