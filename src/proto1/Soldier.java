package proto1;

import battlecode.common.*;
import java.util.ArrayList;

public class Soldier {
    static boolean init = false;

    static int width, height;                                   // Game board dimensions. Needs to be initialized once
    static byte[][] board;                                      // 0 -> seen, 1-2 -> color, 3-4 -> marking

    final static int REFILL_THRES = 50;                         // Min paint value before we go for a refill
    ArrayList<MapLocation> stationLoc;                          // Position of station
    ArrayList<Double> stationWt;                                // Station bias - being crowded or empty down-ranks

    static int chunkSz, widthCh, heightCh, numChunks, covThres; // Side length of a chunk, related constants
    static ArrayList<Integer>[] chunkAdj;                       // Adjacency list for chunk graph
    static int[] chunkCov;                                      // Chunk coverage

    static MapLocation post;                                    // Where we're assigned to be

    public static boolean chkValid(MapLocation loc) {
        return loc.x >= 0 && loc.y >= 0 && loc.x < width && loc.y < height;
    }

    // TODO: Make precomputed array
    public static PaintType extMarking(byte b) {
        return ((b >> 4) & 1) == 0 ? PaintType.EMPTY : ((b >> 5) & 1) == 0 ? PaintType.ALLY_PRIMARY : PaintType.ALLY_SECONDARY;
    }

    // TODO: Make precomputed array
    public static PaintType extPaint(byte b) {
        if ((b & 14) == 0)
            return PaintType.EMPTY;
        if (((b >> 1) & 1) != 0)
            return ((b >> 2) & 1) == 0 ? PaintType.ALLY_PRIMARY : PaintType.ALLY_SECONDARY;
        else
            return ((b >> 3) & 1) == 0 ? PaintType.ENEMY_PRIMARY : PaintType.ENEMY_SECONDARY;
    }

    // TODO: Make precomputed array
    public static boolean extPassable(byte b) {
        return ((b >> 6) & 1) != 0;
    }

    public static int getChunk(int x, int y) {
        return x / chunkSz * heightCh + y / chunkSz;
    }

//    static int qClearTime = 0;
//    static int[] qClearArray = new int[128];
//
//    static void qClear() {
//        qClearTime++;
//    }
//
//    static boolean qClearGet(int idx) {
//        return qClearArray[idx] == qClearTime;
//    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if (!init) {
            width = rc.getMapWidth();
            height = rc.getMapHeight();
            board = new byte[width][height];

            final int area = width * height;
            if (area < 1000)
                chunkSz = 4;
            else if (area < 3000)
                chunkSz = 5;
            else
                chunkSz = 6;

            covThres = (int) (0.9 * chunkSz * chunkSz);
            widthCh = (width + chunkSz - 1) / chunkSz;
            heightCh = (height + chunkSz - 1) / chunkSz;
            numChunks = widthCh * heightCh;

            assert numChunks < 128;
            chunkAdj = new ArrayList[numChunks];
            chunkCov = new int[numChunks];
            for (int i = 0; i < numChunks; i++) {
                chunkAdj[i] = new ArrayList<>();
            }
            for (int i = 0; i < widthCh; i++) {
                for (int j = 0; j < heightCh; j++) {
                    final int cur = i * heightCh + j;
                    if (i + 1 < widthCh) {
                        chunkAdj[cur].add(cur + heightCh);
                        chunkAdj[cur + heightCh].add(cur);
                    }
                    if (j + 1 < heightCh) {
                        chunkAdj[cur].add(cur + 1);
                        chunkAdj[cur + 1].add(cur);
                    }
                }
            }

            init = true;
        }

        // Scan our surroundings
        for (MapInfo tile : rc.senseNearbyMapInfos()) {
            final MapLocation loc = tile.getMapLocation();
            final int x = loc.x, y = loc.y, idx = getChunk(x, y);

            if (extPaint(board[x][y]).isAlly()) {
                chunkCov[idx]--;
            }

            // Reset
            board[x][y] = 0;
            // Visited
            board[x][y] |= 1;
            // Set color
            final PaintType p = tile.getPaint();
            if (p == PaintType.ALLY_PRIMARY || p == PaintType.ALLY_SECONDARY)
                board[x][y] |= 1 << 1;
            if (p == PaintType.ALLY_SECONDARY)
                board[x][y] |= 1 << 2;
            if (p == PaintType.ENEMY_PRIMARY || p == PaintType.ENEMY_SECONDARY)
                board[x][y] |= 1 << 3;
            // Set mark
            final PaintType m = tile.getMark();
            if (m == PaintType.ALLY_PRIMARY || m == PaintType.ALLY_SECONDARY)
                board[x][y] |= 1 << 4;
            if (m == PaintType.ALLY_SECONDARY)
                board[x][y] |= 1 << 5;
            // Set passability
            if (tile.isPassable())
                board[x][y] |= 1 << 6;

            if (extPaint(board[x][y]).isAlly()) {
                chunkCov[idx]++;
            }
        }

        // Paint nearby
        if (rc.getPaint() >= 25 && rc.isActionReady()) {
            for (MapLocation vec : Precomp.ATTACK_VECS) {
                final MapLocation loc = MyMath.addVec(rc.getLocation(), vec);
                if (!chkValid(loc)) {
                    continue;
                }

                final byte data = board[loc.x][loc.y];
                PaintType color = extPaint(data), mark = extMarking(data);

                if (extPassable(data) && (color == PaintType.EMPTY || (mark != PaintType.EMPTY && color != mark))) {
                    rc.attack(loc, Precomp.DEF_PATTERN[loc.x][loc.y]);
                    rc.setIndicatorDot(loc, 0, 255, 0);
                    break;
                }
            }
        }

        // Explore the unknown

        for (int u = 0; u < numChunks; u++) {
            if (chunkCov[u] < covThres) {
                boolean ok = false;
                for (int v : chunkAdj[u]) {
                    if (chunkCov[v] >= covThres) {
                        ok = true;
                        break;
                    }
                }
                if (ok) {

                }
            }
        }
    }
}