package botv3;

import battlecode.common.*;

public class Mopper {
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
        Direction.CENTER
    };

    
    public static RobotController rc;
    public static MapInfo[] nearbyTiles;
    public static RobotInfo[] nearbyRobots;

    public static RobotInfo refuelStation;
    public static int randDir;

    public static void init(RobotController rc_) throws GameActionException {
        HomeNav.init(rc_);
        rc = rc_;
        refuelStation = null;
        randDir = 0;
        initialized = true;
    }

    public static void sense() throws GameActionException {
        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();
        if(FastMath.rand256() <= 20){
            randDir = FastMath.rand256()%8;
        }
        
        if (rc.isMovementReady()) {
            if(!rc.canMove(directions[randDir])){
                randDir = FastMath.rand256()%8;
            }
        }
        for(MapInfo mp : nearbyTiles){
            if(!mp.isWall() && !mp.hasRuin()){
                HomeNav.updatePassable(mp.getMapLocation());
            }
        }
        for (RobotInfo robot : nearbyRobots){
            if(robot.team.isPlayer()){
                if(robot.type.paintPerTurn != 0){
                    refuelStation = robot;
                    HomeNav.declareSource(robot.location.add(directions[FastMath.rand256()&7]));
                }
            }
        }
    }

    public static void tryMove(Direction dir) throws GameActionException {
        if(rc.canMove(dir)){
            rc.move(dir);
        }
        if(rc.getID()%2 == 0){
            if(rc.canMove(dir.rotateLeft())){
                rc.move(dir.rotateLeft());
            }
            if(rc.canMove(dir.rotateRight())){
                rc.move(dir.rotateRight());
            }
        }else{
            if(rc.canMove(dir.rotateRight())){
                rc.move(dir.rotateRight());
            }
            if(rc.canMove(dir.rotateLeft())){
                rc.move(dir.rotateLeft());
            }
        }
        for(int i = 8; --i >= 0;){
            dir = dir.rotateLeft();
            if(rc.canMove(dir)){
                rc.move(dir);
            }
        }
    }

    //out of paint, no choice but to go home
    public static void goHome() throws GameActionException {
        int curDist = HomeNav.getDist(rc.getLocation());
        rc.setIndicatorString("dist = " + curDist);
        Direction way = directions[randDir];
        if(refuelStation != null){
            way = rc.getLocation().directionTo(refuelStation.getLocation());
        }
        for(int i = 9; --i >= 0;){
            if(rc.canMove(directions[i])){
                int newDist = HomeNav.getDist(rc.getLocation().add(directions[i]));
                if(newDist < curDist){
                    way = directions[i];
                }
            }
        }
        tryMove(way);
    }


    public static void checkAttackAround() throws GameActionException{
        MapInfo [] attackLocs = rc.senseNearbyMapInfos(2);
        for (MapInfo tile : attackLocs) {
            MapLocation target = tile.getMapLocation();
            if (rc.canSenseRobotAtLocation(target)){
                if(rc.senseRobotAtLocation(target).getTeam() != rc.getTeam() && rc.senseRobotAtLocation(target).getType().isRobotType()) {
                    if(rc.canAttack(target)) {
                        rc.attack(target);
                    }
                }
            }
        }
        for (MapInfo tile : attackLocs) {
            MapLocation target = tile.getMapLocation();
            if(rc.canAttack(target)) {
                PaintType p = tile.getPaint();
                switch(p){
                    case ENEMY_PRIMARY: case ENEMY_SECONDARY: 
                        rc.attack(target);  
                        rc.setIndicatorString(target.x + " " + target.y);
                        rc.setIndicatorDot(target, 255, 0, 255); 
                    break;
                }
            }
        }
    }

    public static void doJobs() throws GameActionException{

        // Mopping doesn't cost anything
        if (rc.isActionReady()) {
            checkAttackAround();
        }

        if (rc.isMovementReady()) {
            double[] moveScore = new double[9];
            for (int i = 9; --i >= 0;) {
                moveScore[i] = 0;
            }

            MapLocation closestEnemy = null;
            int closestDist = 100;
            for (MapInfo tile : nearbyTiles) {
                final MapLocation loc = tile.getMapLocation();
                final PaintType p = tile.getPaint();
                final int dir = rc.getLocation().directionTo(loc).ordinal();
                final int dist = rc.getLocation().distanceSquaredTo(loc);
                // Get close to enemy paint, but not onto it
                switch(p){
                    case ENEMY_PRIMARY: case ENEMY_SECONDARY:
                        if(dir == 8){
                            moveScore[dir] -= 100.0;
                        }else{
                            moveScore[dir] += ((dist <= 2) ? -40.0 : ((tile.getMark() == PaintType.EMPTY) ? 0.5 : 1.5));
                        }
                        if(dist < closestDist){
                            closestDist = dist;
                            closestEnemy = loc;
                        }
                        break;
                }
                
                if (rc.canSenseRobotAtLocation(loc) && (p.isAlly() || p == PaintType.EMPTY)) {
                    final RobotInfo r = rc.senseRobotAtLocation(loc);
                    if (r.getTeam() == rc.getTeam()) {
                        switch (r.getType()) {
                            //defend against rushes
                            case UnitType.SOLDIER -> moveScore[dir] += 10.0;
                        }
                    }
                }
            }

            if(closestEnemy != null){
                for(int i = 9; --i >= 0;) {
                    if(closestEnemy.distanceSquaredTo(rc.getLocation().add(directions[i])) <= 2){
                        moveScore[i] += 5.0;
                    }else if(closestEnemy.distanceSquaredTo(rc.getLocation().add(directions[i])) > closestDist){
                        moveScore[i] -= 5.0;
                    }
                    moveScore[i] += 2.0 * FastMath.fakefloat();
                }
            }
            for(int i = 9; --i >= 0;) {
                moveScore[i] += 5.0 * FastMath.fakefloat();
            }
            int bestDir = -1;
            for (int i = 9; --i >= 0;) {
                if (rc.canMove(directions[i]) && (bestDir == -1 || moveScore[i] > moveScore[bestDir])) {
                    bestDir = i;
                }
            }
            if (bestDir != -1) {
                tryMove(directions[bestDir]);
            }else{
                tryMove(directions[randDir]);
            }
        }

        if (rc.isActionReady()) {
            checkAttackAround();
        }
    }

    public static void act() throws GameActionException {
        MapLocation prevLoc = rc.getLocation();
        sense();
        if(rc.getPaint() <= 40){
            goHome();
            if(refuelStation != null){
                //grab paint from nearby tower
                if(refuelStation.paintAmount + rc.getPaint() >= 75){
                    if(rc.canTransferPaint(refuelStation.location, Math.max(-refuelStation.paintAmount, rc.getPaint() - 75))){
                        //transfer the paint
                        rc.transferPaint(refuelStation.location, Math.max(-refuelStation.paintAmount, rc.getPaint() - 75));
                    }
                    if(rc.canUpgradeTower(refuelStation.location)){
                        rc.upgradeTower(refuelStation.location);
                    }
                    
                }
            }
        }else{
            doJobs();
        }
        //declare resource boosts
        for(int i = 9; --i >= 0;){
            MapLocation loc = rc.getLocation().add(directions[i]);
            if(((3*loc.x+loc.y)%10) == 0){
                if(rc.canCompleteResourcePattern(loc)){
                    rc.completeResourcePattern(loc);
                }
            }
        }
        HomeNav.recordMove(rc.getLocation(), prevLoc);
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if(!initialized){
            init(rc);
        }
        int x = rc.getRoundNum();
        act();
        int xp1 = rc.getRoundNum();
        int peak = 0;
        while(Clock.getBytecodeNum() <= 14000){
            int start = Clock.getBytecodeNum();
            HomeNav.wasteBytecode();
            int end = Clock.getBytecodeNum();
            peak = Math.max(peak, end-start);
          //  System.out.println(end - start);
        }
    }
}