package jason0;

import battlecode.common.*;

public class RobotPlayer {
    static int round = 0;
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        FastRand.initRand(rc);

        HomeNav.rc = rc;
        HomeNav.init();
        HomeNav.declareSource(rc.getLocation());

        Soldier.rc = rc;
        Mopper.rc = rc;
        Splasher.rc = rc;
        Tower.rc = rc;

        while (true) {
            round = rc.getRoundNum();
            if(round > 1000) rc.resign();
            try {
                switch (rc.getType()){
                    // case SOLDIER: Soldier.run(); break; 
                    case MOPPER: Mopper.run(); break;
                    case SPLASHER: Splasher.run(); break; 
                    default: Tower.run(); break;
                }
                while(Clock.getBytecodeNum() <= 14000){
                    HomeNav.wasteBytecode();
                }
            }
            catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
                if(rc.getRoundNum() > round) System.out.println("Bytecode Limit Exceeded!");
                rc.resign();
            }
            catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
                if(rc.getRoundNum() > round) System.out.println("Bytecode Limit Exceeded!");
                rc.resign();
            }
            finally {
                if(rc.getRoundNum() > round) System.out.println("Bytecode Limit Exceeded!");
                Clock.yield();
            }
        }
    }
}
