package jason0;

import battlecode.common.*;


public class Tower {
    public static RobotController rc;

    public static boolean initialized = false;
    public static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
        Direction.CENTER,
    };

    public static MapInfo[] nearbyTiles;
    public static RobotInfo[] nearbyRobots;

    public static void init() throws GameActionException {
        initialized = true;
    }

    public static void sense() throws GameActionException {
        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();
    }

    public static void think() throws GameActionException {
    }

    public static void act() throws GameActionException {
        if(rc.senseRobotAtLocation(rc.getLocation()).type.paintPerTurn > 0
            && (rc.getRoundNum() > 100 || rc.getChips() > 1000)){
            int offset = FastRand.rand256() & 7;
            for(int i = 8; --i >= 0;){
                MapLocation nextLoc = rc.getLocation().add(directions[i^offset]);
                if (rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
                    rc.buildRobot(UnitType.SOLDIER, nextLoc);
                }
            }
        }
        rc.attack(null);
    }

    @SuppressWarnings("unused")
    public static void run() throws GameActionException {
        if(!initialized) init();
        sense();
        think();
        act();
    }
}
