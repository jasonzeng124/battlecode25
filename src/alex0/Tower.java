package alex0;

import battlecode.common.*;


public class Tower {

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
    };


    public static void init(RobotController rc) throws GameActionException {
        initialized = true;
    }

    //sense surroundings
    public static void sense(RobotController rc) throws GameActionException {
        
    }

    public static void think(RobotController rc) throws GameActionException {

    }

    public static void act(RobotController rc) throws GameActionException {
        if(rc.senseRobotAtLocation(rc.getLocation()).type.paintPerTurn > 0){
            int offset = FastMath.rand256()&7;
            for(int i = 8; --i >= 0;){
                MapLocation nextLoc = rc.getLocation().add(directions[i^offset]);
                if (rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
                    rc.buildRobot(UnitType.SOLDIER, nextLoc);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if(!initialized){
            init(rc);
        }
        sense(rc);
        think(rc);
        act(rc);
    }
}
