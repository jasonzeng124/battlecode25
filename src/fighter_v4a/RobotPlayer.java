package fighter_v4a;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import fighter_v4a.robot.*;

@SuppressWarnings("unused")
public class RobotPlayer {
    public static void run(RobotController rc)  throws GameActionException {
        Robot robot = createRobot(rc);

        if (robot == null) {
            return;
        }

        while (true) {
            try {
                robot.run();
            } catch (Exception e) {
                System.out.println("Exception in robot #" + rc.getID() + " (" + rc.getType() + ")");
                e.printStackTrace();
            }
            Clock.yield();
        }
    }

    private static Robot createRobot(RobotController rc) throws GameActionException {
        switch (rc.getType()) {
            case SOLDIER:
                return new Soldier(rc);
            case MOPPER:
                return new Mopper(rc);
            case LEVEL_ONE_MONEY_TOWER, LEVEL_ONE_PAINT_TOWER, LEVEL_ONE_DEFENSE_TOWER:
                return new Tower(rc);
            case SPLASHER:
                return new Splasher(rc);
            default:
                System.out.println("Unknown robot type '" + rc.getType() + "'");
                return null;
        }
    }
}
