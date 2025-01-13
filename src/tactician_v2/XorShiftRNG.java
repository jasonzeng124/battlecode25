package tactician_v2;

import battlecode.common.RobotController;

// https://stackoverflow.com/a/13533895
public class XorShiftRNG {
    private long last;
    private long inc;

    public XorShiftRNG(RobotController rc) {
        this.last = rc.getID() | 1;
        inc = rc.getID();
    }

    public int nextInt(int max) {
        last ^= (last << 21);
        last ^= (last >>> 35);
        last ^= (last << 4);
        inc += 123456789123456789L;
        int out = (int) ((last + inc) % max);
        return (out < 0) ? -out : out;
    }

    public float nextFloat() {
        return (float) nextInt(Integer.MAX_VALUE) / (Integer.MAX_VALUE);
    }
}