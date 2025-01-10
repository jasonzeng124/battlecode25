package proto1;

import battlecode.common.*;

public class Tower {
    public static void makeAction(RobotController rc) throws GameActionException {
        if (rc.isActionReady()) {
            UnitType type = UnitType.SOLDIER;

            final int offset = MyRand.randInt(8);
            for (int i = 0; i < 8; i++) {
                final MapLocation loc = rc.getLocation().add(Precomp.DIRS[i ^ offset]);
                if (rc.canBuildRobot(type, loc)) {
                    rc.buildRobot(type, loc);
                }
            }
        }

        rc.attack(null);
        for (RobotInfo robot : rc.senseNearbyRobots()) {
            if (robot.getTeam() != rc.getTeam() && rc.getLocation().isWithinDistanceSquared(robot.getLocation(), 9)) {
                rc.attack(robot.getLocation());
                break;
            }
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        makeAction(rc);
    }
}
