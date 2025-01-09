package lwm;

import java.util.Random;

import battlecode.common.*;

public class Util {

    public static final Direction[] dirs = {
        Direction.EAST, Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST,
        Direction.WEST, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST
    };

    public static final Random rand = new Random();

    public static boolean getBoost(MapLocation loc) {

        int x = loc.x;
        int y = loc.y;

        x %= 5;
        y %= 5;

        return ((x + y) % 2 == 0 && (x != 2 || y != 2));

    }

}
