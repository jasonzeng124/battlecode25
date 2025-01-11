package mark2;

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
        if (rc.isActionReady() && (rc.getRoundNum() < 100 || rc.getMoney() >= 1300)) {
            UnitType type = UnitType.SOLDIER;
            if (rc.getRoundNum() >= 300) {
                if (QRand.randDouble() < 0.5) {
                    type = UnitType.MOPPER;
                }
            }

            final int offset = QRand.randInt(8);
            for (int i = 0; i < 8; i++) {
                final MapLocation loc = rc.getLocation().add(directions[i ^ offset]);
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
        turnsActive++;
    }
}
