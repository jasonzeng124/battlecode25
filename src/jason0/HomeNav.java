package jason0;

import battlecode.common.*;


public class HomeNav {

    public static final Direction[] directions = {
        Direction.WEST,
        Direction.NORTHWEST,
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST
    };

    public static int [][] map;
    public static long [] passability;
    public static boolean initialized = false;
    public static int sp;
    public static int ep;
    public static RobotController rc;
    public static MapLocation [] ringbuf;

    public static void init() throws GameActionException {
        map = new int[60][60];
        passability = new long [60];
        sp = 0;
        ep = 0;
        ringbuf = new MapLocation [256];
        initialized = true;
    }

    public static void declareSource(MapLocation x){
        if(map[x.x][x.y] == 0){
        map[x.x][x.y] = 1;
            ringbuf[ep] = x;
            ep = (ep+1)%256;
        }
    }

    //update if a location is passable
    public static void updatePassable(MapLocation x){
        passability[x.x] |= (1L << x.y);
    }

    public static boolean isPassable(MapLocation x){
        return (((passability[x.x]>>x.y)&1) == 1);
    }

    public static int getDist(MapLocation x){
        if(map[x.x][x.y] == 0){
            return 1000000;
        }
        return map[x.x][x.y];
    }

    public static void recordMove(MapLocation c, MapLocation p){
        if(map[p.x][p.y] != 0){
            if(map[c.x][c.y] == 0){
                map[c.x][c.y] = map[p.x][p.y] + 1;
                ringbuf[ep] = new MapLocation(c.x, c.y);
                ep = (ep+1)%256;
            }else{
                if(map[c.x][c.y] > (map[p.x][p.y] + 1)){
                    map[c.x][c.y] = map[p.x][p.y] + 1;
                    ringbuf[ep] = new MapLocation(c.x, c.y);
                    ep = (ep+1)%256;
                }
                if(map[p.x][p.y] > (map[c.x][c.y] + 1)){
                    map[p.x][p.y] = map[c.x][c.y] + 1;
                    ringbuf[ep] = new MapLocation(p.x, p.y);
                    ep = (ep+1)%256;
                }
            }
        }
    }

    public static void wasteBytecode() throws GameActionException {
        if(ringbuf[sp] != null){
            rc.setIndicatorDot(ringbuf[sp], 255, 0, 0);
            //branch
            int x = ringbuf[sp].x;
            int y = ringbuf[sp].y;
            for(int i = 8; --i >= 0;){
                MapLocation nxt = ringbuf[sp].add(directions[i]);
                int nx = nxt.x;
                int ny = nxt.y;
                if(0 <= nx && nx < 60 && 0 <= ny && ny < 60 && isPassable(nxt)){
                    if(map[nx][ny] == 0){
                        map[nx][ny] = map[x][y] + 1;
                        ringbuf[ep] = new MapLocation(nx, ny);
                        ep = (ep+1)%256;
                    }else{
                        if(map[nx][ny] > (map[x][y] + 1)){
                            map[nx][ny] = map[x][y] + 1;
                            ringbuf[ep] = new MapLocation(nx, ny);
                            ep = (ep+1)%256;
                        }
                    }
                }
            }
            sp = (sp+1)%256;
        }
    }
}
