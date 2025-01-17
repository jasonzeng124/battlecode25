package fighter_v2a.robot;

import battlecode.common.*;
import fighter_v2a.util.FastRand;
import fighter_v2a.util.FastIntSet;
import fighter_v2a.util.FastIterableLocSet;
import fighter_v2a.util.FastLocSet;

import java.util.ArrayList;

public abstract class Unit extends Robot {
    protected ArrayList<MapLocation> paintTowerLocs = new ArrayList<>();

    public Unit(RobotController rc) {
        super(rc);
    }

    @Override
    public void function() throws GameActionException {
        for (RobotInfo rob : rc.senseNearbyRobots(rc.getLocation(), 9, myTeam)) {
            if (rob.type.paintPerTurn > 0) {
                if (!paintTowerLocs.contains(rob.getLocation())) {
                    paintTowerLocs.add(rob.getLocation());
                }
            }
            if (rob.type.isTowerType() && rc.getPaint() < rc.getType().paintCapacity - 10) {
                int amount = Math.min(rob.getPaintAmount(), rc.getType().paintCapacity - rc.getPaint());
                if (rc.canTransferPaint(rob.getLocation(), -amount)) {
                    rc.transferPaint(rob.getLocation(), -amount);
                }
            }
        }
    }

    protected MapLocation getClosestPaintTower() {
        int minDist = Integer.MAX_VALUE;
        MapLocation res = null;
        for (MapLocation loc : paintTowerLocs) {
            final int dist = rc.getLocation().distanceSquaredTo(loc);
            if (dist < minDist) {
                minDist = dist;
                res = loc;
            }
        }
        return res;
    }

    protected Direction fuzzyDir(Direction dir) {
        int idx = dir.ordinal();
        int[] order = {
                idx + 4,
                idx + 3, idx + 5,
                idx + 2, idx + 6,
                idx + 1, idx + 7,
                idx
        };
        for (int i = 8; --i >= 0; ) {
            order[i] %= 8;
            if (rc.canMove(allDirections[order[i]])) {
                return allDirections[order[i]];
            }
        }
        return Direction.CENTER;
    }

    protected Direction fuzzyDirTo(MapLocation loc) {
        return fuzzyDir(rc.getLocation().directionTo(loc));
    }
}
