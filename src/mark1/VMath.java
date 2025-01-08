package mark1;

import battlecode.common.*;

public class VMath {
    public static MapLocation addVec(MapLocation a, MapLocation b) {
        return new MapLocation(a.x + b.x, a.y + b.y);
    }

    public static MapLocation subVec(MapLocation a, MapLocation b) {
        return new MapLocation(a.x - b.x, a.y - b.y);
    }

    public static MapLocation scaleVec(double f, MapLocation a) {
        return new MapLocation((int) java.lang.Math.round(f * a.x), (int) java.lang.Math.round(f * a.y));
    }

    public static MapLocation negateVec(MapLocation a) {
        return new MapLocation(-a.x, -a.y);
    }

    public static int dotVec(MapLocation a, MapLocation b) {
        return a.x * b.x + a.y * b.y;
    }

    public static int dotVec(Direction d, MapLocation a) {
        if (d == null) {
            return 0;
        }
        return d.dx * a.x + d.dy * a.y;
    }

    public static int dotVec(Direction a, Direction b) {
        return a.dx * b.dx + a.dy * b.dy;
    }

    public static Direction dirFromVec(MapLocation a) {
        MapLocation origin = new MapLocation(0, 0);
        return origin.directionTo(a);
    }
}