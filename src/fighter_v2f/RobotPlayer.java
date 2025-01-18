package fighter_v2f;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import fighter_v2f.robot.*;

@SuppressWarnings("unused")
public class RobotPlayer {
    public static void run(RobotController rc) {
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

    private static Robot createRobot(RobotController rc) {
        switch (rc.getType()) {
            case SOLDIER:
                return new Soldier(rc);
            case MOPPER:
                return new Mopper(rc);
            case LEVEL_ONE_MONEY_TOWER, LEVEL_ONE_PAINT_TOWER, LEVEL_ONE_DEFENSE_TOWER:
                return new Tower(rc);
            default:
                System.out.println("Unknown robot type '" + rc.getType() + "'");
                return null;
        }
    }
}
