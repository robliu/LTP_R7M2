package com.get.gsappalpha1.ImgProc;

import android.util.Log;

/**
 * Created by Rob on 7/17/2017.
 */

public class TCircle {

    public int     trcID;
    public int     centerX;
    public int     centerY;
    public double  aspectRat;
    public int     diameter;
    public int     type;
    public int[]   prof;   // 8-direction transition profile
    public int     concentric;

    private static   final String TAG   = "TCircle";

     public TCircle() {
         trcID   = -1;
         centerX = -1;
         centerY = -1;
         aspectRat  = -1.0;
         diameter   = -1;
         type       = -1;
         prof       = new int[8];
         concentric = -1;
     }

    public TCircle(int mark, int xCen, int yCen, double aspRatio, int diam, int mtype, int[] trans, int mconcent) {

         trcID      = mark;
         centerX    = xCen;
         centerY    = yCen;
         aspectRat  = aspRatio;
         diameter   = diam;
         type       = mtype;
         prof       = new int[8];
         if(trans.length == 8) { for(int i = 0; i < 8; i++) prof[i] = trans[i]; }
         else                  { for(int i = 0; i < 8; i++) prof[i] = 0; }
         concentric = mconcent;
    }

    public TCircle getCircleProperty(tTrace trace, int id, int mark, int[][] bMap, int dimx, int dimy, int[][]gMap) {
        int xCen = -1, yCen = -1, diam = -1;
        double aspRatio = 0.0;
//        int[][] pntAry = trace.tAry;
        int aLen = trace.tAry.length;
        int x0 = 99999, x1 = 0, y0 = 99999, y1 = 0;
        int x, y, dimym2 = dimy - 2;
        int i, j;
        int gap = trace.gap; // from the quick formula
        int openWidth = 9999;      // from point-by-point tracing
        int type = 0;         // nutural; unknown. Value: (-1, 0, 1) - (darker when expanding, unknown, brighter)
        int[] trans = {0, 0, 0, 0, 0, 0, 0, 0}; // 8-direction transition profiles

        if (aLen > 20 && gap < 6) {
            // go through the entire path and calculate the center and the diameter
//                console.log(" getCircleProperty( "+id+" ) in action.....  ");
            // Path 1 - find BBox
            for (i = 0; i < aLen; i++) {
                if (trace.tAry[i][0] < y0) {
                    y0 = trace.tAry[i][0];
                } else if (trace.tAry[i][0] > y1) {
                    y1 = trace.tAry[i][0];
                }

                if (trace.tAry[i][1] < x0) {
                    x0 = trace.tAry[i][1];
                } else if (trace.tAry[i][1] > x1) {
                    x1 = trace.tAry[i][1];
                }
            }
//    Log.d(TAG, "getCircleProperty( " + id + " ) - found BBox points: (" + x0 + ", " + y0 + "), (" + x1 + ", " + y1 + ") | gap "+gap+" | len = " + aLen);

            int[] xPro = new int[1 - x0 + 1];
            int[] yPro = new int[y1 - y0 + 1];
            int xProLen = xPro.length, yProLen = yPro.length;
            aspRatio = (double)yProLen / xProLen;
            for (i = 0; i < xProLen; i++) {
                xPro[i] = 0;
            }
            for (i = 0; i < yProLen; i++) {
                yPro[i] = 0;
            }

            // Path 2 - verify circle shape using projection profiles and find diameter
            if (x1 - x0 > 10 && y1 - y0 > 10) {
                for (i = 0; i < aLen; i++) {
                    x = trace.tAry[i][1];
                    y = trace.tAry[i][0];
                    xPro[(x - x0)]++;
                    yPro[(y - y0)]++;
                }

                boolean shapePoor = false;
                if (aLen < 300) {
                    for (i = 0; i < xProLen; i++) {
                        if (xPro[i] > 20 || xPro[i] < 1) {
                            shapePoor = true;
                            break;
                        }
                    }
                    if (!shapePoor) {
                        for (i = 0; i < yProLen; i++) {
                            if (yPro[i] > 20 || yPro[i] < 1) {
                                shapePoor = true;
                                break;
                            }
                        }
                    }
                } else {
                    for (i = 0; i < xProLen; i++) {
                        if (xPro[i] < 1) {
                            shapePoor = true;
                            break;
                        }
                        if (i > 5 && i < xProLen - 6) {
                            if (xPro[i] > 20) {
                                shapePoor = true;
                                break;
                            }
                        }
                    }
                    if (!shapePoor) {
                        for (i = 0; i < yProLen; i++) {
                            if (yPro[i] < 1) {
                                shapePoor = true;
                                break;
                            }
                            if (i > 5 && i < yProLen - 6) {
                                if (yPro[i] > 20) {
                                    shapePoor = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!shapePoor) {
                    diam = (int) ((x1 - x0 + y1 - y0) * 0.5);
                    xCen = (int) (0.5 * (x0 + x1));
                    yCen = (int) (0.5 * (y0 + y1));
//                        console.log("getCircleProperty( " + id + " Shape Good!");
                } else {
//                        console.log("!!! getCircleProperty( " + id + " Shape Bad!");
                }
                //		          }
//                    console.log("getCircleProperty( " + id + " ) - edge point xProfile: " + xPro.join(", "));
//                    console.log("getCircleProperty( " + id + " ) - edge point yProfile: " + yPro.join(", "));

                // calculate the edge transition profile using the 4 BBox extreme points
//                int topBBPntX, topBBPntY, bottBBPntX, bottBBPntY, leftBBPntX, leftBBPntY, rightBBPntX, rightBBPntY;
                int dif1 = 0, dif2 = 0, dif3 = 0, dif4 = 0;      // clockwise 90-degrees' transition profiles
                int dif12 = 0, dif23 = 0, dif34 = 0, dif41 = 0; // 45-degrees'
                for (i = 0; i < aLen; i++) {
                    x = trace.tAry[i][1];
                    y = trace.tAry[i][0];
                    int[] tBMVec = bMap[y];
                    if (dif4 == 0 && x == x0 && x > 0) { // left edge point
                        dif4 = tBMVec[x - 1] - tBMVec[x + 1];
                        // expand transition to 4-pix wide
                        if (dif4 < 0) {
                            dif4 = tBMVec[x - 2] - tBMVec[x + 1];
                        } else {
                            dif4 = tBMVec[x - 1] - tBMVec[x + 2];
                        }
                    } else if (dif2 == 0 && x == x1 && x > 0) { // right
                        dif2 = tBMVec[x + 1] - tBMVec[x - 1];
                        if (dif2 < 0) {
                            dif2 = tBMVec[x + 2] - tBMVec[x - 1];
                        } else {
                            dif2 = tBMVec[x + 1] - tBMVec[x - 2];
                        }
                    }

                    if (dif1 == 0 && y == y0 && y > 0) { // top edge point
                        dif1 = bMap[y - 1][x] - bMap[y + 1][x];
                        if (dif1 < 0) {
                            dif1 = bMap[y - 2][x] - bMap[y + 1][x];
                        } else {
                            dif1 = bMap[y - 1][x] - bMap[y + 2][x];
                        }
                    } else if (dif3 == 0 && y == y1 && y > 0) { // bottom edge point
                        dif3 = bMap[y + 1][x] - bMap[y - 1][x];
                        if (dif3 < 0 && y < dimym2) {
                            dif1 = bMap[y + 2][x] - bMap[y - 1][x];
                        } else {
                            dif3 = bMap[y + 1][x] - bMap[y - 2][x];
                        }
                    }
                }
                if (dif1 * dif2 > 0 && dif3 * dif4 > 0) {
                    if (dif1 * dif3 > 0) { // all transitions are consistent
                        type = (dif1 > 0) ? 1 : -1;
                    }
                }

                trans[0] = dif1;
                trans[2] = dif2;
                trans[4] = dif3;
                trans[6] = dif4;
                /// Complete the 45-degree's measurement and record the data
                //  We limit the search to a 11X11 block
//                int tIdx, Idx;
                int tPosX, tPosY;
                boolean FoundIt = false;
                int dist_45 = (int) (diam * 0.707 * 0.5);
                int xpo = xCen + dist_45, ypo = yCen - dist_45; // Quad 1
                int srchLen = 5 + (diam / 50);

                for (i = 0; i < srchLen; i++) {
                    tPosY = ypo + i;
                    if (tPosY < 1 || tPosY > dimy - 1) continue;

                    for (j = 0; j < srchLen; j++) {
                        tPosX = xpo + j;
                        if (tPosX < 1 || tPosX > dimx - 1) continue;

                        if (gMap[tPosY][tPosX] == mark) {
                            dif12 = bMap[tPosY - 1][tPosX + 1] - bMap[tPosY + 1][tPosX - 1];
                            FoundIt = true;
                        }
                        if (!FoundIt) {
                            tPosX = xpo - j;
                            if (tPosX < 1 || tPosX > dimx - 1) continue;

                            if (gMap[tPosY][tPosX] == mark) {
                                dif12 = bMap[tPosY - 1][tPosX + 1] - bMap[tPosY + 1][tPosX - 1];
                                FoundIt = true;
                            }
                        }
                        if (FoundIt) { // expand to 4-pix wide
                            if (dif12 > 0) {
                                dif12 = bMap[tPosY - 1][tPosX + 1] - bMap[tPosY + 2][tPosX - 2];
                            } else {
                                dif12 = bMap[tPosY - 2][tPosX + 2] - bMap[tPosY + 1][tPosX - 1];
                            }
                            break;
                        }
                    }
                    if (FoundIt) break;
                    else {
                        tPosY = ypo - i;
                        if (tPosY < 1 || tPosY > dimy - 1) continue;

                        for (j = 0; j < srchLen; j++) {
                            tPosX = xpo + j;
                            if (tPosX < 1 || tPosX > dimx - 1) continue;

                            if (gMap[tPosY][tPosX] == mark) {
                                dif12 = bMap[tPosY - 1][tPosX + 1] - bMap[tPosY + 1][tPosX - 1];
                                FoundIt = true;
                            }
                            if (!FoundIt) {
                                tPosX = xpo - j;
                                if (tPosX < 1 || tPosX > dimx - 1) continue;

                                if (gMap[tPosY][tPosX] == mark) {
                                    dif12 = bMap[tPosY - 1][tPosX + 1] - bMap[tPosY + 1][tPosX - 1];
                                    FoundIt = true;
                                }
                            }
                            if (FoundIt) { // expand to 4-pix wide
                                if (dif12 > 0) {
                                    dif12 = bMap[tPosY - 1][tPosX + 1] - bMap[tPosY + 2][tPosX - 2];
                                } else {
                                    dif12 = bMap[tPosY - 2][tPosX + 2] - bMap[tPosY + 1][tPosX - 1];
                                }
                                break;
                            }
                        }
                        if (FoundIt) break;
                    }
                }

                FoundIt = false;
                xpo = xCen + dist_45;
                ypo = yCen + dist_45; // Quad 2
                for (i = 0; i < srchLen; i++) {
                    tPosY = ypo + i;
                    if (tPosY < 1 || tPosY > dimy - 1) continue;

                    for (j = 0; j < srchLen; j++) {
                        tPosX = xpo + j;
                        if (tPosX < 1 || tPosX > dimx - 1) continue;

                        if (gMap[tPosY][tPosX] == mark) {
                            dif23 = bMap[tPosY + 1][tPosX + 1] - bMap[tPosY - 1][tPosX - 1];
                            FoundIt = true;
                        }
                        if (!FoundIt) {
                            tPosX = xpo - j;
                            if (tPosX < 1 || tPosX > dimx - 1) continue;

                            if (gMap[tPosY][tPosX] == mark) {
                                dif23 = bMap[tPosY + 1][tPosX + 1] - bMap[tPosY - 1][tPosX - 1];
                                FoundIt = true;
                            }
                        }
                        if (FoundIt) { // expand to 4-pix wide
                            if (dif23 > 0) {
                                dif23 = bMap[tPosY + 1][tPosX + 1] - bMap[tPosY - 2][tPosX - 2];
                            } else {
                                dif23 = bMap[tPosY + 2][tPosX + 2] - bMap[tPosY + 1][tPosX - 1];
                            }
                            break;
                        }
                    }
                    if (FoundIt) break;
                    else {
                        tPosY = ypo - i;
                        if (tPosY < 1 || tPosY > dimy - 1) continue;

                        for (j = 0; j < srchLen; j++) {
                            tPosX = xpo + j;
                            if (tPosX < 1 || tPosX > dimx - 1) continue;

                            if (gMap[tPosY][tPosX] == mark) {
                                dif23 = bMap[tPosY + 1][tPosX + 1] - bMap[tPosY - 1][tPosX - 1];
                                FoundIt = true;
                            }
                            if (!FoundIt) {
                                tPosX = xpo - j;
                                if (tPosX < 1 || tPosX > dimx - 1) continue;

                                if (gMap[tPosY][tPosX] == mark) {
                                    dif23 = bMap[tPosY + 1][tPosX + 1] - bMap[tPosY - 1][tPosX - 1];
                                    FoundIt = true;
                                }
                            }
                            if (FoundIt) { // expand to 4-pix wide
                                if (dif23 > 0) {
                                    dif23 = bMap[tPosY + 1][tPosX + 1] - bMap[tPosY - 2][tPosX - 2];
                                } else {
                                    dif23 = bMap[tPosY + 2][tPosX + 2] - bMap[tPosY + 1][tPosX - 1];
                                }
                                break;
                            }
                        }
                        if (FoundIt) break;
                    }
                }

                FoundIt = false;
                xpo = xCen - dist_45;
                ypo = yCen + dist_45; // Quad 3
                for (i = 0; i < srchLen; i++) {
                    tPosY = ypo + i;
                    if (tPosY < 1 || tPosY > dimy - 1) continue;

                    for (j = 0; j < srchLen; j++) {
                        tPosX = xpo + j;
                        if (tPosX < 1 || tPosX > dimx - 1) continue;

                        if (gMap[tPosY][tPosX] == mark) {
                            dif34 = bMap[tPosY + 1][tPosX - 1] - bMap[tPosY - 1][tPosX + 1];
                            FoundIt = true;
                        }
                        if (!FoundIt) {
                            tPosX = xpo - j;
                            if (tPosX < 1 || tPosX > dimx - 1) continue;

                            if (gMap[tPosY][tPosX] == mark) {
                                dif34 = bMap[tPosY + 1][tPosX - 1] - bMap[tPosY - 1][tPosX + 1];
                                FoundIt = true;
                            }
                        }
                        if (FoundIt) { // expand to 4-pix wide
                            if (dif34 > 0) {
                                dif34 = bMap[tPosY + 1][tPosX - 1] - bMap[tPosY - 2][tPosX + 2];
                            } else {
                                dif34 = bMap[tPosY + 2][tPosX - 2] - bMap[tPosY - 1][tPosX + 1];
                            }
                            break;
                        }
                    }
                    if (FoundIt) break;
                    else {
                        tPosY = ypo - i;
                        if (tPosY < 1 || tPosY > dimy - 1) continue;

                        for (j = 0; j < srchLen; j++) {
                            tPosX = xpo + j;
                            if (tPosX < 1 || tPosX > dimx - 1) continue;

                            if (gMap[tPosY][tPosX] == mark) {
                                dif34 = bMap[tPosY + 1][tPosX - 1] - bMap[tPosY - 1][tPosX + 1];
                                FoundIt = true;
                                break;
                            }
                            if (!FoundIt) {
                                tPosX = xpo - j;
                                if (tPosX < 1 || tPosX > dimx - 1) continue;

                                if (gMap[tPosY][tPosX] == mark) {
                                    dif34 = bMap[tPosY + 1][tPosX - 1] - bMap[tPosY - 1][tPosX + 1];
                                    FoundIt = true;
                                    break;
                                }
                            }
                            if (FoundIt) { // expand to 4-pix wide
                                if (dif34 > 0) {
                                    dif34 = bMap[tPosY + 1][tPosX - 1] - bMap[tPosY - 2][tPosX + 2];
                                } else {
                                    dif34 = bMap[tPosY + 2][tPosX - 2] - bMap[tPosY - 1][tPosX + 1];
                                }
                                break;
                            }
                        }
                        if (FoundIt) break;
                    }
                }

                FoundIt = false;
                xpo = xCen - dist_45;
                ypo = yCen - dist_45; // Quad 4
                for (i = 0; i < srchLen; i++) {
                    tPosY = ypo + i;
                    if (tPosY < 1 || tPosY > dimy - 1) continue;

                    for (j = 0; j < srchLen; j++) {
                        tPosX = xpo + j;
                        if (tPosX < 1 || tPosX > dimx - 1) continue;

                        if (gMap[tPosY][tPosX] == mark) {
                            dif41 = bMap[tPosY - 1][tPosX - 1] - bMap[tPosY + 1][tPosX + 1];
                            FoundIt = true;
                        }
                        if (!FoundIt) {
                            tPosX = xpo - j;
                            if (tPosX < 1 || tPosX > dimx - 1) continue;

                            if (gMap[tPosY][tPosX] == mark) {
                                dif41 = bMap[tPosY - 1][tPosX - 1] - bMap[tPosY + 1][tPosX + 1];
                                FoundIt = true;
                            }
                        }
                        if (FoundIt) { // expand to 4-pix wide
                            if (dif41 > 0) {
                                dif41 = bMap[tPosY - 1][tPosX - 1] - bMap[tPosY + 2][tPosX + 2];
                            } else {
                                dif41 = bMap[tPosY - 2][tPosX - 2] - bMap[tPosY + 1][tPosX + 1];
                            }
                            break;
                        }
                    }
                    if (FoundIt) break;
                    else {
                        tPosY = ypo - i;
                        if (tPosY < 1 || tPosY > dimy - 1) continue;

                        for (j = 0; j < srchLen; j++) {
                            tPosX = xpo + j;
                            if (tPosX < 1 || tPosX > dimx - 1) continue;

                            if (gMap[tPosY][tPosX] == mark) {
                                dif41 = bMap[tPosY - 1][tPosX - 1] - bMap[tPosY + 1][tPosX + 1];
                                FoundIt = true;
                            }
                            if (!FoundIt) {
                                tPosX = xpo - j;
                                if (tPosX < 1 || tPosX > dimx - 1) continue;

                                if (gMap[tPosY][tPosX] == mark) {
                                    dif41 = bMap[tPosY - 1][tPosX - 1] - bMap[tPosY + 1][tPosX + 1];
                                    FoundIt = true;
                                }
                            }
                            if (FoundIt) { // expand to 4-pix wide
                                if (dif41 > 0) {
                                    dif41 = bMap[tPosY - 1][tPosX - 1] - bMap[tPosY + 2][tPosX + 2];
                                } else {
                                    dif41 = bMap[tPosY - 2][tPosX - 2] - bMap[tPosY + 1][tPosX + 1];
                                }
                                break;
                            }
                        }
                        if (FoundIt) break;
                    }
                }
                trans[1] = dif12;
                trans[3] = dif23;
                trans[5] = dif34;
                trans[7] = dif41;
            }
//                console.log("getCircleProperty( " + id + " ) - center(x, y) | diameter: (" + xCen + ", " + yCen + ") | " + diam);
//                console.log("  getCircleProperty( " + id + " ) - transition profile: " + trans.join(", "));
        } else {
//                console.log("getCircleProperty( " + id + " ) quitted: circumference/gap = " + aLen + "/" + gap);
        }

        TCircle  mCircle = new TCircle(mark, xCen, yCen, aspRatio, diam, type, trans, concentric);
        return mCircle;
    }

    public TCircle getSmallCircleProperty(tTrace trace, int id, int mark, int[][] bMap, int dimx, int dimy, int[][]gMap, int minLen) {
        /////    getSmallCircleProperty: function (trace, id, mark, bMap, dimx, dimy, gMap, minLen) {
        int          xCen = -1, yCen = -1, diam = -1;
        double   aspRatio = 0.0;
//        int[][]    pntAry = trace.tAry; // [ypos, xpos]
        int          aLen = trace.tAry.length;
        int[][]  pntAryCp = null;
        int            x0 = 99999, x1 = 0, y0 = 99999, y1 = 0;
        int             x, y, i, j;
        int           gap = trace.gap; // from the quick formula
        int     openWidth = 9999; // from point-by-point tracing
        int          type = 0; // nutural; unknown. Value: (-1, 0, 1) - (darker outward, unknown, brighter)
        int[]       trans = {0, 0, 0, 0, 0, 0, 0, 0}; // 8-direction transition profiles

            if (aLen > minLen && gap <= 3) {
                // go through the entire path and calculate the center and the diameter
                //		      console.log(" getSmallCircleProperty( "+id+" ) in action.....  ");
                // Path 1 - find BBox
                for (i = 0; i < aLen; i++) {
                    pntAryCp[i] = new int[2];
                    int[]   tPt = {trace.tAry[i][0], trace.tAry[i][1]};
                    if (tPt[0] < y0) {
                        y0 = tPt[0];
                    } else if (tPt[0] > y1) { y1 = tPt[0]; }

                    if (tPt[1] < x0) {
                        x0 = tPt[1];
                    } else if (tPt[1] > x1) { x1 = tPt[1]; }
                }
                //		      console.log("getSmallCircleProperty( " + id + " ) - found BBox points: (" + x0 + ", " + y0 + "), (" + x1 + ", " + y1 + ") | gap "+gap+" | len = " + aLen);

                int    xProLen = x1 - x0 + 1, yProLen = y1 - y0 + 1;
                int[]     xPro = new int[xProLen], yPro = new int[yProLen];
                for (i = 0; i < xProLen; i++) { xPro[i] = 0; }
                for (i = 0; i < yProLen; i++) { yPro[i] = 0; }
                aspRatio = (double)yProLen / xProLen;

                // Path 2 - verify circle shape using projection profiles and find diameter
                if (xProLen > 16 && yProLen > 16) { // should use the regular one
                    TCircle cProp = getCircleProperty(trace, id, mark, bMap, dimx, dimy, gMap); // , minLen);
                    return cProp;
                }

                // check if the center is an empty area
                diam = (int)((x1 - x0 + y1 - y0) * 0.5);
                xCen = (int)(0.5 * (x0 + x1));
                yCen = (int)(0.5 * (y0 + y1));
                Log.d(TAG, "getSmallCircleProperty( " + id + ") center at: " + xCen + ", " + yCen);

                for (i = 0; i < aLen; i++) { // get the projection profiles in both dimensions and make a copy of the trace locations
                    int     xx = trace.tAry[i][1];
                    int     yy = trace.tAry[i][0];
                    xPro[(xx - x0)]++;
                    yPro[(yy - y0)]++;
                    pntAryCp[i][1] = xx;
                    pntAryCp[i][0] = yy;
                }
                int      xProLenm = xProLen - 1, yProLenm = yProLen - 1;
                boolean  shapePoor = false;
                for (i = 1; i < xProLenm; i++) {
                    if (xPro[i] < 2) {
                        shapePoor = true;
                        break;
                    }
                }
                if (!shapePoor) {
                    for (i = 1; i < yProLenm; i++) {
                        if (yPro[i] < 2) {	  // broken ring - not a good circle
                            shapePoor = true;
                            break;
                        }
                    }
                    if (shapePoor) {	// broken ring - not a good circle
                        /*
                        return {
                                trcID: mark,
                                centerX: xCen,
                                centerY: yCen,
                                aspectRat: aspRatio,
                                diameter: diam,
                                type: type,
                                prof: trans,
                                concentric: -1
                        }
                        */

                        TCircle  mCircle = new TCircle(mark, xCen, yCen, aspRatio, diam, type, trans, -1);
                        return mCircle;
                    }

                    // check for contour gaps and center cavity shape from the projection profiles both dimensions
                    int[] xCav = new int[xProLen];
                    int[] yCav = new int[yProLen];
                    int  holeSize = 0, holeSize1 = 0;

//                    pntAryCp.sort(function (a, b) { if (a[1] == b[1]) return (a[0] < b[0]) ? -1 : 1; return a[1] - b[1]; });  // sort y first
                    pntAryCp = PosiSort(pntAryCp, false, true);

                    int    xx0 = pntAryCp[0][1], yy0 = pntAryCp[0][0];
                    double avgGray = 0.0, cenGray = 0.0;
                    xCav[0] = 0;
                    for (i = 1, j = 1; i < xProLen && j < aLen; j++) {
                        // count the unoccupied pixels inside the circle
                        if (pntAryCp[j][1] != xx0) { // new colume starts
                            xx0 = pntAryCp[j][1];
                            holeSize += xCav[i - 1];
                            xCav[i++] = 0;
                            continue;
                        } else if (pntAryCp[j][0] - pntAryCp[j - 1][0] > 1) { // update graySum of the center hole
                            xCav[i - 1] += (pntAryCp[j][0] - pntAryCp[j - 1][0] - 1);
                            //              console.log("detected gap of size :" + (pntAryCp[j][0] - pntAryCp[j - 1][0] - 1));
                            for (int k = pntAryCp[j - 1][0] + 1; k < pntAryCp[j][0]; k++) {

                                //              console.log("     - adding gap pixel gray level at k = "+ k);

                                avgGray += bMap[k][xx0];
                            }
                        }
                    }

                    avgGray /= (0.00001 + holeSize);
                    cenGray = bMap[yCen][xCen];
//			      console.log(" getSmallCircleProperty( " + id + ") xCavity Profile: " + xCav.join(", ") + " / holeSize = " + holeSize + " / avg Gray = " + avgGray.toFixed(2));

//                    pntAryCp.sort(function (a, b) { if (a[0] == b[0]) return (a[1] < b[1]) ? -1 : 1; return a[0] - b[0]; });  // sort x first
                    pntAryCp = PosiSort(pntAryCp, true, true);

                    yCav[0] = 0;
                    for (i = 1, j = 1; i < yProLen && j < aLen; j++) {
                        // count the unoccupied pixels inside the circle
                        if (pntAryCp[j][0] != yy0) { // new colume starts
                            yy0 = pntAryCp[j++][0];
                            holeSize1 += yCav[i - 1];
                            yCav[i++] = 0;
                            continue;
                        } else if (pntAryCp[j][1] - pntAryCp[j - 1][1] > 1) { // update graySum of the center hole
                            yCav[i - 1] += (pntAryCp[j][1] - pntAryCp[j - 1][1] - 1);
                        }
                    }
//			Log.d(TAG, " getSmallCircleProperty( " + id + ") yCavity Profile: " + yCav.join(", ") + " / holeSize1 = " + holeSize1 + " / center Gray = " + cenGray.toFixed(2));

                    int    dif1 = 0, dif2 = 0, dif3 = 0, dif4 = 0;
                    int    dif12 = 0, dif23 = 0, dif34 = 0, dif41 = 0;

                    if(y0 < 2 || x0 < 2 || y0 > dimy - 2 || x0 > dimx -2) {
                        type = 0;
                    } else {
                        dif1 = (int) (bMap[y0 - 2][xCen] - cenGray);
                        dif3 = (int) (bMap[y1 + 2][xCen] - cenGray);
                        dif2 = (int) (bMap[yCen][x1 + 2] - cenGray);
                        dif4 = (int) (bMap[yCen][x0 - 2] - cenGray);

                        if (dif1 * dif2 > 0 && dif3 * dif4 > 0) {
                            if (dif1 * dif3 > 0) { // all transitions are consistent
                                type = (dif1 > 0) ? 1 : -1;
                            }
                        }
                    }

                    trans[0] = dif1;
                    trans[2] = dif2;
                    trans[4] = dif3;
                    trans[6] = dif4;

                    dif12 = (int)(bMap[y0 + 1][x1 - 1] - cenGray);
                    dif34 = (int)(bMap[y1 - 1][x0 + 1] - cenGray);
                    dif23 = (int)(bMap[y1 - 1][x1 - 1] - cenGray);
                    dif41 = (int)(bMap[y0 + 1][x0 + 1] - cenGray);

                    trans[1] = dif12;
                    trans[3] = dif23;
                    trans[5] = dif34;
                    trans[7] = dif41;
                    //			      console.log("getSmallCircleProperty( " + id + " ) - center(x, y) | diameter: (" + xCen + ", " + yCen + ") | " + diam);
                    //			      console.log("  getSmallCircleProperty( " + id + " ) - transition profile: " + trans.join(", "));
                }
            } else {
                Log.d(TAG, "getSmallCircleProperty( " + id + " ) quitted: circumference/gap = " + aLen + "/" + gap);
            }
/*
            return {
                    trcID: mark,
                    centerX: xCen,
                    centerY: yCen,
                    aspectRat: aspRatio,
                    diameter: diam,
                    type: type,
                    prof: trans,
                    concentric: -1
            };
        }
        */

        TCircle  mCircle = new TCircle(mark, xCen, yCen, aspRatio, diam, type, trans, -1);
        return mCircle;

    }

    public int[][] PosiSort(int[][] posAry, boolean useX, boolean Increase) {
        // sort the 2D array posAry using either the first element (x) or the second (y)

        int      len = posAry.length, i, j;
        boolean  needSwap;
        if (len < 1) return null;

        int[][]  rAry = new int[len][2];
        int[]    wPos = new int[2];
        int[]    tPos = new int[2];

        for(i=0; i<len; i++) {
            rAry[i][0] = posAry[i][0];
            rAry[i][1] = posAry[i][1];
        }

        if(Increase) {
            for(i=0; i<len-1; i++) {
                wPos[0] = rAry[i][0];
                wPos[1] = rAry[i][1];
                for(j = i+1; j<len; j++) {
                    tPos[0]  = rAry[j][0];
                    tPos[1]  = rAry[j][1];
                    needSwap = false;
                    if(useX) {
                        if(tPos[0] < wPos[0]) { // swap posAry
                            needSwap = true;
                        } else if(tPos[0] == wPos[0]) { // check 'y'
                            if(tPos[1] < wPos[1]) { // swap posAry
                                needSwap = true;
                            }
                        }
                    } else {
                        if(tPos[1] < wPos[1]) { // swap posAry
                            needSwap = true;
                        } else if(tPos[1] == wPos[1]) { // check 'x'
                            if(tPos[0] < wPos[0]) { // swap posAry
                                needSwap = true;
                            }
                        }
                    }
                    if(needSwap) {
                        rAry[i][0] = tPos[0];
                        rAry[i][1] = tPos[1];
                        rAry[j][0] = wPos[0];
                        rAry[j][1] = wPos[1];
                        wPos[0] = tPos[0];
                        wPos[1] = tPos[1];
                    }
                }
            }
        } else {
            for(i=0; i<len-1; i++) {
                wPos[0] = rAry[i][0];
                wPos[1] = rAry[i][1];
                for(j = i+1; j<len; j++) {
                    tPos[0]  = rAry[j][0];
                    tPos[1]  = rAry[j][1];
                    needSwap = false;
                    if(useX) {
                        if(tPos[0] > wPos[0]) { // swap posAry
                            needSwap = true;
                        } else if(tPos[0] == wPos[0]) { // check 'y'
                            if(tPos[1] > wPos[1]) { // swap posAry
                                needSwap = true;
                            }
                        }
                    } else {
                        if(tPos[1] > wPos[1]) { // swap posAry
                            needSwap = true;
                        } else if(tPos[1] == wPos[1]) { // check 'x'
                            if(tPos[0] > wPos[0]) { // swap posAry
                                needSwap = true;
                            }
                        }
                    }

                    if(needSwap) {
                        rAry[i][0] = tPos[0];
                        rAry[i][1] = tPos[1];
                        rAry[j][0] = wPos[0];
                        rAry[j][1] = wPos[1];
                        wPos[0] = tPos[0];
                        wPos[1] = tPos[1];
                    }
                }
            }
        }
        return rAry;
    }
}
