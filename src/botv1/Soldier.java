package botv1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class Soldier {

    public static final int goHomePaint = 100;


    public static boolean initialized = false;
    public static final Direction[] directions = {
        Direction.WEST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.CENTER
    };

    public static final int [] popcount = {0, 1, 1, 2, 1, 2, 2, 3};
    public static final boolean [][] resourceTest = 
        {{true, false, true, false},
         {false, true, false, true},
         {true, false, false, false},
         {false, true, false, true},           
        };

    public static MapInfo[] nearbyTiles;
    public static RobotInfo[] nearbyRobots;
    public static int [] friendCnt;
    public static int [] notPainted;
    public static double [] randCosts;
    public static int randReset;
    public static double [] moveCosts;
    public static HomeNav nav;
    public static RobotInfo refuelStation;
    public static Direction way;
    public static MapLocation ruinLoc;

    public static void init(RobotController rc) throws GameActionException {
        HomeNav.init();
        friendCnt = new int [9];
        notPainted = new int [9];
        randCosts = new double [9];
        moveCosts = new double [9];
        randReset = 0;
        HomeNav.declareSource(rc.getLocation());
        initialized = true;
    }

    public static boolean isPaintable(RobotController rc, MapLocation loc) throws GameActionException{
        if(rc.canAttack(loc)){
            MapInfo mi = rc.senseMapInfo(loc);
            if(mi.getPaint() == PaintType.EMPTY || (mi.getMark() != PaintType.EMPTY && mi.getPaint() != mi.getMark())){
                if(mi.hasRuin()){
                    return (rc.canSenseRobotAtLocation(loc) && !rc.senseRobotAtLocation(loc).getTeam().isPlayer());
                }else{
                    return true;
                }
            }
        }
        return false;
    }

    public static void sense(RobotController rc) throws GameActionException {
        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();
        for(int i = 9; --i >= 0;){
            friendCnt[i] = 0;
            notPainted[i] = 0;
            if(randReset == 0){
                randCosts[i] = FastMath.fakefloat() * 20.0;
            }
        }
        if(randReset == 0){
            randReset = 8;
        }
        randReset--;
        ruinLoc = null;
        for (MapInfo tile : nearbyTiles){
            if(isPaintable(rc, tile.getMapLocation())){
             //   rc.setIndicatorDot(tile.getMapLocation(), 255, 0, 255);
                MapLocation dif = FastMath.addVec(FastMath.minusVec(tile.getMapLocation(), rc.getLocation()), new MapLocation(4, 4));
                notPainted[dif.x] ^= (1 << dif.y);
            }
            if(tile.hasRuin() && !rc.canSenseRobotAtLocation(tile.getMapLocation())){
                ruinLoc = tile.getMapLocation();
            }
        }
        refuelStation = null;
        for (RobotInfo robot : nearbyRobots){
            if(robot.team.isPlayer()){
                if(robot.type.paintPerTurn != 0){
                    refuelStation = robot;
                    HomeNav.declareSource(robot.location.add(directions[FastMath.rand256()&7]));
                }
                MapLocation dif = FastMath.addVec(FastMath.minusVec(robot.location, rc.getLocation()), new MapLocation(4, 4));
                friendCnt[dif.x] ^= (1 << dif.y);
            }
        }
        friendCnt[4] ^= 16;
    }

    public static int countFriends(int x, int y){
        y--;
        return  popcount[(friendCnt[x-1] >> (y)) & 7] + popcount[(friendCnt[x] >> (y)) & 7] + popcount[(friendCnt[x+1] >> (y)) & 7];
    }

    public static int countNotPainted(int x, int y){
        y--;
        return  popcount[(notPainted[x-1] >> (y)) & 7] + popcount[(notPainted[x] >> (y)) & 7] + popcount[(notPainted[x+1] >> (y)) & 7];
    }

    public static void applyPaintCosts(RobotController rc) throws GameActionException {
        for(int i = 9; --i >= 0;){
            //firstly, if you can't move that way, you are trash
            if(rc.canMove(directions[i]) == false){
                moveCosts[i] = 1000000.0;
                continue;
            }
            double paintMultiplier = 10 - rc.getPaint()/20.0;
            if(rc.getPaint() <= 100){
                paintMultiplier = 50 - 7.0 * rc.getPaint()/20.0;
            }
            //considering friends
            {
                int x = 4+directions[i].dx;
                int y = 4+directions[i].dy;
                int numfriends = countFriends(x, y);
                PaintType p = rc.senseMapInfo(rc.getLocation().add(directions[i])).getPaint();
                //how much ouchies do we get?
                double paintCost = 0;
                switch(p){
                    case EMPTY: paintCost = 1.0; break;
                    case ALLY_PRIMARY: case ALLY_SECONDARY: paintCost = 0.0; break;
                    default: paintCost = 2.0 + numfriends * 2.0; break;
                }
                moveCosts[i] += paintMultiplier * paintCost;
                //also it is kinda bad to restrict movement of others
                if(numfriends >= 2){
                    moveCosts[i] += numfriends * 5;
                }
            }
            if(i == 8){
                //unless you have a really good reason, don't sitting duck
                moveCosts[i] += 20;
            }
        }
    }

    public static void applyExploreCosts(RobotController rc) throws GameActionException {
        if((((notPainted[2] >> 2) & 31) == 0) && (((notPainted[3] >> 2) & 31) == 0) && (((notPainted[4] >> 2) & 31) == 0) && (((notPainted[5] >> 2) & 31) == 0) && (((notPainted[6] >> 2) & 31) == 0)){
          //  String s = "";
            for(int i = 9; --i >= 0;){
                int x = 4+3*directions[i].dx;
                int y = 4+3*directions[i].dy;
                int c = countNotPainted(x, y);
             //   s += c;
              //  s += " ";
                if(c == 0){
                    moveCosts[i] += 60.0;
                }else{
                    moveCosts[i] += (5.0 * (3 - c) * (3 - c));
                }
            }
         //   rc.setIndicatorString("F " + s);
        }else{
          //  String s = "";
            for(int i = 9; --i >= 0;){
                int x = 4+directions[i].dx;
                int y = 4+directions[i].dy;
                int c = countNotPainted(x, y);
           //     s += c;
            //    s += " ";
                if(c == 0){
                    moveCosts[i] += 60.0;
                }else{
                    moveCosts[i] += (5.0 * (3 - c) * (3 -c));
                }
            }
          //  rc.setIndicatorString("E " + s);
        }
        if(ruinLoc != null){
            Direction dir = rc.getLocation().directionTo(ruinLoc);
            int a = dir.getDirectionOrderNum()-1;
            int a2 = a - 1;
            int a3 = a + 1;
            if(a == 0){
                a2 = 7;
                a3 = 1;
            }else if(a == 7){
                a2 = 6;
                a3 = 0;
            }
            moveCosts[a2] -= 70.0;
            moveCosts[a] -= 200.0;
            moveCosts[a3] -= 70.0;
        }
    }

    public static void applyGoHomeCosts(RobotController rc) throws GameActionException {
        int curDist = HomeNav.getDist(rc.getLocation());
        for(int i = 9; --i >= 0;){
            if(rc.canMove(directions[i])){
                int newDist = HomeNav.getDist(rc.getLocation().add(directions[i]));
                moveCosts[i] += Math.min(newDist - curDist, 2) * 50.0;
            }
        }
    }

    public static void applyRandomCosts(RobotController rc) throws GameActionException{
        for(int i = 9; --i >= 0;){
            moveCosts[i] += randCosts[i];
        }
    }

    public static void thinkAboutWay(RobotController rc) throws GameActionException {
        for(int i = 9; --i >= 0;){
            moveCosts[i] = 0;
            if(rc.canMove(directions[i]) == false){
                moveCosts[i] = 1000000.0;
                continue;
            }
        }
        applyPaintCosts(rc);
        if(rc.getPaint() >= 100){
            applyExploreCosts(rc);
        }else{
            applyGoHomeCosts(rc);
        }
        applyRandomCosts(rc);
        way = Direction.CENTER;
        double lowestCost = 1000000;
        for(int i = 9; --i >= 0;){
            if(moveCosts[i] < lowestCost){
                lowestCost = moveCosts[i];
                way = directions[i];
            }
        //    rc.setIndicatorDot(rc.getLocation().add(directions[i]), (int)(moveCosts[i]), -(int)(moveCosts[i]), 255);
        }       
    }

    public static boolean tryToPaint(RobotController rc, MapLocation loc) throws GameActionException{
        if(isPaintable(rc, loc)){
           // rc.setIndicatorString(loc.x + " " + loc.y);
          //  rc.setIndicatorDot(loc, 255, 0, 255);
            PaintType p = rc.senseMapInfo(loc).getMark();
            if(p == PaintType.EMPTY){
                rc.attack(loc, (((loc.x+loc.y)%2 == 0) && (((3*loc.x+loc.y)%10) != 0)));
            }else{
                rc.attack(loc, (p == PaintType.ALLY_SECONDARY));
            }
            return true;
        }
        return false;
    }

    public static void act(RobotController rc) throws GameActionException {
        MapLocation prevLoc = rc.getLocation();
        if(rc.canMove(way)){
            rc.move(way);
            MapLocation curLoc = rc.getLocation();
            HomeNav.recordMove(curLoc, prevLoc);
        }
        if(ruinLoc != null && rc.getLocation().isAdjacentTo(ruinLoc)){
            if(rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruinLoc)){
                if(FastMath.rand256() < 192){
                    rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruinLoc);
                }else{
                    rc.markTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, ruinLoc);
                }
            }else{
                if(rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruinLoc)){
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruinLoc);
                }
                if(rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, ruinLoc)){
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, ruinLoc);
                }
            }
        }
        if(rc.getPaint() >= 60){
            if(!tryToPaint(rc, rc.getLocation())){
                if(ruinLoc != null){
                    int offset = FastMath.rand256()%nearbyTiles.length;
                    for(int i = nearbyTiles.length; --i >= 0;){
                        if(nearbyTiles[i].getMapLocation().isWithinDistanceSquared(ruinLoc, 8) && tryToPaint(rc, nearbyTiles[i].getMapLocation())){
                            break;
                        }
                    }
                }else{
                    int offset = FastMath.rand256()%nearbyTiles.length;
                    for(int i = nearbyTiles.length; --i >= 0;){
                        if(tryToPaint(rc, nearbyTiles[i].getMapLocation())){
                            break;
                        }
                    }
                }
            }
        }
        //withdraw paint
        if(refuelStation != null && refuelStation.paintAmount + rc.getPaint() >= 205){
            if(rc.canTransferPaint(refuelStation.location, rc.getPaint() - 200)){
                //transfer time
                rc.transferPaint(refuelStation.location, rc.getPaint() - 200);
            }
        }
        //declare resource boosts
        for(int i = 9; --i >= 0;){
            MapLocation loc = rc.getLocation().add(directions[i]);
            if(rc.canCompleteResourcePattern(loc)){
                rc.completeResourcePattern(loc);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if(!initialized){
            init(rc);
        }
        sense(rc);
        thinkAboutWay(rc);
        act(rc);
    }
}
