package tactician_v2;

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
    public static boolean synctrigger = false;
    public static MapLocation spawn, target;

    static boolean onTarget(MapLocation loc) {
        return loc.isWithinDistanceSquared(target, 9);
    }

    static boolean closeTarget(MapLocation loc) {
        return loc.isWithinDistanceSquared(target, 18);
    }

    public static void makeAction(RobotController rc) throws GameActionException {

        RobotInfo[] ris = rc.senseNearbyRobots();
        // Initialize estimated target location
        if (!init) {
            for (RobotInfo r : ris) {
                if (r.team == rc.getTeam() && r.type.isTowerType() && rc.getLocation().isWithinDistanceSquared(rc.getLocation(), 2)) {
                    spawn = r.getLocation();
                    break;
                }
            }
            final int width = rc.getMapWidth(), height = rc.getMapHeight();
            final int x = spawn.x, y = spawn.y;
            int targetX = x, targetY = y;
            if (Math.abs(x - width / 2) > width / 6)
                targetX = width - x - 1;
            if (Math.abs(y - height / 2) > height / 6)
                targetY = height - y - 1;
            target = new MapLocation(targetX, targetY);

            init = true;
            synctrigger = false;
        }

        // Once it comes into range, pinpoint it
        for (RobotInfo r : ris) {
            if (r.team != rc.getTeam() && r.type.isTowerType() && r.getLocation().isWithinDistanceSquared(target, 12)) {
                target = r.getLocation();
                break;
            }
        }

        if (rc.canSenseLocation(target) && !rc.canSenseRobotAtLocation(target)) {
            RobotPlayer.myJob = RobotPlayer.Job.PAWN;
        }

        if (rc.canAttack(target)) {
            rc.attack(target);
        }

        // For debug purposes
        rc.setIndicatorDot(target, 255, 0, 0);

        if (rc.isMovementReady()) {
            // Combat micro, conserve a bit of paint while constantly attacking
            if (closeTarget(rc.getLocation())) {
                RobotInfo[] ri = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                MapLocation[] towerBuf = new MapLocation[4];//towers that want to attack us
                MapLocation[] mopperBuf = new MapLocation[4];//moppers that want to attack us
                int towerPtr = 0;
                int mopperPtr = 0;
                for (RobotInfo r : ri) {
                    if (r.getType().isTowerType()) {
                        towerBuf[towerPtr] = r.getLocation();
                        towerPtr = (towerPtr + 1) % 4;
                    }
                    if (r.getType() == UnitType.MOPPER) {
                        mopperBuf[mopperPtr] = r.getLocation();
                        mopperPtr = (mopperPtr + 1) % 4;
                    }
                }

                int bestScore = -100;
                Direction bestMove = Direction.CENTER;
                for (int i = 9; --i >= 0; ) {
                    final MapLocation loc = rc.getLocation().add(DIRS[i]);
                    if (rc.canMove(DIRS[i]) && (onTarget(rc.getLocation()) || onTarget(loc))) {
                        int score = 0;
                        if (rc.senseMapInfo(loc).getPaint().isEnemy())
                            score -= 2;
                        if (rc.senseMapInfo(loc).getPaint().isAlly())
                            score += 1;
                        for (int j = 4; --j >= 0; ) {
                            if (towerBuf[j] != null && towerBuf[j].distanceSquaredTo(loc) <= 9) {
                                score -= 25;
                            }
                        }
                        for (int j = 4; --j >= 0; ) {
                            if (mopperBuf[j] != null) {
                                if (mopperBuf[j].distanceSquaredTo(loc) <= 2) {
                                    score -= 3;
                                } else if (mopperBuf[j].distanceSquaredTo(loc) <= 8) {
                                    score -= 1;
                                }
                            }
                        }
                        if (score > bestScore) {
                            bestScore = score;
                            bestMove = DIRS[i];
                        }
                    }
                }
                if (rc.getRoundNum() % 2 != 0 || synctrigger) {
                    if (rc.canMove(bestMove) && bestMove != Direction.CENTER) {
                        rc.move(bestMove);
                    }
                }
                synctrigger = true;
            } else {
                // Make a beeline for the target
                rc.move(GameUtils.greedyPath(rc, rc.getLocation(), target));
            }
        }


        if (rc.canAttack(target)) {
            rc.attack(target);
        }

    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();
        makeAction(rc);
        rc.setIndicatorString("Raider");
        assert rc.getRoundNum() == curRound : "Soldier (raider): Bytecode limit exceeded";
    }
}
