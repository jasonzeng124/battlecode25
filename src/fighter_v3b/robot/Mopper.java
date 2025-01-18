package fighter_v3b.robot;

import battlecode.common.*;
import fighter_v3b.util.FastRand;
import fighter_v3b.util.FastIntSet;
import fighter_v3b.util.FastIterableLocSet;
import fighter_v3b.util.FastLocSet;

public class Mopper extends Unit {

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

    enum MopperState{
        WANDER,
        GOHOME,
    }    

    enum FocusType{
        NONE,
        RANDOM,//go to random location
        ENEMY,//go to enemy paint
        SAFTEY,//go to safer location (unused currently)
        HOME//go home
    }


    MopperState curState = MopperState.WANDER;

    FastLocSet badRuins = new FastLocSet();
    MapLocation focus = null;
    FocusType focusType = FocusType.NONE;
    UnitType buildType = null;
    int focusTimer = 0;

    public void setNewFocus(MapLocation loc, FocusType foc) throws GameActionException{
       // System.out.println(loc.toString() + " " + foc.toString());
       // System.out.println(focusType.ordinal + " " + foc.ordinal());
        if(focusType.ordinal() < foc.ordinal()){
            focus = loc;
            focusType = foc;
        }
    }

    public void clearFocus() throws GameActionException{
        focus = null;
        focusType = FocusType.NONE;
    }

    public void wander() throws GameActionException{
        if(focusType.ordinal() < (FocusType.ENEMY).ordinal()){
            int closest = 100;
            MapLocation emptyLoc = null;
            for(MapInfo mi : rc.senseNearbyMapInfos()){
                if(mi.getPaint() == PaintType.ENEMY_PRIMARY || mi.getPaint() == PaintType.ENEMY_SECONDARY){
                    if(Math.abs(mi.getMapLocation().distanceSquaredTo(rc.getLocation())-2) <= closest){
                        closest = Math.abs(mi.getMapLocation().distanceSquaredTo(rc.getLocation())-2);
                        emptyLoc = mi.getMapLocation();
                    }
                }
            }
            if(emptyLoc != null){
                setNewFocus(emptyLoc, FocusType.ENEMY);
            }
        }
        if(focus == null){
            setNewFocus(new MapLocation((int)(FastRand.nextFloat() * rc.getMapWidth()), (int)(FastRand.nextFloat() * rc.getMapHeight())), FocusType.RANDOM);
        }

        if(rc.isMovementReady()){
            if(focusType == FocusType.ENEMY){
                //don't move onto the enemy paint
                if(rc.getLocation().distanceSquaredTo(focus) > 2){
                    BugNav.move(focus);
                }
            }else{
                BugNav.move(focus);
            }
        }
        
        if(focusType == FocusType.RANDOM){
            if(rc.getRoundNum()%100 == 0 || rc.getLocation().distanceSquaredTo(focus) <= 10){
                clearFocus();
            }
        }
        //clear focus every 500 rounds
        if(rc.getRoundNum()%500 == 0){
            clearFocus();
        }
        if(focus != null && rc.canSenseLocation(focus)){
            if(focusType == FocusType.ENEMY){
                if(rc.senseMapInfo(focus).getPaint() != PaintType.ENEMY_PRIMARY && rc.senseMapInfo(focus).getPaint() != PaintType.ENEMY_SECONDARY){
                    clearFocus();
                }
            }
        }
        if(rc.isActionReady()){
            //try to attack the focus
            if(focusType == FocusType.ENEMY && rc.canAttack(focus)){
                rc.attack(focus);
            }
        }
        for(int i = 8; --i >= 0;){
            MapLocation ml = rc.getLocation().add(directions[i]);
            if(ml.x%4 == 2 && ml.y%4 == 2 && rc.canCompleteResourcePattern(ml)){
                rc.completeResourcePattern(ml);
            }
        }
        if(rc.getPaint() < 40){
            clearFocus();
            curState = MopperState.GOHOME;
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
            if(ri.type.paintPerTurn > 0 && ri.team == myTeam && ri.getPaintAmount() + rc.getPaint() >= 200){
                if(rc.canMove(fuzzyDirTo(focus))){
                    rc.move(fuzzyDirTo(focus));
                    if(rc.canTransferPaint(focus, rc.getPaint()-100)){
                        rc.transferPaint(focus, rc.getPaint()-100);
                        clearFocus();
                        curState = MopperState.WANDER;
                    }
                }
            }
        }

    }

    public Mopper(RobotController rc) {
        super(rc);
    }

    @Override
    public void function() throws GameActionException {
        super.function();
        switch(curState){
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
            case ENEMY:
                rc.setIndicatorDot(focus, 0, 255, 0);
                break;
            case HOME:
                rc.setIndicatorDot(focus, 255, 0, 255);
                break;
        }
    }
}
