package tactician_v2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

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

    static ArrayList<Integer> dispatched = new ArrayList<>();
    static Random rand = new Random();
    static UnitType next = UnitType.SOLDIER;
    static int chipsPerSecond = -1;
    static int prevChips = 1000000000;


    public static void spawn(RobotController rc) throws GameActionException {
        final int offset = FastMath.rand256() % 8;
        for (int i = 0; i < 8; i++) {
            final MapLocation loc = rc.getLocation().add(directions[i ^ offset]);
            if (rc.canBuildRobot(next, loc)) {
                rc.buildRobot(next, loc);
                if(rc.getType().paintPerTurn == 0){
                    if(rc.getPaint() < 200){
                        next = UnitType.MOPPER;
                    }else{
                        next = UnitType.SOLDIER;
                    }
                }else{
                    next = UnitType.SOLDIER;
                    if (rc.getRoundNum() >= rc.getMapWidth() * rc.getMapHeight() / 12) {
                        final double val = rand.nextDouble();
                        if (val < 0.3) {
                            next = UnitType.MOPPER;
                        } else if (val < 0.6) {
                            next = UnitType.SPLASHER;
                        }
                    }
                }
            }
        }
    }

    public static void countChips(RobotController rc) throws GameActionException {
        int curChips = rc.getMoney();
        if((curChips - prevChips) > chipsPerSecond){
            chipsPerSecond = (curChips - prevChips);
        }
        prevChips = curChips;
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        countChips(rc);
        rc.attack(null);
        boolean hit = false;
        for (RobotInfo robot : rc.senseNearbyRobots()) {
            final MapLocation loc = robot.getLocation();
            if (robot.team != rc.getTeam()){
                if(rc.canAttack(loc)) {
                    rc.attack(loc);
                }
                hit = true;
            }
        }
        //build some moppers to scare away the soldiers
        if(hit){
            final int offset = FastMath.rand256() % 8;
            for (int i = 0; i < 8; i++) {
                final MapLocation loc = rc.getLocation().add(directions[i ^ offset]);
                if (rc.canBuildRobot(UnitType.MOPPER, loc)) {
                    rc.buildRobot(UnitType.MOPPER, loc);
                }
            }
        }
        //build something
        if (rc.isActionReady()){
            if(rc.getRoundNum() < 100){
                if(rc.getMoney() >= 1000){ 
                    spawn(rc);
                }
            }else{
                if(rc.getType().paintPerTurn > 0){
                    if(rc.getMoney() >= 100 * rc.getNumberTowers()){
                        spawn(rc);
                    }
                }else{
                    if(rc.getRoundNum()%20 == 0 && rc.getMoney() >= 100 * rc.getNumberTowers()){
                        spawn(rc);
                    }
                }
            }

        }

        
    }
}
