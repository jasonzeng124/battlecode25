package fighter_v2f.robot;

import battlecode.common.*;
import fighter_v2f.util.FastRand;
import fighter_v2f.util.FastIntSet;
import fighter_v2f.util.FastIterableLocSet;
import fighter_v2f.util.FastLocSet;

public class Tower extends Robot {
    public Tower(RobotController rc) {
        super(rc);
    }

    int numUsage = 0;

    @Override
    public void function() throws GameActionException {
        for (RobotInfo rob : rc.senseNearbyRobots(rc.getLocation(), -1, oppTeam)) {
            if (rc.canAttack(rob.getLocation())) {
                rc.attack(rob.getLocation());
            }
        }
        rc.attack(null);
        if(rc.getPaint() > 500){
            numUsage = Math.max(numUsage - 1, 0);
        }else{
            numUsage++;
        }
        addIndicatorField("Numusage: " + numUsage);
        if (rc.isActionReady() && (rc.getRoundNum() < 50 || (rc.getChips() >= 1300 && rc.getPaint() >= 500)) && FastRand.next256() <= (1024/rc.getNumberTowers())) {
            // UnitType type = rc.getRoundNum() >= 50 && FastRand.next256() < 32 ? UnitType.MOPPER : UnitType.SOLDIER;
            UnitType type = UnitType.SOLDIER;
            MapLocation bestloc = null;
            int bestscore = -100;

            for (MapInfo tile : rc.senseNearbyMapInfos(rc.getLocation(), 2)) {
                MapLocation loc = tile.getMapLocation();

                if (rc.canBuildRobot(type, loc)) {
                    int score = 0;
                    for (RobotInfo rob : rc.senseNearbyRobots(loc, 1, rc.getTeam())) {
                        if (rob.getType() == type)
                            score -= 1;
                    }
                    MapInfo info = rc.senseMapInfo(loc);
                    if (info.getPaint().isEnemy())
                        score -= 2;
                    if (info.getPaint().isAlly())
                        score -= 1;

                    score += FastRand.next256() % 3;

                    if (score > bestscore) {
                        bestscore = score;
                        bestloc = loc;
                    }
                }
            }
            if (bestloc != null)
                rc.buildRobot(type, bestloc);
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
