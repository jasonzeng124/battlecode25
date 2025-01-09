package jason0;

import battlecode.common.*;


public class HomeNav {
    public static int [][] map;
    public static boolean initialized = false;

    public static void init(){
        map = new int[60][60];
        initialized = true;
    }

    public static void declareSource(MapLocation x){
        map[x.x][x.y] = 1;
    }

    public static int getDist(MapLocation x){
        if(map[x.x][x.y] == 0) return 1000000;
        return map[x.x][x.y];
    }

    public static Direction getDirection(MapLocation loc) {
        int x = loc.x;
        int y = loc.y;
        if(map[x][y] == 0) return null;
        int min = map[x][y];
        Direction best = Direction.CENTER;
        if(map[x+1][y] < min){
            min = map[x+1][y];
            best = Direction.EAST;
        }
        if(map[x-1][y] < min){
            min = map[x-1][y];
            best = Direction.WEST;
        }
        if(map[x][y+1] < min){
            min = map[x][y+1];
            best = Direction.NORTH;
        }
        if(map[x][y-1] < min){
            min = map[x][y-1];
            best = Direction.SOUTH;
        }
        if(map[x+1][y+1] < min){
            min = map[x+1][y+1];
            best = Direction.NORTHEAST;
        }
        if(map[x-1][y-1] < min){
            min = map[x-1][y-1];
            best = Direction.SOUTHWEST;
        }
        if(map[x+1][y-1] < min){
            min = map[x+1][y-1];
            best = Direction.SOUTHEAST;
        }
        if(map[x-1][y+1] < min){
            min = map[x-1][y+1];
            best = Direction.NORTHWEST;
        }
        
        return best;
    }

    public static void recordMove(MapLocation c, MapLocation p){
        if(map[c.x][c.y] == 0) {
            map[c.x][c.y] = map[p.x][p.y] + 1;
        }
        else{
            if(map[c.x][c.y] > map[p.x][p.y] + 1) {
                map[c.x][c.y] = map[p.x][p.y] + 1;
            }
            // if(map[p.x][p.y] > map[c.x][c.y] + 1) {
                // map[p.x][p.y] = map[c.x][c.y] + 1;
            // }
        }
    }
}
