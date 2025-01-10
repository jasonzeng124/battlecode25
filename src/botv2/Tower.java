package botv2;

import battlecode.common.*;

public class Tower {
    public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static int turnsActive = 0;

    public static void makeAction(RobotController rc) throws GameActionException {
        if (rc.isActionReady()) {
            //save up cash to make towers
            if((rc.getRoundNum()/55)%3 != 2){
                final int offset = FastMath.rand256()%8;
                for (int i = 8; --i >= 0;) {
                    MapLocation nextLoc = rc.getLocation().add(directions[i ^ offset]);
                    UnitType type = UnitType.SOLDIER;
                    if (turnsActive >= 100 && rc.getRoundNum() >= 400) {
               //         if (FastMath.fakefloat() < 0.1) {
                //            type = UnitType.MOPPER;
              //          }
                    }
                    if (rc.canBuildRobot(type, nextLoc)) {
                        rc.buildRobot(type, nextLoc);
                    }
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
        turnsActive++;
    }
}