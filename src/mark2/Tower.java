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
        if (rc.senseRobotAtLocation(rc.getLocation()).type.paintPerTurn > 0) {
            final int offset = QRand.randInt(8);
            for (int i = 7; i >= 0; i--) {
                MapLocation nextLoc = rc.getLocation().add(directions[i ^ offset]);
                UnitType type = UnitType.SOLDIER;
                if (turnsActive >= 250 && QRand.randDouble() < 0.6) {
                    type = UnitType.MOPPER;
                }
                if (rc.canBuildRobot(type, nextLoc)) {
                    rc.buildRobot(type, nextLoc);
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
