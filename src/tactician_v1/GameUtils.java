package tactician_v1;

import battlecode.common.*;

public class GameUtils {
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

    // Player tile checks
    public static boolean isMyColor(PaintType p) {
        return p == PaintType.ALLY_PRIMARY || p == PaintType.ALLY_SECONDARY;
    }
    public static boolean isMyTile(MapInfo m) {
        return isMyColor(m.getPaint());
    }
    public static boolean hasMyTile(RobotController rc, MapLocation l) throws GameActionException {
        return isMyColor(rc.senseMapInfo(l).getPaint());
    }

    // Enemy tile checks
    public static boolean isEnemyColor(PaintType p) {
        return p == PaintType.ENEMY_PRIMARY || p == PaintType.ENEMY_SECONDARY;
    }
    public static boolean isEnemyTile(MapInfo m) {
        return isEnemyColor(m.getPaint());
    }
    public static boolean hasEnemyTile(RobotController rc, MapLocation l) throws GameActionException {
        return isEnemyColor(rc.senseMapInfo(l).getPaint());
    }

    // For recognizing potential recharge stations
    public static boolean hasAllyPaintTower(RobotController rc, MapLocation loc) throws GameActionException {
        if (!rc.canSenseRobotAtLocation(loc)) {
            return false;
        }
        final RobotInfo r = rc.senseRobotAtLocation(loc);
        return r.getTeam() == rc.getTeam() && (r.getType() == UnitType.LEVEL_ONE_PAINT_TOWER || r.getType() == UnitType.LEVEL_TWO_PAINT_TOWER || r.getType() == UnitType.LEVEL_THREE_PAINT_TOWER);
    }

    // Greedy pathing
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
}
