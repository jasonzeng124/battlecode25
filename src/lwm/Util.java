package lwm;

import java.util.Random;

import battlecode.common.*;

public class Util {

    public static final Direction[] dirs = {
        Direction.EAST, Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST,
        Direction.WEST, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST,
        Direction.CENTER
    };

    public static final Random rand = new Random();

    public static boolean getBoost(MapLocation loc) {

        final int x = loc.x;
        final int y = loc.y;

        return ((x + y) % 2 == 0 && (x * 3 + y) % 10 != 0);

    }

    public static int getChoice(int[] probs) {

        int sum = 0;

        for (int prob : probs) {
            sum += prob;
        }

        final int val = rand.nextInt(sum);

        sum = 0;

        for (int i = 0; i < probs.length; ++i) {
            sum += probs[i];
            if (val < sum) {
                return i;
            }
        }

        return -1;

    }

    public static int getIdx(Direction dir) {

        for (int i = 0; i < dirs.length; ++i) {
            if (dirs[i] == dir) {
                return i;
            }
        }

        return -1;

    }

}
