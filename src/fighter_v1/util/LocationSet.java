package fighter_v1.util;

import battlecode.common.MapLocation;

public class LocationSet {
    private StringBuilder sb = new StringBuilder();

    public void add(MapLocation location) {
        sb.append((char) (location.y * 60 + location.x));
    }

    public boolean contains(MapLocation location) {
        return sb.indexOf(String.valueOf((char) (location.y * 60 + location.x))) != -1;
    }
}