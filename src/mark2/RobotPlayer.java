package mark2;

import battlecode.common.*;

public class RobotPlayer {
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                switch (rc.getType()) {
                    case SOLDIER:
                        Soldier.run(rc);
                        break;
                    case MOPPER:
                        Mopper.run(rc);
                        break;
                    case SPLASHER:
                        Splasher.run(rc);
                        break;
                    default:
                        Tower.run(rc);
                        break;
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
        }
    }
}
