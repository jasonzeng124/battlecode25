package fighter_v2c.robot;

import battlecode.common.*;
import fighter_v2c.util.FastRand;
import fighter_v2c.util.FastIntSet;
import fighter_v2c.util.FastIterableLocSet;
import fighter_v2c.util.FastLocSet;

public class Soldier extends Unit {
    static final int[] UPG_COST = {1000, 2500, 5000, (int) 1e9};

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

    enum SoldierState{
        WANDER,
        BUILD,
        FIGHT,
        GOHOME
    }    

    //5 things to focus on as a soldier
    enum FocusType{
        NONE,
        RANDOM,
        WRONG,
        EMPTY,
        ENEMY,
        RUIN,
        HOME
    }


    SoldierState curState = SoldierState.WANDER;

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
        boolean [][] pat = rc.getResourcePattern();
        MapLocation [] ruins = rc.senseNearbyRuins(-1);
        FastLocSet emptyRuinsSet = new FastLocSet();
        for(MapLocation ml : ruins){
            if(rc.canSenseRobotAtLocation(ml)){
                RobotInfo r = rc.senseRobotAtLocation(ml);
                if(r.team != myTeam){
                    setNewFocus(ml, FocusType.ENEMY);
                }
            }else{
                if(rc.getNumberTowers() < 25){
                    emptyRuinsSet.add(ml);
                    if(!badRuins.contains(ml)){
                        boolean goodFocus = true;
                        checkGoodFocus: {
                            for(RobotInfo ri : rc.senseNearbyRobots(ml, 8, myTeam)){
                                if(ri.type == UnitType.SOLDIER){
                                    goodFocus = false;
                                    break checkGoodFocus;
                                }
                            }
                            
                            for (int i = 0; i < 5; i++) {
                                for (int j = 0; j < 5; j++) {
                                    MapLocation loc = new MapLocation(ml.x + i - 2, ml.y + j - 2);
                                    if(rc.canSenseLocation(loc)){
                                        MapInfo tile = rc.senseMapInfo(loc);
                                        if (tile.getPaint() == PaintType.ENEMY_PRIMARY || tile.getPaint() == PaintType.ENEMY_SECONDARY) {
                                            goodFocus = false;
                                            break checkGoodFocus;
                                        }
                                    }
                                }
                            }
                        }
                        if(goodFocus){
                            setNewFocus(ml, FocusType.RUIN);
                        }else{
                            badRuins.add(ml);
                            curState = SoldierState.WANDER;
                        }
                    }
                }
            }
        }
        MapLocation [] emptyRuins = emptyRuinsSet.getKeys();
        if(focusType.ordinal() < (FocusType.EMPTY.ordinal())){
            int closest = 100;
            MapLocation emptyLoc = null;
            for(MapInfo mi : rc.senseNearbyMapInfos()){
                if(mi.getPaint() == PaintType.EMPTY && !mi.hasRuin() && !mi.isWall()){
                    if(Math.abs(mi.getMapLocation().distanceSquaredTo(rc.getLocation())-2) <= closest){
                        closest = Math.abs(mi.getMapLocation().distanceSquaredTo(rc.getLocation())-2);
                        emptyLoc = mi.getMapLocation();
                    }
                }
            }
            if(emptyLoc != null){
                setNewFocus(emptyLoc, FocusType.EMPTY);
            }else{
                for(MapInfo mi : rc.senseNearbyMapInfos(8)){
                    if(mi.getPaint().isAlly() && mi.getPaint().isSecondary() != pat[mi.getMapLocation().x%4][mi.getMapLocation().y%4]){
                        if(Math.abs(mi.getMapLocation().distanceSquaredTo(rc.getLocation())-2) <= closest){
                            boolean good = true;
                            for(MapLocation ru : emptyRuins){
                                if(ru.distanceSquaredTo(mi.getMapLocation()) <= 8){
                                    good = false;
                                }
                            }
                            if(good){
                                closest = Math.abs(mi.getMapLocation().distanceSquaredTo(rc.getLocation()) - 2);
                                emptyLoc = mi.getMapLocation();
                            }
                        }
                    }
                }
                if(emptyLoc != null){
                    setNewFocus(emptyLoc, FocusType.WRONG);
                }
            }
        }
        if(focus == null){
            //set the focus to be some random location on the map
            //lol
            setNewFocus(new MapLocation((int)(FastRand.nextFloat() * rc.getMapWidth()), (int)(FastRand.nextFloat() * rc.getMapHeight())), FocusType.RANDOM);
        }
        if(rc.isMovementReady()){
            BugNav.move(focus);
        }
        int distToFocus = rc.getLocation().distanceSquaredTo(focus);
        switch(focusType){
            case RUIN:
                if(distToFocus <= 8){
                    boolean checkCanBuild = true;
                    for(RobotInfo ri : rc.senseNearbyRobots(focus, 8, myTeam)){
                        if(ri.type == UnitType.SOLDIER){
                            checkCanBuild = false;
                            break;
                        }
                    }
                    if(!checkCanBuild){
                        clearFocus();
                    }else{
                        curState = SoldierState.BUILD;
                        return;
                    }
                }
                break;
            case ENEMY:
                if(distToFocus <= 20){
                    curState = SoldierState.FIGHT;
                    return;
                }
                break;
            case RANDOM:
                if(rc.getRoundNum()%100 == 0 || distToFocus <= 10){
                    clearFocus();
                }
        }
        //clear focus every 500 rounds
        if(rc.getRoundNum()%500 == 0){
            clearFocus();
        }
        if(focus != null && rc.canSenseLocation(focus)){
            if(focusType == FocusType.EMPTY){
                if(rc.senseMapInfo(focus).getPaint() != PaintType.EMPTY){
                    clearFocus();
                }
            }
            if(focusType == FocusType.WRONG){
                PaintType desiredType = pat[focus.x%4][focus.y%4] ? PaintType.ALLY_SECONDARY : PaintType.ALLY_PRIMARY;
                if(rc.senseMapInfo(focus).getPaint() == desiredType){
                    clearFocus();
                }
            }
        }
        if(rc.isActionReady()){
            if(rc.senseMapInfo(rc.getLocation()).getPaint() == PaintType.EMPTY){
                if(rc.canAttack(rc.getLocation())){
                    rc.attack(rc.getLocation(), pat[rc.getLocation().x%4][rc.getLocation().y%4]);
                }
            }else{
                //try to attack the focus
                if((focusType == FocusType.EMPTY || focusType == FocusType.WRONG) && rc.canAttack(focus)){
                    rc.attack(focus, pat[focus.x%4][focus.y%4]);
                }
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
            curState = SoldierState.GOHOME;
        }
    }

    public void fight() throws GameActionException{
        if(rc.canAttack(focus)){
            rc.attack(focus);
        }        
        RobotInfo[] ri = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        MapLocation[] towerBuf = new MapLocation[4];//towers that want to attack us
        MapLocation[] mopperBuf = new MapLocation[4];//moppers that want to attack us
        int towerPtr = 0;
        int mopperPtr = 0;
        for (RobotInfo r : ri) {
            if (r.getType().isTowerType()) {
                towerBuf[towerPtr] = r.getLocation();
                towerPtr = (towerPtr + 1) % 4;
            }
            if(r.getType() == UnitType.MOPPER){
                mopperBuf[mopperPtr] = r.getLocation();
                mopperPtr = (mopperPtr + 1)%4;
            }
        }
        int bestScore = -100;
        Direction bestMove = Direction.CENTER;
        for (int i = 8; --i >= 0; ) {
            final MapLocation loc = rc.getLocation().add(adjDirections[i]);
            if (rc.canMove(adjDirections[i])) {
                int score = 0;
                //the kiting experience
                if (rc.getLocation().distanceSquaredTo(focus) > 9){
                    if(loc.distanceSquaredTo(focus) <= 9){
                        score += 30;
                    }
                    if(loc.distanceSquaredTo(focus) < rc.getLocation().distanceSquaredTo(focus)){
                        score += 30;
                    }
                }
                if (rc.senseMapInfo(loc).getPaint().isEnemy())
                    score -= 2;
                if (rc.senseMapInfo(loc).getPaint().isAlly())
                    score += 1;
                for (int j = 4; --j >= 0; ) {
                    if (towerBuf[j] != null && towerBuf[j].distanceSquaredTo(loc) <= 9) {
                        score -= 25;
                    }
                }
                for (int j = 4; --j >= 0; ) {
                    if(mopperBuf[j] != null){
                        if(mopperBuf[j].distanceSquaredTo(loc) <= 2){
                            score -= 3;
                        }else if(mopperBuf[j].distanceSquaredTo(loc) <= 8){
                            score -= 1;
                        }
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = adjDirections[i];
                }
            }
        }
        if(rc.canMove(bestMove)){
            rc.move(bestMove);
        }
        if(rc.canAttack(focus)){
            rc.attack(focus);
        }        
        if(!rc.canSenseRobotAtLocation(focus)){
            clearFocus();
            curState = SoldierState.WANDER;
        }
    }

    public void build() throws GameActionException{
        if(focus != null && rc.canSenseRobotAtLocation(focus)){
            buildType = null;
            clearFocus();
            return;
        }

        if(focus == null){
            for (RobotInfo ri : rc.senseNearbyRobots(rc.getLocation(), -1, myTeam)) {
                if (ri.type.isTowerType() && ri.getPaintAmount() + rc.getPaint() >= 200) {
                    if (rc.canTransferPaint(ri.getLocation(), rc.getPaint()-200)) {
                        rc.transferPaint(ri.getLocation(), rc.getPaint()-200);
                    }
                }
            }
            if(rc.getPaint() >= 150){
                curState = SoldierState.WANDER;
            }
            return;
        }

        //well we can't paint here, so go home and cry i guess
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                MapLocation loc = new MapLocation(focus.x + i - 2, focus.y + j - 2);
                if(rc.canSenseLocation(loc)){
                    MapInfo tile = rc.senseMapInfo(loc);
                    if (tile.getPaint() == PaintType.ENEMY_PRIMARY || tile.getPaint() == PaintType.ENEMY_SECONDARY) {
                        clearFocus();
                        curState = SoldierState.WANDER;
                        return;
                    }
                }
            }
        }

        //move then paint to not waste paint
        if(rc.canMove(fuzzyDirTo(focus))){
            rc.move(fuzzyDirTo(focus));
        }
        if(buildType == null){
            double chanceOfMoney = (rc.getNumberTowers() <= 2) ? (1.0) : (0.5 - 0.001 * rc.getRoundNum());
            int typeId = ((((FastRand.next256() / 256.0) <= (chanceOfMoney))) ? 1 : 0);
            buildType = (typeId == 1 ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER);
        }

        UnitType type = buildType;
        boolean[][] pattern = rc.getTowerPattern(type);
        if(rc.senseMapInfo(rc.getLocation()).getPaint() == PaintType.EMPTY){
            if(rc.canAttack(rc.getLocation())){
                int x = rc.getLocation().x - focus.x + 2;
                int y = rc.getLocation().y - focus.y + 2;
                rc.attack(rc.getLocation(), pattern[x][y]);
            }
        }


        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                MapLocation loc = new MapLocation(focus.x + i - 2, focus.y + j - 2);
                if (rc.canAttack(loc) && !(i == 2 && j == 2)) {
                    MapInfo tile = rc.senseMapInfo(loc);
                    if (tile.getPaint() == PaintType.EMPTY || (tile.getPaint() == PaintType.ALLY_SECONDARY) != pattern[i][j]) {
                        rc.attack(loc, pattern[i][j]);
                        break;
                    }
                }
            }
        }

        //out of paint: guess i'll die
        if(rc.getPaint() < 5){
            rc.disintegrate();
        }
        // Complete the tower
        if (rc.canCompleteTowerPattern(type, focus)) {
            rc.completeTowerPattern(type, focus);
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
                    if(rc.canTransferPaint(focus, rc.getPaint()-200)){
                        rc.transferPaint(focus, rc.getPaint()-200);
                        clearFocus();
                        curState = SoldierState.WANDER;
                    }
                }
            }
        }

    }

    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    public void function() throws GameActionException {
        super.function();
        switch(curState){
            case WANDER:
                wander();
                break;
            case FIGHT:
                fight();
                break;
            case BUILD:
                build();
                break;
            case GOHOME:
                goHome();
                break;
        }
        addIndicatorField(curState.toString());
        addIndicatorField(focusType.toString());
        addIndicatorField(paintTowerLocs.size()+"");
        if(focus != null){
            addIndicatorField(focus.toString());
        }
        switch(focusType){
            case RANDOM:
                rc.setIndicatorDot(focus, 255, 0, 0);
                break;
            case WRONG:
                rc.setIndicatorDot(focus, 255, 128, 0);
                break;
            case EMPTY:
                rc.setIndicatorDot(focus, 255, 255, 0);
                break;
            case ENEMY:
                rc.setIndicatorDot(focus, 0, 255, 0);
                break;
            case RUIN:
                rc.setIndicatorDot(focus, 0, 0, 255);
                break;
            case HOME:
                rc.setIndicatorDot(focus, 255, 0, 255);
                break;
        }
    }
}
