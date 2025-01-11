package tactician_v1;

import battlecode.common.*;

public class Pawn {
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

    public static boolean defaultColor(MapLocation loc) {
        return ((loc.x + loc.y) % 2 == 0) && (((3 * loc.x + loc.y) % 10) != 0);
    }

    public static void makeAction(RobotController rc) throws GameActionException {
        for (MapInfo tile : rc.senseNearbyMapInfos()) {
            final MapLocation loc = tile.getMapLocation();

            if (rc.canAttack(loc) && tile.getPaint() == PaintType.EMPTY && !tile.hasRuin()) {
                rc.attack(loc, defaultColor(loc));
                break;
            }
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        final int curRound = rc.getRoundNum();
        makeAction(rc);
        assert rc.getRoundNum() == curRound : "Soldier (pawn): Bytecode limit exceeded";
    }
}
