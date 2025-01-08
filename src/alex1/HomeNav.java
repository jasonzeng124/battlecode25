package alex1;

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
        if(map[x.x][x.y] == 0){
            return 1000000;
        }
        return map[x.x][x.y];
    }


    public static void recordMove(MapLocation c, MapLocation p){
        if(map[c.x][c.y] == 0){
            map[c.x][c.y] = map[p.x][p.y] + 1;
        }else{
            if(map[c.x][c.y] > (map[p.x][p.y] + 1)){
                map[c.x][c.y] = map[p.x][p.y] + 1;
            }
        }
    }
}
