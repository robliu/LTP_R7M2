package com.get.gsappalpha1;

/**
 *  ===== This work is copyrighted! Do not copy or reuse without Guidance permission! ======
 *
 *    For detail, contact www.guidance-edu.com
 *
 *
 *     Created by Rob on 3/7/2017.
 *
 *  Copy Rights 2017, Guidance Education Technologies, All Rights Reserved.
 *
 */

import android.util.Log;

import java.util.ArrayList;


public class laserHit { // hit record in a single hit frame

    public  int  FmNum;
    public  long cTime;             // time lapse in Session
    public  int  hitID;
    public  int  xCenter;
    public  int  yCenter;
    public  int  state;             // 0: initial; 1: raw data; 2: processed (xCenter/yCenter ready)
    public  int  posLen;
    public  int  session;           // which (Timer) session it was recorded in

    public  ArrayList<Short> xPos;
    public  ArrayList<Short> yPos;
    public  ArrayList<Short> Code1;
    public  ArrayList<Short> Code2;

    private static final String TAG   = "===laserHit";


    public void setup() {
        xPos  = new ArrayList<>();
        yPos  = new ArrayList<>();
        Code1 = new ArrayList<>();
        Code2 = new ArrayList<>();

        cTime   = 0L;
        hitID   = -1;
        state   = 0;
        posLen  = 0;
        session = -1;
        xCenter = -1;
        yCenter = -1;
    }

    public void setup(long time) {
        xPos  = new ArrayList<>();
        yPos  = new ArrayList<>();
        Code1 = new ArrayList<>();
        Code2 = new ArrayList<>();

        cTime   = time;
        hitID   = -1;
        state   = 0;
        posLen  = 0;
        session = -1;
        xCenter = -1;
        yCenter = -1;
    }

    public laserHit copy( ) {
        int         len = xPos.size();
        laserHit  newLH = new laserHit();
        newLH.setup();

        newLH.cTime   = cTime;
        newLH.hitID   = hitID;
        newLH.state   = state;
        newLH.posLen  = posLen;
        newLH.session = session;
        newLH.xCenter = xCenter;
        newLH.yCenter = yCenter;

        Log.d(TAG, " laserHit:copy() copied; len / cTime / hitID / state / posLen = "+len+" / "+newLH.cTime +" / "+newLH.hitID+" / "+newLH.state+" / "+newLH.posLen);

        for (int i = 0; i < len; i++) {
            newLH.add(xPos.get(i), yPos.get(i), Code1.get(i), Code2.get(i));
        }

        return newLH;
    }

    public int findAvgX(int MaxLen) {
        int  sum = 0;
        int xLen = xPos.size();
        if (xLen < 1) return 0;

        if(xLen > MaxLen) xLen = MaxLen;

        for (int i = 0; i < xLen; i++) {
            sum += xPos.get(i);
        }

        return (sum / xLen);
    }


    public int[] findAvgXYPlus(int MaxLen, boolean UseOld) {
        //   calculate the average position of xPos[] without the outliers, which is determined
        // by the distance to the NN.
        //
        //  Input:
        //    MaxLen - max length of input data points of concern (to handle big data size)
        //    UseOld - use the existing xCenter/yCenter if already exist (to reduce duplicate computation)
        //
        // Note:
        //   MaxLen is disabled due to incomplete implementation as of 09/16/2017

        int[]  rPos = new int[2];

        if(UseOld && state == 2) {
            rPos[0] = xCenter;
            rPos[1] = yCenter;
            return rPos;
        }

        int    xLen = xPos.size();
        if (xLen < 1) return rPos;

        int       i;
//        int[]  hist = new int[256];

        Log.d(TAG, "+ findAvgXYPlus() entered; xLen = "+xLen);

        sortXY(1);     // sort X in ascending
        setNBRCount(); // set the 8-Nbr count

        int  wt;
        int  wtCount = 0; // weighted count; artificial sample count using NN # as weight
        int  sumx    = 0, sumy = 0;

        for (i = 0; i < xLen; i++) {
            if (Code1.get(i) < 1) {
//                Log.d(TAG, "  xx findAvgXYPlus() skipping bad point: "+i);
                continue; // ignore this isolated/bad pixel
            }

            wt = Code1.get(i); // 8-Nbr count
            int  intense = Code2.get(i);

            if(intense <= 0) continue;

//            hist[intense]++;
            double  wtinten = GetIntenseWeight(intense, 90, 10);

            wt = (int)(wt*wtinten); // weight is (#Nbr x Intensity Level)

            sumx += xPos.get(i) * wt;
            sumy += yPos.get(i) * wt;
            wtCount += wt;
        }

        if(wtCount > 0) {
            rPos[0] = sumx / wtCount;
            rPos[1] = sumy / wtCount;

//            Log.d(TAG, "    ++++ findAvgXYPlus() returns: "+rPos[0]+", "+rPos[1]+" | wtCount = "+wtCount);
        } else {
            Log.d(TAG, "    XXXX findAvgXYPlus() failed! wtCount = "+wtCount);
        }

        xCenter = rPos[0];
        yCenter = rPos[1];
        state   = 2;

        return rPos;
    }


    public void addCoordinate(short x, short y) {
        xPos .add(x);
        yPos .add(y);
        Code1.add((short)-1);
        Code2.add((short)-1);
    }

    public void addCoordinatePlus(short x, short y, short c1) {
        xPos .add(x);
        yPos .add(y);
        Code1.add(c1);
        Code2.add((short)-1);
    }

    public void add(short x, short y, short c1, short c2) {
        xPos .add(x);
        yPos .add(y);
        Code1.add(c1);
        Code2.add(c2);
    }

    public void sortXY(int mode) {
        int    i, j;
        short  x1, x2, y1, y2;
        short  c1a, c2a, c1b, c2b;
        int    dLen  = xPos.size();
        int    dLenm = dLen -1;

        if(mode == 1) { // x ascending
            for (i = 0; i < dLenm; i++) {
                x1  = xPos.get(i);
                y1  = yPos.get(i);
                c1a = Code1.get(i);
                c2a = Code2.get(i);

                for(j = i+1; j<dLen; j++) {
                    x2 = xPos.get(j);
                    y2 = yPos.get(j);

                    if(x1 > x2) {
                        xPos.set(i, x2);
                        xPos.set(j, x1);
                        yPos.set(i, y2);
                        yPos.set(j, y1);

                        c1b = Code1.get(j);
                        c2b = Code2.get(j);
                        Code1.set(i, c1b);
                        Code2.set(i, c2b);
                        Code1.set(j, c1a);
                        Code2.set(j, c2a);
                        x1 = x2;
                        y1 = y2;
                        c1a = c1b;
                        c2a = c2b;
                    }
                }
            }
        } else if(mode == 2) { // y ascending
            for (i = 0; i < dLenm; i++) {
                x1  = xPos.get(i);
                y1  = yPos.get(i);
                c1a = Code1.get(i);
                c2a = Code2.get(i);

                for(j = i+1; j<dLen; j++) {
                    x2 = xPos.get(j);
                    y2 = yPos.get(j);

                    if(y1 > y2) {
                        xPos.set(i, x2);
                        xPos.set(j, x1);
                        yPos.set(i, y2);
                        yPos.set(j, y1);

                        c1b = Code1.get(j);
                        c2b = Code2.get(j);
                        Code1.set(i, c1b);
                        Code2.set(i, c2b);
                        Code1.set(j, c1a);
                        Code2.set(j, c2a);
                        x1  = x2;
                        y1  = y2;
                        c1a = c1b;
                        c2a = c2b;
                    }
                }
            }
        } else if(mode == -1) { // x descending
            for (i = 0; i < dLenm; i++) {
                x1  = xPos.get(i);
                y1  = yPos.get(i);
                c1a = Code1.get(i);
                c2a = Code2.get(i);

                for(j = i+1; j<dLen; j++) {
                    x2 = xPos.get(j);
                    y2 = yPos.get(j);

                    if(x1 < x2) {
                        xPos.set(i, x2);
                        xPos.set(j, x1);
                        yPos.set(i, y2);
                        yPos.set(j, y1);

                        c1b = Code1.get(j);
                        c2b = Code2.get(j);
                        Code1.set(i, c1b);
                        Code2.set(i, c2b);
                        Code1.set(j, c1a);
                        Code2.set(j, c2a);
                        x1  = x2;
                        y1  = y2;
                        c1a = c1b;
                        c2a = c2b;
                    }
                }
            }
        } else if(mode == -2) { // y descending
            for (i = 0; i < dLenm; i++) {
                x1  = xPos.get(i);
                y1  = yPos.get(i);
                c1a = Code1.get(i);
                c2a = Code2.get(i);

                for(j = i+1; j<dLen; j++) {
                    x2 = xPos.get(j);
                    y2 = yPos.get(j);

                    if(y1 < y2) {
                        xPos.set(i, x2);
                        xPos.set(j, x1);
                        yPos.set(i, y2);
                        yPos.set(j, y1);

                        c1b = Code1.get(j);
                        c2b = Code2.get(j);
                        Code1.set(i, c1b);
                        Code2.set(i, c2b);
                        Code1.set(j, c1a);
                        Code2.set(j, c2a);
                        x1  = x2;
                        y1  = y2;
                        c1a = c1b;
                        c2a = c2b;
                    }
                }
            }
        }
    }

    public void sortByClassCode(int mode) {
        int   i, j;
        short x1, x2, y1, y2;
        short c1a, c2a, c1b, c2b;
        int   dLen  = xPos.size();
        int   dLenm = dLen -1;
        if(mode == 1) { // Code1 ascending
            for (i = 0; i < dLenm; i++) {
                x1  = xPos.get(i);
                y1  = yPos.get(i);
                c1a = Code1.get(i);
                c2a = Code2.get(i);

                for(j = i+1; j<dLen; j++) {
                    c1b = Code1.get(j);
                    c2b = Code2.get(j);

                    if(c1a > c1b) {
                        x2 = xPos.get(j);
                        y2 = yPos.get(j);
                        xPos.set(i, x2);
                        xPos.set(j, x1);
                        yPos.set(i, y2);
                        yPos.set(j, y1);

                        Code1.set(i, c1b);
                        Code2.set(i, c2b);
                        Code1.set(j, c1a);
                        Code2.set(j, c2a);
                        x1  = x2;
                        y1  = y2;
                        c1a = c1b;
                        c2a = c2b;
                    }
                }
            }
        } else if(mode == 2) { // Code2 ascending
            for (i = 0; i < dLenm; i++) {
                x1  = xPos.get(i);
                y1  = yPos.get(i);
                c1a = Code1.get(i);
                c2a = Code2.get(i);

                for(j = i+1; j<dLen; j++) {
                    c1b = Code1.get(j);
                    c2b = Code2.get(j);

                    if(c2a > c2b) {
                        x2 = xPos.get(j);
                        y2 = yPos.get(j);
                        xPos.set(i, x2);
                        xPos.set(j, x1);
                        yPos.set(i, y2);
                        yPos.set(j, y1);

                        Code1.set(i, c1b);
                        Code2.set(i, c2b);
                        Code1.set(j, c1a);
                        Code2.set(j, c2a);
                        x1  = x2;
                        y1  = y2;
                        c1a = c1b;
                        c2a = c2b;
                    }
                }
            }
        } else if(mode == -1) { // Code1 descending
            for (i = 0; i < dLenm; i++) {
                x1  = xPos.get(i);
                y1  = yPos.get(i);
                c1a = Code1.get(i);
                c2a = Code2.get(i);

                for(j = i+1; j<dLen; j++) {
                    c1b = Code1.get(j);
                    c2b = Code2.get(j);

                    if(c1a < c1b) {
                        x2 = xPos.get(j);
                        y2 = yPos.get(j);
                        xPos.set(i, x2);
                        xPos.set(j, x1);
                        yPos.set(i, y2);
                        yPos.set(j, y1);

                        Code1.set(i, c1b);
                        Code2.set(i, c2b);
                        Code1.set(j, c1a);
                        Code2.set(j, c2a);
                        x1  = x2;
                        y1  = y2;
                        c1a = c1b;
                        c2a = c2b;
                    }
                }
            }
        } else if(mode == -2) { // Code2 descending
            for (i = 0; i < dLenm; i++) {
                x1  = xPos.get(i);
                y1  = yPos.get(i);
                c1a = Code1.get(i);
                c2a = Code2.get(i);

                for(j = i+1; j<dLen; j++) {
                    c1b = Code1.get(j);
                    c2b = Code2.get(j);

                    if(c2a < c2b) {
                        x2 = xPos.get(j);
                        y2 = yPos.get(j);
                        xPos.set(i, x2);
                        xPos.set(j, x1);
                        yPos.set(i, y2);
                        yPos.set(j, y1);

                        Code1.set(i, c1b);
                        Code2.set(i, c2b);
                        Code1.set(j, c1a);
                        Code2.set(j, c2a);
                        x1  = x2;
                        y1  = y2;
                        c1a = c1b;
                        c2a = c2b;
                    }
                }
            }
        }
    }

    public void setNBRCount( ) {
        // find the 8-neighbor count and store results in classCode1
        //
        //  Code value:
        //   1 - 8:  normal neighbor count
        //     -99:  0 neighbor
        //    -999:  bad data pixel


        int          dLen     = xPos.size();
        final short  zeros    = (short)0;
        final short  neg99s   = (short)-99;
        final short  neg999s  = (short)-999;

//        Log.d(TAG, " setNBRCount() - data length = "+dLen);

        for(int i=0; i<dLen; i++) {
            Code1.set(i, zeros);
        }
        for(int i=0; i<dLen; i++) {
            final short  x0  = xPos.get(i);
            final short  y0  = yPos.get(i);
            short       tc1  = (short)-1;

            for(int j=0; j<dLen; j++) { // self included!
                final short  x1 = xPos.get(j);
                if( (x0 - x1) > 1 || (x0 - x1) < -1) continue;

                final short  y1 = yPos.get(j);
                if( (y0 - y1) > 1 || (y0 - y1) < -1) continue;

                tc1++;
            }
            Code1.set(i, tc1);
        }

        for(int i=0; i<dLen; i++) { // mark isolated pixels
            if(Code1.get(i) > 9 || Code1.get(i) < 0)  Code1.set(i, neg999s);   // bad data
            else if(Code1.get(i) == 0)                Code1.set(i, neg99s);
        }
    }

    public int[] findEndPoints() {
        int i, x, y;
        int idx2 = 0;
        int dLen = xPos.size();

        int x1   = -1;
        int y1   = -1;
        int x2   = -1;
        int y2   = -1;
        int dist = 0;
        int nbr1 = 0;
        int nbr2 = 0;
        if(dLen < 2) {
            Log.d("== findEndPoints", " Hot pixel list too short: "+dLen);
            int[] rtn = new int []{x1, y1, x2, y2, nbr1, nbr2, dist};
            return rtn;
        }

        setNBRCount();
        sortByClassCode(1);

        for(i = dLen -1; i >= 0; i--) {
            if(Code1.get(i) < 9) break;
        }

        dLen = i; // usable date
        x1   = xPos.get(0);
        y1   = yPos.get(0);
        nbr1 = 0;

        int dx, dy;

        for(i = 1; i < dLen; i++) {
            dx = Math.abs(x1 - xPos.get(i));
            dy = Math.abs(y1 - yPos.get(i));
            if(dx + dy > dist) {
                dist = dx + dy;
                idx2 = i;
            }
        }

        if(idx2 > 0) {
            x2   = xPos.get(idx2);
            y2   = yPos.get(idx2);
            nbr2 = idx2;
        }

        int[] rtn1 = new int []{x1, y1, x2, y2, nbr1, nbr2, dist};
        return rtn1;
    }

    public int[] findHeadLocationByNextFrame(laserHit hitN) {
        //  find the hit center of the next frame and determine the farthest point
        //  of the hit region in the current frame

        int[]  centerN;
        int[]  centerF;
        int       dLen = xPos.size();

        if(dLen < 8 || hitN.xPos.size() < 8) {
            // too few points; just get the center of the hit in the first frame
            centerF = this.findAvgXYPlus(5000, true);

            Log.d(TAG, " findHeadLocationByNextFrame() - hit size(s) too small, use center #1!");

            return centerF;
        }

        centerN = hitN.findAvgXYPlus(5000, true);

        if(centerN[0] < 1 || centerN[1] < 1) {
            // Can't find the hit center of Frame 2;
            // just use the center of the hit in the first frame
            centerF = this.findAvgXYPlus(5000, true);

            Log.d(TAG, " findHeadLocationByNextFrame() - can't find center #2, use center #1!");
            return centerF;
        } else {
            setNBRCount();
            sortByClassCode(-1); // Code1 (Nbr count) in descending order

            int       dist = 0;
            int      cSize = 0;  // core pop size
            int     cNBidx = 8;
            int         x2 = centerN[0], y2 = centerN[1];
            int[]  NBhisto = new int[9];

            centerF = new int[] {-1, -1};

            for(int i = 0; i < dLen; i++) {
                if(Code1.get(i) > 0)  NBhisto[Code1.get(i)]++;
            }

            Log.d(TAG, " findHeadLocationByNextFrame() - Nbr Histo (8 - 1): ");
            Log.d(TAG, "    -------- "+NBhisto[8]+", "+NBhisto[7]+", "+NBhisto[6]+", "+NBhisto[5]+", "+NBhisto[4]+", "+NBhisto[3]+", "
                    +NBhisto[2]+", "+NBhisto[1]);


            if(NBhisto[8] >= 20) { // meaningful case
                Log.d(TAG, " findHeadLocationByNextFrame() going for real end point....");

                for (int i = 0; i < dLen; i++) {
                    if (Code1.get(i) < 7 && centerF[0] > 0) {
                        Log.d(TAG, " findHeadLocationByNextFrame() found real end point after [ "+i+" ] samples.");
                        break; // good points used up
                    }

                    int x1 = xPos.get(i);
                    int y1 = yPos.get(i);
                    int dist2 = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);

                    if (dist2 > dist) {
                        dist = dist2;
                        centerF[0] = x1;
                        centerF[1] = y1;
                    }
                }
            } else { // Center not well defined - didn't work out
                Log.d(TAG, " findHeadLocationByNextFrame() going for avg point.");

                centerF = this.findAvgXYPlus(5000, true);
            }

            return centerF;
        }

    }

    public native double    GetIntenseWeight(int i1, int i2, int i3);
}
