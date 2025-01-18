package tactician_v3_soldiers_only;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

public class Tower {
    public static final Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST,};

    static MapLocation origin;
    static ArrayList<Integer> dispatched = new ArrayList<>();
    static Random rand = new Random();
    static UnitType next = UnitType.SOLDIER;
    static int chipsPerSecond = -1;
    static int prevChips = 1000000000;

    public static void countChips(RobotController rc) throws GameActionException {
        int curChips = rc.getMoney();
        if ((curChips - prevChips) > chipsPerSecond) {
            chipsPerSecond = (curChips - prevChips);
        }
        prevChips = curChips;
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // Initialize stuff
        if (origin == null) {
            if (rc.getRoundNum() == 1) {
                origin = rc.getLocation();
            } else {
                for (Message msg : rc.readMessages(-1)) {
                    final int data = msg.getBytes();
                    origin = new MapLocation(data & 63, (data >> 6) & 63);
                    break;
                }
            }
        }

        countChips(rc);
        rc.attack(null);

        // Build something
        if (rc.isActionReady() && rc.getChips() >= 1000) {
            UnitType type = null;

            final int earlyThres = rc.getMapWidth() * rc.getMapHeight() / 15;
            if (rc.getRoundNum() < earlyThres) {
                type = UnitType.SOLDIER;
            } else if (rc.getType().paintPerTurn > 0 && rc.getPaint() >= 300) {
                final double r = rand.nextDouble();
                if (r < 0.33) {
                    type = UnitType.SOLDIER;
                } else if (r < 0.66) {
                    type = UnitType.MOPPER;
                } else {
                    type = UnitType.SPLASHER;
                }
            }

            if (type != null) {
                for (MapInfo tile : rc.senseNearbyMapInfos(rc.getLocation(), 4)) {
                    final MapLocation loc = tile.getMapLocation();
                    if (rc.canBuildRobot(type, loc)) {
                        rc.buildRobot(type, loc);
                        break;
                    }
                }
            }
        }

        // Share the origin
        if (origin != null) {
            for (RobotInfo r : rc.senseNearbyRobots(rc.getLocation(), 9, rc.getTeam())) {
                if (rc.canSendMessage(r.getLocation())) {
                    rc.sendMessage(r.getLocation(), origin.x | (origin.y << 6));
                }
            }
        }
    }
}
