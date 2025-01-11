package tactician_v1;

import battlecode.common.*;

import java.util.ArrayList;

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

    static ArrayList<Integer> dispatched = new ArrayList<>();
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        FastMath.initRand(rc);

        if (rc.isActionReady() && (rc.getRoundNum() < 100 || rc.getMoney() >= 1300)) {
            UnitType type = UnitType.SOLDIER;

            if (rc.getRoundNum() >= 200 && FastMath.fakefloat() < 0.3)
                type = UnitType.MOPPER;

            final int offset = FastMath.rand256() % 8;
            for (int i = 0; i < 8; i++) {
                final MapLocation loc = rc.getLocation().add(directions[i ^ offset]);
                if (rc.canBuildRobot(type, loc)) {
                    rc.buildRobot(type, loc);
                }
            }
        }

        rc.attack(null);
        for (RobotInfo robot : rc.senseNearbyRobots()) {
            final MapLocation loc = robot.getLocation();

            if (robot.team != rc.getTeam() && rc.canAttack(loc)) {
                rc.attack(loc);
            }
        }
    }
}
