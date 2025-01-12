package jason0;

import battlecode.common.*;


public class Soldier {
    public static RobotController rc;
/* 

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

    public static final int [] popcount = {0, 1, 1, 2, 1, 2, 2, 3};

    public static MapInfo[] nearbyTiles;
    public static RobotInfo[] nearbyRobots;
    public static int [] friendCnt;
    public static int [] notPainted;
    public static double [] randCosts;
    public static int randReset;
    public static double [] moveCosts;
    public static MapLocation ruinLoc;

    public static int offshore = 0;

    public static void init() throws GameActionException {
        friendCnt = new int [9];
        notPainted = new int [9];
        randCosts = new double [9];
        moveCosts = new double [9];
        randReset = 0;
        HomeNav.declareSource(rc.getLocation());
        initialized = true;
    }

    static boolean isPaintable(MapLocation loc) throws GameActionException {
        if (!rc.canAttack(loc)) {
            return false;
        }

        final MapInfo mi = rc.senseMapInfo(loc);
        return mi.getPaint() == PaintType.EMPTY ||
            (mi.getPaint().isAlly() && mi.getMark() != PaintType.EMPTY && mi.getPaint() != mi.getMark());
    }

    public static void sense() throws GameActionException {
        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();
        for(int i = 9; --i >= 0;) {
            friendCnt[i] = 0;
            notPainted[i] = 0;
            if(randReset == 0) {
                randCosts[i] = FastRand.randfloat() * 20.0;
            }
        }
        if(randReset == 0) randReset = 8;
        randReset--;

        for (MapInfo tile : nearbyTiles){
            if(isPaintable(tile.getMapLocation())) {
                MapLocation dif = FastMath.addVec(FastMath.minusVec(tile.getMapLocation(), rc.getLocation()), new MapLocation(4, 4));
                notPainted[dif.x] ^= (1 << dif.y);
            }
            if(tile.hasRuin() && !rc.canSenseRobotAtLocation(tile.getMapLocation()))
                ruinLoc = tile.getMapLocation();
        }
        refuelStation = null;
        for (RobotInfo robot : nearbyRobots){
            if(robot.team.isPlayer()){
                if(robot.type.paintPerTurn != 0){
                    refuelStation = robot;
                    HomeNav.declareSource(robot.location.add(directions[FastRand.rand256()&7]));
                }
                MapLocation dif = FastMath.addVec(FastMath.minusVec(robot.location, rc.getLocation()), new MapLocation(4, 4));
                friendCnt[dif.x] ^= (1 << dif.y);
            }
        }
        friendCnt[4] ^= 16;

        MapLocation curLoc = rc.getLocation();
        MapInfo curTile = rc.senseMapInfo(curLoc);
        if(curTile.getPaint() == PaintType.EMPTY)
            offshore += 1;
        else if(curTile.getPaint().isEnemy())
            offshore += 3;
        else
            offshore = 0;
        rc.setIndicatorString("Offshore " + offshore);
    }

    public static int countFriends(int x, int y){
        y--;
        return  popcount[(friendCnt[x-1] >> (y)) & 7] + popcount[(friendCnt[x] >> (y)) & 7] + popcount[(friendCnt[x+1] >> (y)) & 7];
    }

    public static int countNotPainted(int x, int y){
        y--;
        return  popcount[(notPainted[x-1] >> (y)) & 7] + popcount[(notPainted[x] >> (y)) & 7] + popcount[(notPainted[x+1] >> (y)) & 7];
    }

    public static void applyPaintCosts() throws GameActionException {
        for(int i = 9; --i >= 0;){
            Direction dir = directions[i];
            if(rc.canMove(dir) == false){
                moveCosts[i] = 1000000.0;
                continue;
            }
            double paintMultiplier = 10 - rc.getPaint()/20.0;
            if(rc.getPaint() <= 100){
                paintMultiplier = 50 - 7.0 * rc.getPaint()/20.0;
            }
            //considering friends
            {
                int x = 4+dir.dx;
                int y = 4+dir.dy;
                int numfriends = countFriends(x, y);
                PaintType p = rc.senseMapInfo(rc.getLocation().add(dir)).getPaint();
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
        }
        //unless you have a really good reason, don't sitting duck
        moveCosts[8] += 20;
    }

    public static void applyExploreCosts() throws GameActionException {
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
        if(ruinLoc != null && rc.getChips() > 800){
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

    public static void applyGoHomeCosts() throws GameActionException {
        int curDist = HomeNav.getDist(rc.getLocation());
        for(int i = 9; --i >= 0;){
            if(rc.canMove(directions[i])){
                int newDist = HomeNav.getDist(rc.getLocation().add(directions[i]));
                moveCosts[i] += Math.min(newDist - curDist, 2) * 50.0;
            }
        }
    }

    public static void applyRandomCosts() throws GameActionException{
        for(int i = 9; --i >= 0;){
            moveCosts[i] += randCosts[i];
        }
    }

    public static void think() throws GameActionException {
        for(int i = 9; --i >= 0;){
            moveCosts[i] = 0;
            if(rc.canMove(directions[i]) == false){
                moveCosts[i] = 1000000.0;
                continue;
            }
        }
        applyPaintCosts();
        if(rc.getPaint() - offshore >= 100){
            applyExploreCosts();
        }else{
            applyGoHomeCosts();
        }
        applyRandomCosts();
        way = Direction.CENTER;
        double lowestCost = 1000000;
        for(int i = 9; --i >= 0;){
            if(moveCosts[i] < lowestCost){
                lowestCost = moveCosts[i];
                way = directions[i];
            }
            if(rc.onTheMap(rc.getLocation().add(directions[i])))
                rc.setIndicatorDot(rc.getLocation().add(directions[i]), (int)(moveCosts[i]), (int)(moveCosts[i]), (int)(moveCosts[i]));
        }
    }

    public static boolean tryToPaint(MapLocation loc) throws GameActionException{
        if(isPaintable(loc)){
            // rc.setIndicatorString(loc.x + " " + loc.y);
            rc.setIndicatorDot(loc, 255, 0, 0);
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

    public static void act() throws GameActionException {
        MapLocation prevLoc = rc.getLocation();
        if(rc.canMove(way)){
            rc.move(way);
            MapLocation curLoc = rc.getLocation();
            HomeNav.recordMove(curLoc, prevLoc);
        }
        // if(ruinLoc != null && rc.getLocation().isAdjacentTo(ruinLoc)){
        if(ruinLoc != null){
            rc.setIndicatorDot(ruinLoc, 0, 0, 255);
            if(rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruinLoc)){
                if(FastRand.rand256() < 192){
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
            if(!tryToPaint(rc.getLocation())){
                if(ruinLoc != null){
                    int offset = FastRand.rand256()%nearbyTiles.length;
                    for(int i = nearbyTiles.length; --i >= 0;){
                        if(nearbyTiles[i].getMapLocation().isWithinDistanceSquared(ruinLoc, 8) && tryToPaint(nearbyTiles[i].getMapLocation())){
                            break;
                        }
                    }
                }else{
                    int offset = FastRand.rand256()%nearbyTiles.length;
                    for(int i = nearbyTiles.length; --i >= 0;){
                        if(tryToPaint(nearbyTiles[i].getMapLocation())){
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
        for(MapInfo tile : nearbyTiles){
            MapLocation loc = tile.getMapLocation();
            if(rc.canCompleteResourcePattern(loc)){
                rc.completeResourcePattern(loc);
            }
        }
    }

    @SuppressWarnings("unused")
    public static void run() throws GameActionException {
        if(!initialized) init();
        sense();
        think();
        act();
    }
*/
}