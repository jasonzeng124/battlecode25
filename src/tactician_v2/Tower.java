package tactician_v2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

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
    static Random rand = new Random();
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        FastMath.initRand(rc);

        if (
            rc.isActionReady() &&
            (rc.getRoundNum() < 100 || (rc.getType().paintPerTurn > 0 && rc.getMoney() >= 1300 && rc.getPaint() >= 300))
        ) {
            UnitType type = UnitType.SOLDIER;

            if (rc.getRoundNum() >= rc.getMapWidth() * rc.getMapHeight() / 12) {
                final double val = rand.nextDouble();
                if (val < 0.3) {
                    type = UnitType.MOPPER;
                } else if (val < 0.6) {
                    type = UnitType.SPLASHER;
                }
            }

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
