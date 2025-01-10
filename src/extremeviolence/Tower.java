package extremeviolence;

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

    static boolean init = false;
    static MapLocation guessA, guessB;

    static ArrayList<Integer> dispatched = new ArrayList<>();
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if (!init) {
            final int x = rc.getLocation().x, y = rc.getLocation().y;
            final int width = rc.getMapWidth(), height = rc.getMapHeight();
            guessA = new MapLocation(width - x, y);
            guessB = new MapLocation(x, height - y);
            if (height > width) {
                final MapLocation tmp = guessA;
                guessA = guessB;
                guessB = tmp;
            }
            init = true;
        }

        if (rc.isActionReady()) {
            UnitType type = UnitType.SOLDIER;

            final int offset = QRand.randInt(8);
            for (int i = 0; i < 8; i++) {
                final MapLocation loc = rc.getLocation().add(directions[i ^ offset]);
                if (rc.canBuildRobot(type, loc) && rc.senseMapInfo(loc).getPaint().isAlly()) {
                    rc.buildRobot(type, loc);
                }
            }
        }

        rc.attack(null);
        for (RobotInfo robot : rc.senseNearbyRobots()) {
            final MapLocation loc = robot.getLocation();

            if (robot.team != rc.getTeam() && rc.canAttack(loc)) {
                rc.attack(loc);
                continue;
            }

            if (rc.canSendMessage(loc) && !dispatched.contains(robot.getID())) {
                int res = 0;

                res |= guessA.x;
                res |= guessA.y << 6;
                res |= guessA.x << 12;
                res |= guessB.y << 18;

                res |= 3 << 24;

                rc.sendMessage(loc, res);
                dispatched.add(robot.getID());

                rc.setIndicatorDot(loc, 0, 255, 255);
            }
        }
    }
}
