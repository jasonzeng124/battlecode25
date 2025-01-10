package tactician_v1;

import battlecode.common.*;

public class Raider {
    public static final Direction[] DIRS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    public static MapLocation guessA, guessB;
    public static boolean go = false;
    public static boolean doneA = false, doneB = false;

    static Direction greedyPath(RobotController rc, MapLocation a, MapLocation b) {
        final int idx = a.directionTo(b).ordinal();
        int[] order = {
                idx,
                idx + 7, idx + 1,
                idx + 6, idx + 2,
                idx + 5, idx + 3,
                idx + 4
        };
        for (int i = 0; i < 8; i++) {
            if (order[i] >= 8) {
                order[i] -= 8;
            }
            if (rc.canMove(DIRS[order[i]])) {
                return DIRS[order[i]];
            }
        }
        return null;
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        for (Message msg : rc.readMessages(-1)) {
            final int b = msg.getBytes();
            guessA = new MapLocation(b & 63, (b >> 6) & 63);
            guessB = new MapLocation((b >> 12) & 63, (b >> 18) & 63);
            go = true;
        }

        if (rc.isMovementReady() && go) {
            rc.move(greedyPath(rc, rc.getLocation(), doneA ? guessB : guessA));
            rc.setIndicatorDot(guessA, 255, 0, 0);
            rc.setIndicatorDot(guessB, 0, 255, 0);
        }

        if (rc.isActionReady() && go) {
            boolean attacked = false;

            for (RobotInfo robot : rc.senseNearbyRobots()) {
                final MapLocation loc = robot.getLocation();

                if (robot.team != rc.getTeam() && rc.canAttack(loc)) {
                    rc.attack(loc);
                    attacked = true;
                    break;
                }
            }

            if (rc.getLocation().isWithinDistanceSquared(guessA, 9) && !doneA && !attacked) {
                doneA = true;
            }
            if (rc.getLocation().isWithinDistanceSquared(guessB, 9) && !doneB && !attacked) {
                doneB = true;
            }
        }

        if (doneA && doneB) {
            RobotPlayer.myJob = RobotPlayer.Job.PAWN;
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();
        makeAction(rc);
        assert rc.getRoundNum() == curRound : "Soldier (raider): Bytecode limit exceeded";
    }
}
