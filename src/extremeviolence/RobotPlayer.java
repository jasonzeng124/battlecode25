package extremeviolence;

import battlecode.common.*;

public class RobotPlayer {

    enum Job {
        // Unemployment arc
            NONE,

        // Home base for units
            BASE,

        // Soldiers:
            PAWN, // General-use unit, slowly expands and builds outwards.
            SETTLER, // Quickly explore and build towers near the target area without painting unnecessary roads.
            RAIDER, // Try to swiftly destroy a tower at the target location(s), and become a settler if we survive.

        // Moppers:
            GUARD, // Focused on defending against enemy raider-like units
    };

    static Job myJob = Job.NONE;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if (rc.getType().isTowerType()) {
            myJob = Job.BASE;
        }

        for (Message msg : rc.readMessages(-1)) {
            final int b = (msg.getBytes() >> 24) & 63;
            switch (b) {
                case 0 -> myJob = Job.NONE;
                case 1 -> myJob = Job.PAWN;
                case 2 -> myJob = Job.SETTLER;
                case 3 -> myJob = Job.RAIDER;
                case 4 -> myJob = Job.GUARD;
            }
        }

        while (true) {
            try {
                switch (myJob) {
                    case NONE:
                        rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
                        break;
                    case BASE:
                        Tower.run(rc);
                        break;
                    case PAWN:
                        Pawn.run(rc);
                        break;
                    case SETTLER:
                        Pawn.run(rc);
                        break;
                    case RAIDER:
                        Raider.run(rc);
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
