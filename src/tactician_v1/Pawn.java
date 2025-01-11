package tactician_v1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Pawn {
    public static boolean initialized = false;
    public static final Direction[] directions = {
            Direction.WEST,
            Direction.NORTHWEST,
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.CENTER
    };

    public static RobotController rc;
    public static MapInfo[] nearbyTiles;
    public static RobotInfo[] nearbyRobots;

    public static MapLocation goal;
    public static int priority;
    public static MapLocation prevGoal;
    public static MapLocation[] retry;
    public static int[] retryTime;
    public static RobotInfo refuelStation;
    public static int randDir;

    public static void init(RobotController rc_) throws GameActionException {
        HomeNav.init(rc_);
        rc = rc_;
        goal = null;
        priority = 0;
        prevGoal = null;
        refuelStation = null;
        randDir = 0;
        retry = new MapLocation[16];
        retryTime = new int[16];
        initialized = true;
    }

    public static void sense() throws GameActionException {
        nearbyTiles = rc.senseNearbyMapInfos();
        nearbyRobots = rc.senseNearbyRobots();
        for (int i = 0; i < 16; i++) {
            retryTime[i]--;
            if (retryTime[i] <= 0) {
                retry[i] = null;
            }
        }

        if (rc.isMovementReady()) {
            if (!rc.canMove(directions[randDir])) {
                randDir = FastMath.rand256() % 8;
            }
        }
        goal = null;
        priority = 0;
        if (FastMath.rand256() <= 20) {
            randDir = FastMath.rand256() % 8;
        }
        for (MapInfo mp : nearbyTiles) {
            if (!mp.isWall() && !mp.hasRuin()) {
                HomeNav.updatePassable(mp.getMapLocation());
            }
        }
        for (RobotInfo robot : nearbyRobots) {
            if (robot.team.isPlayer()) {
                if (robot.type.paintPerTurn != 0) {
                    refuelStation = robot;
                    //  System.out.println(rc.senseMapInfo(robot.location).hasRuin());
                    HomeNav.declareSource(robot.location.add(directions[FastMath.rand256() & 7]));
                }
            }
        }
    }

    public static void tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
        if (rc.getID() % 2 == 0) {
            if (rc.canMove(dir.rotateLeft())) {
                rc.move(dir.rotateLeft());
            }
            if (rc.canMove(dir.rotateRight())) {
                rc.move(dir.rotateRight());
            }
        } else {
            if (rc.canMove(dir.rotateRight())) {
                rc.move(dir.rotateRight());
            }
            if (rc.canMove(dir.rotateLeft())) {
                rc.move(dir.rotateLeft());
            }
        }
        for (int i = 8; --i >= 0; ) {
            dir = dir.rotateLeft();
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
    }

    public static void goHome() throws GameActionException {
        int curDist = HomeNav.getDist(rc.getLocation());
        rc.setIndicatorString("dist = " + curDist);
        Direction way = directions[randDir];
        if (refuelStation != null) {
            way = rc.getLocation().directionTo(refuelStation.getLocation());
        }
        for (int i = 9; --i >= 0; ) {
            if (rc.canMove(directions[i])) {
                int newDist = HomeNav.getDist(rc.getLocation().add(directions[i]));
                if (newDist < curDist) {
                    way = directions[i];
                }
            }
        }
        tryMove(way);
    }

    public static PaintType defaultPattern(MapLocation loc) {
        if (((loc.x + loc.y) % 2 == 0) && (((3 * loc.x + loc.y) % 10) != 0)) {
            return PaintType.ALLY_SECONDARY;
        } else {
            return PaintType.ALLY_PRIMARY;
        }
    }

    public static void setGoal(MapLocation m, int p) {
        if (p > priority) {
            goal = m;
            priority = p;
        } else if (p == priority) {
            if (p == 1) {
                if (rc.getLocation().distanceSquaredTo(goal) > rc.getLocation().distanceSquaredTo(m)) {
                    goal = m;
                }
            } else {
                if (prevGoal != null && goal != null && prevGoal.equals(goal)) {
                    return;
                }
                if (FastMath.hash256(60 * m.x + m.y) < FastMath.hash256(60 * goal.x + goal.y)) {
                    goal = m;
                }
            }
        }
    }

    public static boolean tryToPaint(MapLocation loc) throws GameActionException {
        if (rc.canAttack(loc)) {
            MapInfo mp = rc.senseMapInfo(loc);
            if (!mp.hasRuin()) {
                PaintType mk = mp.getMark();
                PaintType cur = mp.getPaint();
                PaintType desired;
                if (mk == PaintType.EMPTY) {
                    desired = defaultPattern(loc);
                } else {
                    desired = mk;
                }
                if (desired != cur) {
                    rc.attack(loc, desired == PaintType.ALLY_SECONDARY);
                    rc.setIndicatorDot(loc, 255, 255, 0);
                    return true;
                }
            }
        }
        return false;
    }

    public static void doJobs() throws GameActionException {
        for (int i = nearbyTiles.length; --i >= 0; ) {
            MapInfo mi = nearbyTiles[i];
            MapLocation ml = mi.getMapLocation();
            if (mi.hasRuin()) {
                // System.out.println("bruh");
                if (rc.canSenseRobotAtLocation(ml)) {
                    RobotInfo rb = rc.senseRobotAtLocation(ml);
                    //if it is not my tower, and we can whack it, why not
                    if (rb.getTeam() != rc.getTeam()) {
                        if (rc.canAttack(ml)) {
                            rc.attack(ml);
                        }
                    }
                } else {
                    //has it been marked?
                    if (rc.senseMapInfo(ml.subtract(rc.getLocation().directionTo(ml))).getMark() == PaintType.EMPTY) {
                        setGoal(ml, 4);
                        break;
                    }
                    //has it been completed?
                    if (rc.getMoney() >= 1000 && rc.getNumberTowers() < 25) {
                        //check if it has been seen in retry
                        boolean seen = false;
                        for (int j = 16; --j >= 0; ) {
                            if (ml.equals(retry[j])) {
                                seen = true;
                            }
                        }
                        if (!seen) {
                            setGoal(ml, 4);
                        }
                    }
                }
            } else if (!mi.isWall()) {
                PaintType m = mi.getMark();
                PaintType p = mi.getPaint();
                switch (p) {
                    case EMPTY:
                    case ALLY_PRIMARY:
                    case ALLY_SECONDARY:
                        //check if the paint is wrong color
                        if (m == PaintType.EMPTY) {
                            if (p != defaultPattern(ml)) {
                                setGoal(ml, 2);
                            }
                        } else {
                            if (p != m) {
                                setGoal(ml, 3);
                            }
                        }
                        break;
                    default:
                        //stay near the front line, possibly assisting moppers
                        setGoal(ml, 1);
                }
            }
        }
        if (goal == null) {
            if (prevGoal == null) {
                //stick to wander strat
                priority = 0;
            } else {
                goal = prevGoal;
                priority = 2;
            }
        }
        Direction[] allowedDirs = new Direction[30];
        int numDirs = 0;
        if (priority == 0) {
            numDirs = 1;
            allowedDirs[0] = directions[randDir];
        } else if (priority == 1) {
            //as long as the goal doesn't leave vision range, it is allowed
            for (int i = 9; --i >= 0; ) {
                if (rc.canMove(directions[i])) {
                    MapLocation ml = rc.getLocation().add(directions[i]);
                    if (ml.distanceSquaredTo(goal) <= 20) {
                        if (rc.senseMapInfo(ml).getPaint().isAlly()) {
                            allowedDirs[numDirs] = directions[i];
                            numDirs++;
                        }
                    }
                }
            }
        } else if (priority >= 2) {
            int[] range = {0, 0, 9, 9, 2};
            for (int i = 8; --i >= 0; ) {
                if (rc.canMove(directions[i])) {
                    MapLocation ml = rc.getLocation().add(directions[i]);
                    //ideally you should get closer to the goal
                    if (ml.distanceSquaredTo(goal) < rc.getLocation().distanceSquaredTo(goal)) {
                        allowedDirs[numDirs] = directions[i];
                        numDirs++;
                        if (rc.senseMapInfo(ml).getPaint().isAlly()) {
                            allowedDirs[numDirs] = directions[i];
                            numDirs++;
                        }
                        if (ml.distanceSquaredTo(goal) <= range[priority]) {
                            allowedDirs[numDirs] = directions[i];
                            numDirs++;
                        }
                    }
                }
            }
        }
        if (numDirs == 0) {
            numDirs = 1;
            allowedDirs[0] = directions[randDir];
        }
        tryMove(allowedDirs[FastMath.rand256() % numDirs]);

        rc.setIndicatorString(priority + "");
        if (goal != null) {
            rc.setIndicatorDot(goal, 255, 0, 255);
        }
        if (prevGoal != null) {
            rc.setIndicatorDot(prevGoal, 0, 255, 255);
        }

        if (prevGoal != null && rc.getLocation().equals(prevGoal)) {
            prevGoal = null;
        } else {
            prevGoal = goal;
        }
        tryToPaint(rc.getLocation());
        //now we do the actual stuff
        if (priority == 4) {
            if (rc.getLocation().isAdjacentTo(goal)) {
                //mark it
                if (rc.senseMapInfo(rc.getLocation()).getMark() == PaintType.EMPTY) {
                    if (FastMath.fakefloat() <= 0.6) {
                        if (rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, goal)) {
                            rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, goal);
                        }
                    } else {
                        if (rc.canMarkTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, goal)) {
                            rc.markTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, goal);
                        }
                    }
                }
                //declare it
                boolean needToRetry = true;
                if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, goal)) {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, goal);
                    needToRetry = false;
                }
                if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, goal)) {
                    rc.completeTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER, goal);
                    needToRetry = false;
                }
                //try again in 100 turns
                if (needToRetry) {
                    for (int i = 16; --i >= 0; ) {
                        if (retry[i] == null) {
                            retry[i] = goal;
                            retryTime[i] = 100;
                            break;
                        }
                        if (i == 0) {
                            retry[15] = goal;
                            retryTime[i] = 100;
                        }
                    }
                }
            }
        } else if (priority == 2 || priority == 3) {
            if (tryToPaint(rc.getLocation())) {
                return;
            }
            if (tryToPaint(goal)) {
                return;
            }
            for (int i = nearbyTiles.length; --i >= 0; ) {
                if (tryToPaint(nearbyTiles[i].getMapLocation())) {
                    return;
                }
            }
        }
    }

    public static void act() throws GameActionException {
        MapLocation prevLoc = rc.getLocation();
        sense();
        if (rc.getPaint() <= 50) {
            goHome();
            if (refuelStation != null) {
                //grab paint from nearby tower
                if (refuelStation.paintAmount + rc.getPaint() >= 155) {
                    if (rc.canTransferPaint(refuelStation.location, Math.max(-refuelStation.paintAmount, rc.getPaint() - 200))) {
                        //transfer the paint
                        rc.transferPaint(refuelStation.location, Math.max(-refuelStation.paintAmount, rc.getPaint() - 200));
                    }
                    //  if(rc.canUpgradeTower(refuelStation.location)){
                    //         rc.upgradeTower(refuelStation.location);
                    //    }

                }
            }
        } else {
            doJobs();
        }

        //declare resource boosts
        for (int i = 9; --i >= 0; ) {
            MapLocation loc = rc.getLocation().add(directions[i]);
            if (((3 * loc.x + loc.y) % 10) == 0) {
                if (rc.canCompleteResourcePattern(loc)) {
                    rc.completeResourcePattern(loc);
                }
            }
        }
        HomeNav.recordMove(rc.getLocation(), prevLoc);
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if (!initialized) {
            init(rc);
        }
        act();
    }
}
