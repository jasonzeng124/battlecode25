package fighter_v3b.robot;

import battlecode.common.*;
import fighter_v3b.util.FastRand;
import fighter_v3b.util.FastIntSet;
import fighter_v3b.util.FastIterableLocSet;
import fighter_v3b.util.FastLocSet;

public class Tower extends Robot {
    public Tower(RobotController rc) throws GameActionException {
        super(rc);
    }

    int numUsage = 0;
    int lastAttack = 0;
    UnitType type = UnitType.SOLDIER;

    @Override
    public void function() throws GameActionException {
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
                }
            }
            if(best != null) {
                lastAttack = 0;
                rc.attack(best.getLocation());
            }
            rc.attack(null);
        }
        lastAttack ++;
        if(rc.getPaint() > 500){
            numUsage = Math.max(numUsage - 1, 0);
        }else{
            numUsage++;
        }
        addIndicatorField("Numusage: " + numUsage);
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
        int threshold;
        switch(rc.getType()){
            case LEVEL_ONE_PAINT_TOWER:
                threshold = Math.max(6000 - (numUsage * 20), 2500);
                break;
            case LEVEL_TWO_PAINT_TOWER:
                threshold = Math.max(12000 - (numUsage * 20), 5000);
                break;
            case LEVEL_ONE_MONEY_TOWER:
                threshold = 5000;
                break;
            case LEVEL_TWO_MONEY_TOWER:
                threshold = 10000;
                break;
            default:
                threshold = 100000;
                break;
        }
        if(rc.getChips() > threshold){
            if(rc.canUpgradeTower(rc.getLocation())){
                rc.upgradeTower(rc.getLocation());
            }
        }
    }
}
