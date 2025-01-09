package lwm;

import battlecode.common.*;

public class RobotPlayer {

    public static void run(RobotController rc) {

        while (true) {
            try {
                switch (rc.getType()) {
                    case UnitType.SOLDIER:
                        Soldier.run(rc);
                        break;
                    case UnitType.MOPPER:
                        Mopper.run(rc);
                        break;
                    case UnitType.SPLASHER:
                        Splasher.run(rc);
                        break;
                    default:
                        Tower.run(rc);
                        break;
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                System.out.println();
                System.out.println(e.getMessage());
                System.out.println();
                e.printStackTrace();
                System.out.println();
            } catch (Exception e) {
                System.out.println("Exception");
                System.out.println();
                System.out.println(e.getMessage());
                System.out.println();
                e.printStackTrace();
                System.out.println();
            } finally {
                Clock.yield();
            }
        }

    }

}
