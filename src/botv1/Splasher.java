package botv1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class Splasher {

    public static boolean initialized = false;
    
    public static void init(RobotController rc) throws GameActionException {
        initialized = true;
    }

    //sense surroundings
    public static void sense(RobotController rc) throws GameActionException {
        
    }

    public static void think(RobotController rc) throws GameActionException {

    }

    public static void act(RobotController rc) throws GameActionException {

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
