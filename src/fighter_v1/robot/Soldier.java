package fighter_v1.robot;

import battlecode.common.*;
import fighter_v1.util.FastRand;
import fighter_v1.util.LocationSet;

public class Soldier extends Unit {
    static final int[] UPG_COST = {1000, 2500, 5000, (int) 1e9};

    Direction curDir;

    LocationSet badRuins = new LocationSet();
    MapLocation focusedRuin;

    public Soldier(RobotController rc) {
        super(rc);

        curDir = allDirections[FastRand.next256() % 8];
    }

    @Override
    public void function() throws GameActionException {
        super.function();

        // Prioritize working on ruins
        if (focusedRuin == null && rc.getNumberTowers() < 25) {
            for (MapLocation loc : rc.senseNearbyRuins(-1)) {
                if (!rc.canSenseRobotAtLocation(loc) && !badRuins.contains(loc)) {
                    focusedRuin = loc;
                    break;
                }
            }
        }
        if (focusedRuin != null) {
            // Get close and orbit
            if (rc.isMovementReady()) {
                rc.move(fuzzyDirTo(focusedRuin));
            }

            // Yield to higher IDs, we don't need more than one worker here at once
            for (RobotInfo rob : rc.senseNearbyRobots(focusedRuin, 2, myTeam)) {
                if (rob.getID() > rc.getID()) {
                    badRuins.add(focusedRuin);
                    focusedRuin = null;
                    break;
                }
            }

            // Okay, work on the ruin
            if (focusedRuin != null) {
                // Paint the build pattern
                UnitType type = (focusedRuin.x + focusedRuin.y) % 3 == 0 ? UnitType.LEVEL_ONE_PAINT_TOWER : UnitType.LEVEL_ONE_MONEY_TOWER;
//                if (Math.sqrt(focusedRuin.distanceSquaredTo(spawnLoc)) >= rc.getMapWidth() / 2) {
//                    type = UnitType.LEVEL_ONE_DEFENSE_TOWER;
//                }

                boolean[][] pattern = rc.getTowerPattern(type);
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 5; j++) {
                        MapLocation loc = new MapLocation(focusedRuin.x + i - 2, focusedRuin.y + j - 2);
                        if (rc.canAttack(loc) && !(i == 2 && j == 2)) {
                            MapInfo tile = rc.senseMapInfo(loc);
                            if (tile.getPaint() == PaintType.EMPTY || (tile.getPaint() == PaintType.ALLY_SECONDARY) != pattern[i][j]) {
                                rc.attack(loc, pattern[i][j]);
                                break;
                            }
                        }
                    }
                }
                // Complete the tower
                if (rc.canCompleteTowerPattern(type, focusedRuin)) {
                    rc.completeTowerPattern(type, focusedRuin);
                    focusedRuin = null;
                }
            }
        }

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
            for (MapInfo tile : rc.senseNearbyMapInfos()) {
                if (rc.canUpgradeTower(tile.getMapLocation()) && rc.getChips() >= 2500) {
                    rc.upgradeTower(tile.getMapLocation());
                }
                if (rc.canAttack(tile.getMapLocation()) && tile.getPaint() == PaintType.EMPTY) {
                    rc.attack(tile.getMapLocation());
                }
            }
        }

        // Chance to clear this stuff
        if (FastRand.next256() < 16) {
            badRuins = new LocationSet();
        }
    }
}
