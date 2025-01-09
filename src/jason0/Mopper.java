package jason0;

import battlecode.common.*;


public class Mopper {
    public static RobotController rc;

    public static boolean initialized = false;

    public static void init() throws GameActionException {
        initialized = true;
    }

    public static void sense() throws GameActionException {
        
    }

    public static void think() throws GameActionException {

    }

    public static void act() throws GameActionException {

    }

    @SuppressWarnings("unused")
    public static void run() throws GameActionException {
        if(!initialized) init();
        sense();
        think();
        act();
    }
}
