package fighter_v3a.robot;

import battlecode.common.*;
import fighter_v3a.util.FastRand;
import fighter_v3a.util.FastIntSet;
import fighter_v3a.util.FastIterableLocSet;
import fighter_v3a.util.FastLocSet;

public class Tower extends Robot {
    public Tower(RobotController rc) {
        super(rc);
    }

    int numUsage = 0;

    @Override
    public void function() throws GameActionException {
        rc.attack(null);
        for (RobotInfo rob : rc.senseNearbyRobots(rc.getLocation(), -1, oppTeam)) {
            if (rc.canAttack(rob.getLocation())) {
                rc.attack(rob.getLocation());
            }
        }
        if(rc.getPaint() > 500){
            numUsage = Math.max(numUsage - 1, 0);
        }else{
            numUsage++;
        }
        addIndicatorField("Numusage: " + numUsage);
        if (rc.isActionReady() && (rc.getRoundNum() < 50 || (rc.getChips() >= 1300 && rc.getPaint() >= 500)) && FastRand.next256() <= (1024/rc.getNumberTowers())) {
            for (MapInfo tile : rc.senseNearbyMapInfos(rc.getLocation(), 4)) {
                MapLocation loc = tile.getMapLocation();
//                UnitType type = rc.getRoundNum() >= 50 && FastRand.next256() < 32 ? UnitType.MOPPER : UnitType.SOLDIER;
                UnitType type = UnitType.SOLDIER;

                if (rc.canBuildRobot(type, loc)) {
                    rc.buildRobot(type, loc);
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
