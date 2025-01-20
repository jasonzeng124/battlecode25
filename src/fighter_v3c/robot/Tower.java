package fighter_v3c.robot;

import battlecode.common.*;
import fighter_v3c.util.FastRand;
import fighter_v3c.util.FastIntSet;
import fighter_v3c.util.FastIterableLocSet;
import fighter_v3c.util.FastLocSet;

public class Tower extends Robot {
    public Tower(RobotController rc) throws GameActionException {
        super(rc);
        atkbuf = new int[20];
        usagebuf = new int[20];
    }

    int numUsage = 0;
    int lastAttack = 0;
    int atkfreq = 0;
    int[] atkbuf;
    int[] usagebuf;
    UnitType type = UnitType.SOLDIER;

    @Override
    public void function() throws GameActionException {
        atkfreq -= atkbuf[rc.getRoundNum() % 20];
        {
            RobotInfo best = null;
            int bestscore = -1000;
            for (RobotInfo rob : rc.senseNearbyRobots(-1, oppTeam)) {
                if (rc.canAttack(rob.getLocation())) {
                    int score = 0;
                    if(rob.getHealth() <= rc.getType().aoeAttackStrength)
                        score -= 200;
                    if(rob.getType() == UnitType.SOLDIER) score += 300;
                    if(rob.getType() == UnitType.SPLASHER) score += 100;
                    score += rob.getPaintAmount();
                    score += rob.getID() % 10;

                    if (score > bestscore) {
                        bestscore = score;
                        best = rob;
                    }
                    atkbuf[rc.getRoundNum() % 20]++;
                }
            }
            if(best != null) {
                lastAttack = 0;
                rc.attack(best.getLocation());
            }
            rc.attack(null);
        }
        atkfreq += atkbuf[rc.getRoundNum() % 20];
        lastAttack ++;
        numUsage -= usagebuf[rc.getRoundNum() % 20];
        if(rc.getPaint() > 500){
            usagebuf[rc.getRoundNum() % 20] = 0;
        }else{
            usagebuf[rc.getRoundNum() % 20] = (600-rc.getPaint())/100;
        }
        numUsage += usagebuf[rc.getRoundNum() % 20];
        addIndicatorField("Numusage: " + numUsage);
        addIndicatorField("Atkfreq: " + atkfreq);
        if (rc.isActionReady() && (rc.getRoundNum() < 50 || (rc.getChips() >= 1300 && (rc.getPaint() >= 500 || rc.getType().paintPerTurn == 0))) && FastRand.next256() <= (1024/rc.getNumberTowers())) {
            for (MapInfo tile : rc.senseNearbyMapInfos(rc.getLocation(), 4)) {
                MapLocation loc = tile.getMapLocation();
//                UnitType type = rc.getRoundNum() >= 50 && FastRand.next256() < 32 ? UnitType.MOPPER : UnitType.SOLDIER;
                if (rc.canBuildRobot(type, loc)) {
                    rc.buildRobot(type, loc);
                    if(rc.getRoundNum() < 50){
                        type = UnitType.SOLDIER;
                    }else{
                        double x = FastRand.nextFloat();
                        if(x < 0.5 || (rc.getType().paintPerTurn == 0 && rc.getPaint() < 200)){
                            type = UnitType.MOPPER;
                        }else{
                            type = UnitType.SOLDIER;
                        }
                    }
                    return;
                }
            }
        }
        int est = rc.getChips() + estimatedIncome * (2000-rc.getRoundNum());
        addIndicatorField("Est: " + est);
        est -= rc.getType().moneyPerTurn;
        int need = (2000-rc.getRoundNum()) * 800;
        addIndicatorField("Need: " + need);
        int threshold;
        switch(rc.getType()){
            case LEVEL_ONE_PAINT_TOWER:
                threshold = Math.max(4000 - numUsage * 80, 2500);
                break;
            case LEVEL_TWO_PAINT_TOWER:
                threshold = Math.max(8000 - numUsage * 80, 5000);
                break;
            case LEVEL_ONE_MONEY_TOWER:
                threshold = 3000;
                break;
            case LEVEL_TWO_MONEY_TOWER:
                threshold = 5000;
                break;
            case LEVEL_ONE_DEFENSE_TOWER:
                threshold = Math.max(5000 - atkfreq * 80, 2500);
                break;
            case LEVEL_TWO_DEFENSE_TOWER:
                threshold = Math.max(9000 - atkfreq * 80, 5000);
                break;
            default:
                threshold = 10000;
                break;
        }

        if(rc.getType().getBaseType() == UnitType.LEVEL_ONE_MONEY_TOWER) {
            if(rc.getRoundNum() > 400 && rc.getNumberTowers() > 4 && est > need && rc.getChips() > 5000) {
                convert(UnitType.LEVEL_ONE_PAINT_TOWER);
            }
        }
        if(rc.getHealth() < 150) {
            convert(UnitType.LEVEL_ONE_DEFENSE_TOWER);
        }
        if(rc.getType().getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER && lastAttack > 100) {
            convert(UnitType.LEVEL_ONE_PAINT_TOWER);
        }

        if(rc.getChips() > threshold){
            if(rc.canUpgradeTower(rc.getLocation())){
                rc.upgradeTower(rc.getLocation());
            }
        }
    }

    public void convert(UnitType type) throws GameActionException {
        boolean ok = false;
        for(RobotInfo rob : rc.senseNearbyRobots(-1, myTeam)) {
            int delta = Math.min(rc.getPaint(), rob.getType().paintCapacity - rob.getPaintAmount());
            if(rc.canTransferPaint(rob.getLocation(), delta)) {
                rc.transferPaint(rob.getLocation(), delta);
            }
            if(rob.getType() == UnitType.SOLDIER && rc.canSendMessage(rob.location)) {
                rc.sendMessage(rob.location,
                    (1<<31) | (Soldier.FocusType.RUIN.ordinal() << 24)
                    | (rc.getLocation().x << 16)
                    | (rc.getLocation().y << 8)
                    | type.ordinal());
                ok = true;
            }
        }
        if(type == UnitType.LEVEL_ONE_DEFENSE_TOWER)
            rc.setTimelineMarker("self-destruct", 255, 0, 0);
        if(type == UnitType.LEVEL_ONE_PAINT_TOWER)
            rc.setTimelineMarker("self-destruct", 0, 0, 255);
        if(!ok) return;
        rc.disintegrate();
    }

    // public void markPattern(UnitType unitType) throws GameActionException {
    //     boolean[][] pattern = rc.getTowerPattern(unitType);
    //     for(int i=0; i<5; i++) {
    //         for(int j=0; j<5; j++) {
    //             int x = rc.getLocation().x + i - 2;
    //             int y = rc.getLocation().y + j - 2;
    //             PaintType pt = rc.senseMapInfo(new MapLocation(x, y)).getPaint();
    //             if(!pattern[i][j] && pt != PaintType.ALLY_PRIMARY)
    //                 rc.mark(new MapLocation(x, y), pattern[i][j]);
    //             if(pattern[i][j] && pt != PaintType.ALLY_SECONDARY)
    //                 rc.mark(new MapLocation(x, y), pattern[i][j]);
    //         }
    //     }
    // }

    // public void unMark() throws GameActionException {
    //     for(int i=0; i<5; i++) {
    //         for(int j=0; j<5; j++) {
    //             int x = rc.getLocation().x + i - 2;
    //             int y = rc.getLocation().y + j - 2;
    //             if(rc.canRemoveMark(new MapLocation(x, y)))
    //                 rc.removeMark(new MapLocation(x, y));
    //         }
    //     }
    // }
}
