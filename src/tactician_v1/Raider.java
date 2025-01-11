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
            Direction.NORTHWEST,
            Direction.CENTER
    };

    public static boolean init = false;
    public static MapLocation target;

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

    static boolean onTarget(MapLocation loc) {
        return loc.isWithinDistanceSquared(target, 9);
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        // Initialize estimated target location
        if (!init) {
            MapLocation loc = rc.getLocation();
            for (RobotInfo r : rc.senseNearbyRobots()) {
                if (r.team.isPlayer() && r.type.isTowerType()) {
                    loc = r.getLocation();
                    break;
                }
            }
            final int width = rc.getMapWidth(), height = rc.getMapHeight();
            final int x = loc.x, y = loc.y;
            int targetX = x, targetY = y;
            if (Math.abs(x - width / 2) > width / 5)
                targetX = width - x - 1;
            if (Math.abs(y - height / 2) > height / 5)
                targetY = height - y - 1;
            target = new MapLocation(targetX, targetY);

            init = true;
        }

        // Once it comes into range, pinpoint it
        for (RobotInfo r : rc.senseNearbyRobots()) {
            if (r.team != rc.getTeam() && r.type.isTowerType()) {
                target = r.getLocation();
                break;
            }
        }

        if (rc.isMovementReady()) {
            // Combat micro, conserve a bit of paint while constantly attacking
            if (onTarget(rc.getLocation())) {
                int bestScore = -1;
                Direction bestMove = null;
                for (int i = 0; i < 9; i++) {
                    final MapLocation loc = rc.getLocation().add(DIRS[i]);
                    if (rc.canMove(DIRS[i]) && onTarget(loc)) {
                        int score = 0;
                        if (rc.senseMapInfo(loc).getPaint().isEnemy())
                            score -= 2;
                        if (rc.senseMapInfo(loc).getPaint().isAlly())
                            score += 2;
                        if (rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).type == UnitType.MOPPER)
                            score -= 1;
                        if (score > bestScore) {
                            bestScore = score;
                            bestMove = DIRS[i];
                        }
                    }
                }
                rc.move(bestMove);
            } else {
                // Make a beeline for the target
                rc.move(greedyPath(rc, rc.getLocation(), target));
                rc.setIndicatorDot(target, 255, 0, 0);
                rc.setIndicatorDot(target, 0, 255, 0);
            }
        }

        // We're on the target, attack
        if (rc.isActionReady() && onTarget(rc.getLocation())) {
            boolean attacked = false;

            for (RobotInfo robot : rc.senseNearbyRobots()) {
                final MapLocation loc = robot.getLocation();

                if (robot.type.isTowerType() && robot.team != rc.getTeam() && rc.canAttack(loc)) {
                    rc.attack(loc);
                    attacked = true;
                    break;
                }
            }

            // The target is dead
            if (!attacked) {
                RobotPlayer.myJob = RobotPlayer.Job.PAWN;
            }
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();
        makeAction(rc);
        assert rc.getRoundNum() == curRound : "Soldier (raider): Bytecode limit exceeded";
    }
}
