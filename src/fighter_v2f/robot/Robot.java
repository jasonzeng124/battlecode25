package fighter_v2f.robot;

import battlecode.common.*;
import fighter_v2f.util.FastRand;
import fighter_v2f.util.FastIntSet;
import fighter_v2f.util.FastIterableLocSet;
import fighter_v2f.util.FastLocSet;

public abstract class Robot {
    protected final Direction[] allDirections = Direction.values();
    protected final Direction[] adjDirections = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST
    };
    protected final Direction[] diagDirections = {
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST
    };

    protected RobotController rc;

    protected Team myTeam;
    protected Team oppTeam;

    protected MapLocation spawnLoc;

    protected int mapWidth;
    protected int mapHeight;
    protected int mapArea;

    protected int lastTurnCoins = -1;
    protected int thisTurnCoins = -1;
    protected int estimatedIncome = 20;

    protected int turnsActive = 0;
    protected int maxBytecodes = 0;
    protected int sumBytecodes = 0;

    private StringBuilder indicatorString = new StringBuilder();

    public Robot(RobotController rc) {
        this.rc = rc;

        myTeam = rc.getTeam();
        oppTeam = rc.getTeam().opponent();

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        mapArea = mapWidth * mapHeight;

        spawnLoc = rc.getLocation();

        FastRand.seed(rc);
        BugNav.init(rc);
    }

    public void run() throws GameActionException {
        lastTurnCoins = thisTurnCoins;
        thisTurnCoins = rc.getChips();
        if (lastTurnCoins != -1) {
            estimatedIncome = Math.max(estimatedIncome, thisTurnCoins - lastTurnCoins);
        }
        estimatedIncome--;
        

        function();

        turnsActive++;
        maxBytecodes = Math.max(maxBytecodes, Clock.getBytecodeNum());
        sumBytecodes += Clock.getBytecodeNum();
        addIndicatorField("Estimated Income: " + estimatedIncome);
        addIndicatorField("Max bytecodes: " + maxBytecodes);
        addIndicatorField("Average bytecodes: " + sumBytecodes / turnsActive);

        rc.setIndicatorString(indicatorString.toString());
        indicatorString.setLength(0);
    }

    public abstract void function() throws GameActionException;

    protected void addIndicatorField(String s) {
        indicatorString.append(s).append("\n");
    }
}
