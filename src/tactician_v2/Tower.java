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

    static XorShiftRNG rng;
    static MapLocation origin;
    static ArrayList<Integer> dispatched = new ArrayList<>();

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // Initialize stuff
        if (rng == null) {
            rng = new XorShiftRNG(rc);
        }
        if (origin == null) {
            if (rc.getRoundNum() == 1) {
                origin = rc.getLocation();
            } else {
                for (Message msg : rc.readMessages(-1)) {
                    final int data = msg.getBytes();
                    origin = new MapLocation(data & 63, (data >> 6) & 63);
                    break;
                }
            }
        }

        final int curRound = rc.getRoundNum();

        // Attack nearby enemies
        rc.attack(null);
        for (RobotInfo robot : rc.senseNearbyRobots()) {
            final MapLocation loc = robot.getLocation();

            if (robot.team != rc.getTeam() && rc.canAttack(loc)) {
                rc.attack(loc);
            }
        }

        // Spawn units
        if (rc.isActionReady() && (curRound < 50 || (rc.getType().paintPerTurn > 0 && rc.getMoney() >= 1000))) {
            UnitType type = UnitType.SOLDIER;

            if (curRound >= rc.getMapWidth() * rc.getMapHeight() / 12) {
                final float r = rng.nextFloat();
                if (r < 0.3) {
                    type = UnitType.MOPPER;
                } else if (r < 0.6) {
                    type = UnitType.SPLASHER;
                }
            }

            final int offset = rng.nextInt(8);
            for (int i = 0; i < 8; i++) {
                final MapLocation loc = rc.getLocation().add(directions[i ^ offset]);
                if (rc.canBuildRobot(type, loc)) {
                    rc.buildRobot(type, loc);
                }
            }
        }

        // Share the origin
        if (origin != null) {
            for (RobotInfo r : rc.senseNearbyRobots(rc.getLocation(), 9, rc.getTeam())) {
                if (rc.canSendMessage(r.getLocation())) {
                    rc.sendMessage(r.getLocation(), origin.x | (origin.y << 6));
                }
            }
        }
    }
}
