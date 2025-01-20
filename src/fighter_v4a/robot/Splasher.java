package fighter_v4a.robot;

import battlecode.common.*;
import fighter_v4a.util.FastRand;
import fighter_v4a.util.FastIntSet;
import fighter_v4a.util.FastIterableLocSet;
import fighter_v4a.util.FastLocSet;

public class Splasher extends Unit {

    static final Direction[] directions = {
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

    enum SplasherState{
        GOHOME,
        WANDER,
        WORK
    }    

     enum FocusType{
        NONE,
        RANDOM,
        WORK,
        HOME
    }

    SplasherState curState = SplasherState.WANDER;
    MapLocation focus = null;
    MapLocation workLoc = null;
    Direction prvDir = null;
    FocusType focusType = FocusType.NONE;
    int workTimer = 0;
    int wanderTimer = 0;


    public void setNewFocus(MapLocation loc, FocusType foc) throws GameActionException{
        if(focusType.ordinal() < foc.ordinal()){
            focus = loc;
            focusType = foc;
        }
    }

    public void clearFocus() throws GameActionException{
        focus = null;
        focusType = FocusType.NONE;
    }

    public boolean tryAttack() throws GameActionException {
        if (rc.isActionReady()) {
            MapLocation loc = null;
            int scr = 0;
            for (MapInfo tile : rc.senseNearbyMapInfos()) {
                final MapLocation tgt = tile.getMapLocation();
                if (!rc.canAttack(tgt)) {
                    continue;
                }
                int curScr = 0;
                for (MapInfo curTile : rc.senseNearbyMapInfos(tgt, 4)) {
                    if (curTile.isWall() || curTile.hasRuin()) {
                        continue;
                    }
                    switch (curTile.getPaint()) {
                        case ALLY_PRIMARY, ALLY_SECONDARY -> curScr -= 6;
                        case EMPTY -> curScr += 1;
                        case ENEMY_PRIMARY, ENEMY_SECONDARY -> curScr += 3;
                    }
                }
                if (curScr > scr) {
                    loc = tgt;
                    scr = curScr;
                }
            }
            if (loc != null && scr >= 10) {
                prvDir = rc.getLocation().directionTo(loc);
                rc.attack(loc);
                return true;
            }
        }
        return false;
    }

    public void setWorkFocus() throws GameActionException{
        if(workLoc != null){
            int lx = rc.getLocation().x;
            int ly = rc.getLocation().y;
            int wx = workLoc.x;
            int wy = workLoc.y;
            setNewFocus(new MapLocation(Math.min(Math.max((3 * wx - lx)/2, 0), rc.getMapWidth()-1), Math.min(Math.max((3 * wy - ly)/2, 0), rc.getMapHeight()-1)), FocusType.WORK);  
        }else{
            setNewFocus(new MapLocation((int)(FastRand.nextFloat() * rc.getMapWidth()), (int)(FastRand.nextFloat() * rc.getMapHeight())), FocusType.RANDOM);
        }
    }
    
    public void wander() throws GameActionException {
        wanderTimer++;
        if(tryAttack()){
            workLoc = rc.getLocation();
            wanderTimer = 0;
            curState = SplasherState.WORK;
            return;
        }
        if(focus == null){
            setWorkFocus();
        }
        if(rc.isMovementReady()){
            BugNav.move(focus);
        }
        if(rc.getLocation().distanceSquaredTo(focus) <= 2 || wanderTimer >= 75){
            //well this work location is no good (perhaps it is already painted)
            wanderTimer = 0;
            workLoc = null;
            clearFocus();
            setWorkFocus();
        }
        //go home
        if (rc.getPaint() < 75) {
            clearFocus();
            workTimer = 0;
            curState = SplasherState.GOHOME;
        }
    }

    //splasher work micro - try splashing as good as possible
    //turns back into wander micro if haven't splashed for a long time
    public void work() throws GameActionException {
        workTimer++;
        // Try to splash something
        if(tryAttack()){
            workTimer = 0;
            workLoc = rc.getLocation();
        }
        if (rc.isMovementReady()) {
            double[] moveScore = new double[9];
            for (int i = 0; i < 9; i++) {
                moveScore[i] = 0;
            }

            for (MapInfo tile : rc.senseNearbyMapInfos()) {
                final MapLocation loc = tile.getMapLocation();
                final int dir = rc.getLocation().directionTo(loc).ordinal();
                final double dist = rc.getLocation().distanceSquaredTo(loc);

                // Get close to enemy paint, but not onto it
                if (tile.getPaint().isEnemy()) {
                    moveScore[dir] += dist <= 2 ? -20 : +3;
                }

                // Get close to empty paint
                if (tile.getPaint() == PaintType.EMPTY) {
                    moveScore[dir] += dist <= 2 ? -10 : +2;
                }
            }

            // Inertia
            if (prvDir != null) {
                moveScore[prvDir.ordinal()] += 8;
                moveScore[(prvDir.ordinal() + 1) % 8] += 1;
                moveScore[(prvDir.ordinal() + 7) % 8] += 1;
            }

            // TODO: Add probabilistic choice to avoid collisions?
            int bestDir = -1;
            for (int i = 8; --i >= 0; ) {
                if (rc.canMove(directions[i]) && (bestDir == -1 || moveScore[i] > moveScore[bestDir])) {
                    bestDir = i;
                }
            }
            if (bestDir != -1) {
                rc.move(directions[bestDir]);
                prvDir = directions[bestDir];
            }
        }
        if (rc.getPaint() < 75) {
            workTimer = 0;
            curState = SplasherState.GOHOME;
            clearFocus();
        }
        if(workTimer >= 50){
            workTimer = 0;
            curState = SplasherState.WANDER;
            clearFocus();
            setWorkFocus();
        }
    }

    public void goHome() throws GameActionException{
        if(focusType != FocusType.RANDOM && (focus != null && !paintTowerLocs.contains(focus))){
            clearFocus();
        }
        if(paintTowerLocs.size() != 0){
            setNewFocus(getRandomPaintTower(), FocusType.HOME);
        }
        if((paintTowerLocs.size() == 0) && (focus == null || rc.getLocation().distanceSquaredTo(focus) <= 20)){
            clearFocus();
            setNewFocus(new MapLocation((int)(FastRand.nextFloat() * rc.getMapWidth()), (int)(FastRand.nextFloat() * rc.getMapHeight())), FocusType.RANDOM);
        }

        if(rc.getLocation().distanceSquaredTo(focus) > 8){
            //todo: find a way to make bugnav not go onto enemy cells
            BugNav.move(focus);
        }
        //todo: towers control who gets to refuel
        if(rc.canSenseRobotAtLocation(focus)){
            RobotInfo ri = rc.senseRobotAtLocation(focus);
            if(ri.type.paintPerTurn > 0 && ri.team == myTeam && ri.getPaintAmount() + rc.getPaint() >= 280){
                if(rc.canMove(fuzzyDirTo(focus))){
                    rc.move(fuzzyDirTo(focus));
                    if(rc.canTransferPaint(focus, rc.getPaint()-280)){
                        rc.transferPaint(focus, rc.getPaint()-280);
                        clearFocus();
                        setWorkFocus();
                        curState = SplasherState.WANDER;
                    }
                }
            }
        }
    }


    public Splasher(RobotController rc) {
        super(rc);
    }

    @Override
    public void function() throws GameActionException {
        super.function();
        switch(curState){
            case WORK:
                work();
                break;
            case WANDER:
                wander();
                break;
            case GOHOME:
                goHome();
                break;
        }
        addIndicatorField(curState.toString());
        addIndicatorField(focusType.toString());
        if(focus != null){
            addIndicatorField(focus.toString());
        }
        switch(focusType){
            case RANDOM:
                rc.setIndicatorDot(focus, 255, 0, 0);
                break;
            case WORK:
                rc.setIndicatorDot(focus, 0, 255, 0);
                break;
            case HOME:
                rc.setIndicatorDot(focus, 255, 0, 255);
                break;
        }
    }
}
