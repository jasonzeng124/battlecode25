package fighter_v2f.robot;

import battlecode.common.*;
import fighter_v2f.util.FastRand;
import fighter_v2f.util.FastIntSet;
import fighter_v2f.util.FastIterableLocSet;
import fighter_v2f.util.FastLocSet;

public class Mopper extends Unit {
    Direction curDir;

    public Mopper(RobotController rc) {
        super(rc);

        curDir = allDirections[FastRand.next256() % 8];
    }

    @Override
    public void function() throws GameActionException {
        super.function();

        if (rc.isMovementReady()) {
            if (!rc.canMove(curDir)) {
                if (!rc.canSenseLocation(rc.getLocation().add(curDir))) {
                    curDir = allDirections[(curDir.ordinal() + 11 + FastRand.next256() % 3) % 8];
                }
                curDir = fuzzyDir(curDir);
            }
            if (rc.isActionReady()) {
                MapLocation nxt = rc.getLocation().add(curDir);
                if (rc.canAttack(nxt) && rc.senseMapInfo(nxt).getPaint() == PaintType.EMPTY) {
                    rc.attack(nxt);
                }
            }
            rc.move(curDir);
        }

        // Filler actions
        if (rc.isActionReady()) {
            for (int i = 0; i < 8; i++) {
                MapLocation loc = rc.getLocation().add(allDirections[i]);
                if (rc.canAttack(loc)) {
                    MapInfo tile = rc.senseMapInfo(loc);
                    if (tile.getPaint().isEnemy() || (rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).team != myTeam)) {
                        rc.attack(loc);
                        break;
                    }
                }
            }
        }
    }
}
