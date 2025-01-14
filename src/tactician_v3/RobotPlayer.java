package tactician_v3;

import battlecode.common.*;

public class RobotPlayer {
    static boolean init = false;

    enum Job {
        // Unemployment arc
            NONE,

        // Home base for units
            BASE,

        // Soldiers:
            RAIDER, // Try to swiftly destroy a tower at the target location(s), and become a settler if we survive.
            SETTLER, // Quickly explore and build towers near the target area without painting unnecessary roads.
            PAWN, // General-use unit, slowly expands and builds outwards.

        // Moppers:
            MOPPER,
            SPLASHER
    };

    static Job myJob = Job.NONE;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        
        FastMath.initRand(rc);
        if (!init) {
            if (rc.getType().isTowerType()) {
                myJob = Job.BASE;
            }
            if (rc.getType() == UnitType.SOLDIER) {
                myJob = rc.getRoundNum() < 5 ? Job.RAIDER : Job.PAWN;
                if (rc.getRoundNum() < 5) {
                    myJob = Job.RAIDER;
//                    myJob = Job.SETTLER;
                } else if (rc.getRoundNum() < rc.getMapWidth() * rc.getMapHeight() / 15) {
                    myJob = Job.SETTLER;
                } else {
                    myJob = Job.PAWN;
                }
            }
            if (rc.getType() == UnitType.MOPPER) {
                myJob = Job.MOPPER;
            }
            if (rc.getType() == UnitType.SPLASHER) {
                myJob = Job.SPLASHER;
            }
        }

        while (true) {
            try {
                switch (myJob) {
                    case NONE -> rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
                    case BASE -> Tower.run(rc);
                    case RAIDER -> Raider.run(rc);
                    case SETTLER -> Settler.run(rc);
                    case PAWN -> Pawn.run(rc);
                    case MOPPER -> Mopper.run(rc);
                    case SPLASHER -> Splasher.run(rc);
                    default -> System.err.println("Unknown unit type");
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
