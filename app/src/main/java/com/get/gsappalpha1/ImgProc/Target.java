package com.get.gsappalpha1.ImgProc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Rob on 7/14/2017.
 */

public class Target {
    private int     TargetID;
    private int     TargetModel;
    private int     TdimX;
    private int     TdimY;
    public  String  TargetName;
    public  String  model;
    public  String  Device;

    private GrayProc mGrayProc;

//    public

    public static String[] TargetNames = {
            "Birchwood Casey Shoot-N-C 6in",
            "Birchwood Casey Shoot-N-C 8in",
            "Birchwood Casey Shoot-N-C 3in",
            "GlowShot 6in Multicolor Splatter",
            "GlowShot 6in Multicolor Splatter Adhesive",
            "GlowShot 6in Orange Adhesive",
            "GlowShot 6in Regular Adhesive",
            "unknown1",
            "unknown2",
            "unknown3"
    };

    public static double[][] TargetRings = {
            {5.0, 1.0, 2.6, 3.9, 5.8, 9.3},
            {5.0, 1.0, 3.25, 5.2, 7.45, 10.15},
            {4.0, 1.0, 3.99, 9.2, 10.85}
    };    // 1-3.99-9.2-10.85


    public static double[][][] sRanges = {
            {{0.8, 1.2}, {2.3, 2.8}, {2.5, 3.0}, {3.8, 4.6}, {4.15, 4.75}, {5.2, 6.2}, {5.5, 6.5}, {8.6, 10.70}}, // BC 6"
            {{0.8, 1.2}, {2.8, 3.5}, {3.0, 3.8}, {4.4, 5.7}, {4.75, 5.9}, {6.3, 7.95}, {7.1, 8.3}, {9.05, 11.25}}, // BC 8"
            {{0.8, 1.2}, {3.5, 4.3}, {3.6, 4.6}, {7.8, 10.6}, {8.1, 10.98}, {9.1, 15.9}, {-100.0, -100.0}, {-100.0, -100.0}}
            }; // BC 3"

    int[]     TargetBBox = { 9999999, 9999999, 0, 0 }; // {x0, y0, x1, y1}
    double[]   RingSizes = {-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0}; // 12-long

    public      Activity   mActivity;
    private          int   XBE, YBE;

    private static final int     G_minBERingSize = 8;
    private static final String              TAG = "Target";


    public int[][] setSearchRanges (int cRadius, int[] range, int dimx, int dimy) {
        int[] xPos0 = {-1, -1, -1, -1, -1, -1, -1, -1};
        int[] xPos1 = {-1, -1, -1, -1, -1, -1, -1, -1};
        int[] yPos0 = {-1, -1, -1, -1, -1, -1, -1, -1};
        int[] yPos1 = {-1, -1, -1, -1, -1, -1, -1, -1};

        if (cRadius > 8 && range.length == 4) {

            int          midPt = (range[0] + range[1] + 1)/2;
            double     bRadius = Math.floor(midPt * cRadius);
            double  bRadius_45 = Math.floor(bRadius * 0.707);
            double        sprd = 1 + Math.max(0, Math.floor(0.5 * (range[1] - range[0]) * cRadius));
            double     sprd_45 = Math.max(1, Math.floor(sprd * 0.707));
            int          dimxm = dimx - 1, dimym = dimy - 1;

            int[] xPos = {XBE,
                         (int)(XBE + bRadius_45),
                         (int)(XBE + bRadius),
                         (int)(XBE + bRadius_45),
                         XBE,
                         (int)(XBE - bRadius_45),
                         (int)(XBE - bRadius),
                         (int)(XBE - bRadius_45)};
            int[] yPos = {
                         (int)(YBE - bRadius),
                         (int)(YBE - bRadius_45),
                         YBE,
                         (int)(YBE + bRadius_45),
                         (int)(YBE + bRadius),
                         (int)(YBE + bRadius_45),
                         YBE,
                         (int)(YBE - bRadius_45)};

            xPos0[0] = xPos[0];
            xPos0[1] = (int)(xPos[1] - sprd_45);
            xPos0[2] = (int)(xPos[2] - sprd);
            xPos0[3] = (int)(xPos[3] - sprd_45);
            xPos0[4] = xPos[4];
            xPos0[5] = (int)(xPos[5] - sprd_45);
            xPos0[6] = (int)(xPos[6] - sprd);
            xPos0[7] = (int)(xPos[7] - sprd_45); // X always from low to high

            xPos1[0] = xPos[0];
            xPos1[1] = (int)(xPos[1] + sprd_45);
            xPos1[2] = (int)(xPos[2] + sprd);
            xPos1[3] = (int)(xPos[3] + sprd_45);
            xPos1[4] = xPos[4];
            xPos1[5] = (int)(xPos[5] + sprd_45);
            xPos1[6] = (int)(xPos[6] + sprd);
            xPos1[7] = (int)(xPos[7] + sprd_45);

            yPos0[0] = (int)(yPos[0] - sprd);
            yPos0[1] = (int)(yPos[1] + sprd_45);
            yPos0[2] = yPos[2];
            yPos0[3] = (int)(yPos[3] - sprd_45);
            yPos0[4] = (int)(yPos[4] - sprd);
            yPos0[5] = (int)(yPos[5] + sprd_45);
            yPos0[6] = yPos[6];
            yPos0[7] = (int)(yPos[7] - sprd_45); // Y goes with X

            yPos1[0] = (int)(yPos[0] + sprd);
            yPos1[1] = (int)(yPos[1] - sprd_45);
            yPos1[2] = yPos[2];
            yPos1[3] = (int)(yPos[3] + sprd_45);
            yPos1[4] = (int)(yPos[4] + sprd);
            yPos1[5] = (int)(yPos[5] - sprd_45);
            yPos1[6] = yPos[6];
            yPos1[7] = (int)(yPos[7] + sprd_45);

            for (int i = 0; i < 8; i++) {
                if (xPos0[i] < 1) xPos0[i] = 1;
                else if (xPos0[i] > dimxm) xPos0[i] = dimxm;

                if (yPos0[i] < 1) yPos0[i] = 1;
                else if (yPos0[i] > dimxm) yPos0[i] = dimxm;

                if (xPos1[i] < 1) xPos1[i] = 1;
                else if (xPos1[i] > dimxm) xPos1[i] = dimxm;

                if (yPos1[i] < 1) yPos1[i] = 1;
                else if (yPos1[i] > dimxm) yPos1[i] = dimxm;
            }
        }
        int[][] rPos = {xPos0, xPos1, yPos0, yPos1};
        return rPos;
    }


    public boolean roundUpTargetRings (int TargetModel, int[] oid, int maxRSize, int dimx, int dimy, int[][] bMap, int[][] gMap, TRing[] tRings, int[] data_u32, int[][][] sRanges) {
        //////////////
        /// to complete the Target Ring Profile from the existing partial finding
        ///
        /// 1. estimate the true target size and determine the rough locations of the missing rings
        /// 2. determine the orientation of the markings (for avoidance)
        /// 3. draw crosssection lines from the center to lcoate the missing rings and finish tracing

        // $scope.TargetModel = 0; // Exp values: BC 6"; 1-2.7-4.3-5.9-9.6; 1 - BC 8"; 1-3.25-5.2-7.45-10.15
        int rnCnt = 8;
        int ri = 8, i, j, k;
        double bRadius = Math.floor(0.5 * (tRings[0].diameter + 0.5));
        double bRadius_45;
        int[][]    sRange = sRanges[0];
        if (TargetModel != 0) { sRange = sRanges[TargetModel]; }
        //          if ($scope.TargetModel == 2) { rnCnt = 6; }

        if (tRings[0].diameter < 1) {
            Log.d(TAG, "!!  roundUpTargetRings(): Center Ring information missing - roundUpTargetRings() quitted !!");
            return false;
        }

        int     px, py, pxe, pye;
        int     xSign, ySign;
        tTrace  epntT = new tTrace();
        int[]   tPos = new int[2];
        boolean traced;
        int     rim;
        double  cRadius = Math.floor(0.5 * (tRings[0].diameter + 0.5));
        int     rnLimit = rnCnt - 1;

        ri = rnCnt;
        while (ri > 0) {
            rim = ri - 1;
            if (tRings[rim].centerX > 1 && tRings[rim].centerY > 1 && tRings[rim].diameter > 1) { ri--; continue; }

            int[][] rangeSet = setSearchRanges((int)cRadius, sRange[rim], dimx, dimy);

            int[] xPos0 = rangeSet[0];   // rangeSet.xP0;
            int[] xPos1 = rangeSet[1];   // rangeSet.xP1;
            int[] yPos0 = rangeSet[2];   // rangeSet.yP0;
            int[] yPos1 = rangeSet[3];   // rangeSet.yP1;

            if (xPos0[0] > 0 && xPos1[0] > 0 && yPos0[0] > 0 && yPos1[0] > 0) {
                for (k = 0; k <= rnLimit; k++) {

                    px     = xPos0[k];
                    py     = yPos0[k];
                    pxe    = Math.min(xPos1[k], dimx - 1);
                    pye    = Math.min(yPos1[k], dimy - 1);
                    if   (pxe == px) xSign = 0;
                    else xSign  = (pxe > px)? 1 : -1;

                    if   (pye == py) ySign  = 0;
                    else ySign  = (pye > py)? 1 : -1;

                    epntT  = null;
                    traced = false;
                    int    count = 0;
                    while (px <= pxe && py != pye && count < 60) {
                        if (gMap[pye][pxe] == 0) { // found an Edge Point - start Contour Tracing
                            tPos[0] = pye;
                            tPos[1] = pxe;
                            int traceDir = 1;

//                            Log.d(TAG, " >> roundUpTargetRings() - RingIndex = " + ri + " | Direction Index = " + k + " | Tracing Point = " + tPos.join(", "));

                            epntT = mGrayProc.traceEdge(oid, gMap, tPos, dimx, dimy, ri, traceDir);  // returns edge contour 'ri' from tPos


                            if(epntT == null) continue;
                            //// traceEdge (int[] id, int[][] gMap, int[] sPos, int dimX, int dimY, int mark, int direction) <GrayProc.java>

                            if (epntT.tAry.length > 10) {
                                TCircle tCircle = new TCircle();

                                tCircle = tCircle.getCircleProperty(epntT, ri - 1, ri, bMap, dimx, dimy, gMap);

                                int  aryLen = epntT.tAry.length;
                                if (Math.abs(tCircle.centerX - XBE) < 10 && Math.abs(tCircle.centerY - YBE) < 10 && tCircle.diameter > 1) { // ring's good
                                    ////// Record the ring and mark the detected contours

                                    tRings[ri - 1].trcID       = tCircle.trcID;
                                    tRings[ri - 1].centerX     = tCircle.centerX;
                                    tRings[ri - 1].centerY     = tCircle.centerX;
                                    tRings[ri - 1].type        = tCircle.type;
                                    tRings[ri - 1].diameter    = -1;
                                    tRings[ri - 1].Center      = null;
                                    tRings[ri - 1].prof        = tCircle.prof;
                                    tRings[ri - 1].aspectRat   = tCircle.aspectRat;


//                                    int[] tcData = epntT.tAry;
                                    int   alpha = 0xff << 24;
                                    int   intens1 = Math.min(255, 255 - ri * 30);
                                    int   intens2 = Math.min(255, 30 * ri);
                                    int   intens3 = Math.min(255, 128 + ri * 15);
                                    int   iPix    = alpha | (intens3 << 16) | (intens2 << 8) | intens1;
                                    for (int ti = 0; ti < aryLen; ti++) {
                                        j = epntT.tAry[ti][0] * dimx + epntT.tAry[ti][1];
                                        data_u32[j] = iPix;
                                    }
                                    traced = true;
                                    break;
                                }
                            }
                        }
                        pxe -= xSign;
                        pye -= ySign;
                        count++;
                    }
                    if (traced) break;
                }
            }
            ri--;
        } // while()

        // sort tRing and look for missing rings
        //	      console.log(" roundUpTargetRings() - <Unsorted> tRings[] before final completion:");
        //	      $scope.logRings(tRings);

        TRing[] sRings = sortRingsByName(tRings, rnCnt);

        Log.d(TAG, " roundUpTargetRings() - <Sorted> tRings[] before final completion:");
//        logRings(sRings);

        //////// Check if more rings are needed; if not, save and exit.
        for (ri = 0; ri < rnCnt; ri++) {
            TRing sCircle = sRings[ri];
            tRings[ri]    = sCircle;

            if (tRings[ri].centerX < 0 || tRings[ri].centerY < 0 || tRings[ri].diameter < 0) {
                // complete the ring building as needed
                int[][] rangeSet = setSearchRanges((int)cRadius, sRange[ri], dimx, dimy);

                int[] xPos0 = rangeSet[0];
                int[] xPos1 = rangeSet[1];
                int[] yPos0 = rangeSet[2];
                int[] yPos1 = rangeSet[3];

                Log.d(TAG, "   roundUpTargetRings() - making up missing ring " + ri + " | range: (" + xPos0 + ", " + yPos0 + ") (" + xPos1 + ", " + yPos1 + ")");

                if (xPos0[0] > 0 && xPos1[0] > 0 && yPos0[0] > 0 && yPos1[0] > 0) {
                    for (k = rnLimit; k >= 0; --k) {

                        Log.d(TAG, "    roundUpTargetRings() - making up missing ring in direction " + k);

                        px     = xPos0[k];
                        py     = yPos0[k];
                        pxe    = Math.min(xPos1[k], dimx - 1);
                        pye    = Math.min(yPos1[k], dimy - 1);

                        if   (pxe == px) xSign = 0;
                        else xSign  = (pxe > px)? 1 : -1;

                        if   (pye == py) ySign  = 0;
                        else ySign  = (pye > py)? 1 : -1;

                        epntT  = new tTrace();
                        traced = false;
                        int count = 0;
                        while (px <= pxe && py != pye && count < 60) {
                            if (gMap[pye][pxe] == 0) { // found an Edge Point - start Contour Tracing
                                tPos[0] = pye;
                                tPos[1] = pxe;
                                int traceDir = 1;

//                                Log.d("  >>> roundUpTargetRings() makeup process for RingIndex = " + (ri + 1) + " | Direction Index = " + k + " | Tracing Point = " + tPos.join(", "));

                                epntT = mGrayProc.traceEdge(oid, gMap, tPos, dimx, dimy, ri + 1, traceDir);  // returns edge contour 'ri' from tPos

                                if(epntT == null) continue;
                                //// traceEdge (int[] id, int[][] gMap, int[] sPos, int dimX, int dimY, int mark, int direction) <GrayProc.java>

                                int  aryLen = epntT.tAry.length;

                                if (aryLen > 10) {
                                    TCircle tCircle = new TCircle();

                                    tCircle = tCircle.getCircleProperty(epntT, ri - 1, ri, bMap, dimx, dimy, gMap);

                                    if (Math.abs(tCircle.centerX - XBE) < 10 && Math.abs(tCircle.centerY - YBE) < 10 && tCircle.diameter > 1) { // ring's good
                                        ////// Record the ring and mark the detected contours
                                        tRings[ri].trcID       = tCircle.trcID;
                                        tRings[ri].centerX     = tCircle.centerX;
                                        tRings[ri].centerY     = tCircle.centerX;
                                        tRings[ri].type        = tCircle.type;
                                        tRings[ri].diameter    = -1;
                                        tRings[ri].Center      = null;
                                        tRings[ri].prof        = tCircle.prof;
                                        tRings[ri].aspectRat   = tCircle.aspectRat;


//                                        int[][] tcData = epntT.tAry;
                                        int alpha = 0xff << 24;
                                        int intens1 = Math.min(255, 255 - ri * 30);
                                        int intens2 = Math.min(255, 30 * ri);
                                        int intens3 = Math.min(255, 128 + ri * 15);
                                        int    iPix = alpha | (intens3 << 16) | (intens2 << 8) | intens1;
                                        for (int ti = 0; ti < aryLen; ti++) {
                                            j = epntT.tAry[ti][0] * dimx + epntT.tAry[ti][1];
                                            data_u32[j] = iPix;
                                        }
                                        traced = true;
                                        break;
                                    }
                                }
                            }
                            pxe -= xSign;
                            pye -= ySign;
                            count++;
                        }
                        if (traced) break;
                    }
                }
            }
        }

        Log.d(TAG, " roundUpTargetRings() - tRings[] after final completion:");
//        $scope.logRings(tRings);
        return true;
    }

    public TRing[] sortRingsByName (TRing[] tRings, int limit) {
        // To sort the tRing set 'tRings' by Target Name and mark the missing rings
        //
        // We assume that the Base Ring is already put at tRings[0]
        //

        TRing[] bRings = tRings;

        if (limit < 2 || tRings.length > 0) { // nothing to do
            Log.d(TAG, "!! sortRingsByName() quited - poor input data. !!");
            return bRings;
        } else if (TargetModel < 0 || TargetModel > 5) {
            Log.d(TAG, "! sortRingsByName() quited - unknown target model " + TargetModel + "!");
            return bRings;
        } else if (limit > tRings.length) {
            limit = tRings.length;
        }

        int     beX = -1, beY = -1, indexBE = 0;
        double  bDiam = -1.0;

        if (tRings[0].diameter < G_minBERingSize || tRings[0].type >= 0) {
            Log.d(TAG, "!! $scope.sortRings() quitted - center not set. !!");
            return bRings;
        } else {
            beX   = tRings[0].centerX;
            beY   = tRings[0].centerY;
            bDiam = tRings[0].diameter;
        }

        int       i, j, k, limitm = limit - 1;
        TRing[]   wRings = new TRing[limit];
        double[]  rDiams = new double[limit];

        for (j = 0; j < limit; j++) { wRings[j] = new TRing(); rDiams[j] = tRings[j].diameter; }

        // sort all rings based on size
        int [] rDiams_sortIndices = sortWithIndeces(rDiams);

        boolean found = false;
        for (i = 0; i < limit; i++) { // locate the Base Ring in the sorted array
            if (rDiams_sortIndices[i] == 0) {
                indexBE = i;
                found = true; break;
            }
        }

        if (!found) {
            Log.d(TAG, "!!! $scope.sortRings() quitted - process failed. !!!");
            return bRings;
        }

        // push up the good rings to the front
        int    sIdx = 0;
        TRing   tri = tRings[sIdx];
        wRings[0]   = tri;

        j = 1;
        for (k = indexBE + 1; k < limit; k++) {
            sIdx = rDiams_sortIndices[k];
            TRing   trk = tRings[sIdx];
            wRings[j++] = trk;
        } // remainder wRings[] are dummies

        Log.d(TAG, " >> $scope.sortRings() initial wRings:");
//        $scope.logRings(wRings);

        // identify and fill in the missing rings using sRanges[];
        double[][] ranges = sRanges[TargetModel];
        double       rate = 1.0;
        int          sign = 1;

        for (i = 1; i < limit - 1; i++) {
            int di = (int)wRings[i].diameter;
            if (di < 22) break; // done
            rate = di / bDiam;
            if (rate > ranges[i][1] || wRings[i].type != sign) {
                // push down to the right spot and replace the current one with a dummy

                for (k = limit - 1; k > i; --k) {
                    TRing   trk = wRings[k - 1];
                    wRings[k]   = trk;
                }
                wRings[i] = new TRing();
            }
            sign *= -1;
        }

        return wRings;
    }


    public int[] sortWithIndeces(double[] inAry) {
        // function to sort the array "toSortAry" and return the sorted index array.
        //
        //  toSortAry      - original array to be sorted after return
        //  returned array - the original index of the elements in the sorted array

        if(inAry == null || inAry.length < 2) return null;

        int       AryLen = inAry.length;
        int      AryLenm = AryLen - 1;
        int[] SortedIndx = new int[AryLen];
        int   i, j;

        for (i = 0; i < AryLen; i++) {
            SortedIndx[i] = i;
        }

        for(i=0; i<AryLenm; i++) {
            double  ta = inAry[i];
            for(j=i+1; j<AryLen; j++) {
                if(ta > inAry[j]) {
                    ta            = inAry[j];
                    inAry[j]      = inAry[i];
                    inAry[i]      = ta;
                    SortedIndx[i] = j;
                }
            }
        }

        return SortedIndx;
    }


    public int countCenterCorners(keypoint[] corners, int dimx, int dimy) {
        if (corners.length < 1 || dimx < 10 || dimy < 10) { return -1; }

        int   dimx4 = dimx >> 2, dimy4 = dimy >> 2;
        int  dimx4e = dimx - dimx4, dimy4e = dimy - dimy4;
        int     cnt = 0;
        keypoint tCorner;

        for (int i = 0; i < corners.length; i++) {
            tCorner = corners[i];
            if (tCorner.x <= dimx4e && tCorner.x >= dimx4 && tCorner.y <= dimy4e && tCorner.y >= dimy4) cnt++;
        }
        return cnt;
    }

    public boolean saveTargetTemplate (TRing[] Rings, boolean toFile) {
        // store template's rings into Local Storage

        SharedPreferences         preferences = mActivity.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor       editor = preferences.edit();

        if (Rings.length < 1) {
            Log.d(TAG, "!! Saving Target Template failed - invalid data length. !!");
            return false;
        }


//        KBConnected = preferences.getBoolean("keyboardconnected", false);

        int nTmpl = preferences.getInt("TemplateCount", -1);

        if (nTmpl < 0) { // first time access
            nTmpl = 0;
            editor.putInt("TemplateCount", 0); // Lockr.set('TemplateCount', 0); // total templates saved
            editor.commit();



//            Lockr.set('orgDevice', []); // Originated device model
            editor.putString("templateName", " ");    // Lockr.set('templateName', []); // Standard Name of the templete
            editor.commit();
            editor.putString("templateModel", " ");   // Lockr.set('templateModel', []); // Standard Model of the template
            editor.commit();
            editor.putString("templateDate", " ");    // Lockr.set('templateDate', []); // Original saving date
            editor.commit();
            editor.putInt("templateSize", -1);        // Lockr.set('templateSize', []); // 6" or 8"
            editor.commit();
            editor.putString("templateColor", " ");   // Lockr.set('templateColor', []); // Background Color
            editor.commit();
            editor.putString("templateBEColor", " "); // Lockr.set('templateBEColor', []); // Bull'e Eye Color
            editor.commit();
            editor.putInt("templateXDim", -1);        // Lockr.set('templateXDim', []);  // Target x-dimension in Pix
            editor.commit();
            editor.putInt("templateYDim", -1);        // Lockr.set('templateYDim', []);  // Target y-dimension in Pix
            editor.commit();
            editor.putInt("ringCount", -1);           // Lockr.set('ringCount', []);
            editor.commit();
            editor.putInt("ring0", -1);               // Lockr.set('ring0', []);
            editor.commit();
            editor.putInt("ring1", -1);           // Lockr.set('ring1', []);
            editor.commit();
            editor.putInt("ring2", -1);           // Lockr.set('ring2', []);
            editor.commit();
            editor.putInt("ring3", -1);           // Lockr.set('ring3', []);
            editor.commit();
            editor.putInt("ring4", -1);           // Lockr.set('ring4', []);
            editor.commit();
            editor.putInt("ring5", -1);           // Lockr.set('ring5', []);
            editor.commit();
            editor.putInt("ring6", -1);           // Lockr.set('ring6', []);
            editor.commit();
            editor.putInt("ring7", -1);           // Lockr.set('ring7', []);
            editor.commit();
            editor.putInt("ring8", -1);           // Lockr.set('ring8', []);
            editor.commit();
            editor.putInt("ring9", -1);           // Lockr.set('ring9', []);
            editor.commit();
            editor.putInt("ring10", -1);           // Lockr.set('ring10', []);
            editor.commit();
            editor.putInt("ring11", -1);           // Lockr.set('ring11', []);
            editor.commit();
            editor.putInt("ring12", -1);           // Lockr.set('ring12', []);
            editor.commit();
            editor.putInt("ring13", -1);           // Lockr.set('ring13', []);
            editor.commit();
            editor.putInt("ring14", -1);           // Lockr.set('ring14', []);
            editor.commit();
            editor.putInt("rProf0", -1);           // Lockr.set('rProf0', []);
            editor.commit();
            editor.putInt("rProf1", -1);           // Lockr.set('rProf1', []);
            editor.commit();
            editor.putInt("rProf2", -1);           // Lockr.set('rProf2', []);
            editor.commit();
            editor.putInt("rProf3", -1);           // Lockr.set('rProf3', []);
            editor.commit();
            editor.putInt("rProf4", -1);           // Lockr.set('rProf4', []);
            editor.commit();
            editor.putInt("rProf5", -1);           // Lockr.set('rProf5', []);
            editor.commit();
            editor.putInt("rProf6", -1);           // Lockr.set('rProf6', []);
            editor.commit();
            editor.putInt("rProf7", 0);            // Lockr.set('rProf7', []);
            editor.commit();
        }

        // adding new parameters
        String       dat1 = "Iron_0" + Integer.toString(nTmpl);
        String       dat2 = TargetName;
        String       datD = Device;
        String  timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        int    mIndx = TargetModel;
        if (mIndx > 2) { // ringCount may be different
            Log.d(TAG, "ringCount not set for this Target Type!");
        }
        int  dat4 = 6; // 6" deafult
        int  rCnt = 5;
        if (mIndx == 1) {
            dat4 = 8;
        } else if (mIndx == 2) {
            dat4 = 3;
            rCnt = 4;
        }
        String dat5 = "black";
        String dat6 = "red";

        editor.putString("templateName", dat1);        // Lockr.sadd('templateName', dat1);
        editor.commit();
        editor.putString("orgDevice", datD);           // Lockr.sadd('orgDevice', datD);
        editor.commit();
        editor.putString("templateModel", dat1);       // Lockr.sadd('templateModel', dat2);
        editor.commit();
        editor.putString("templateDate", timeStamp);   // Lockr.sadd('templateDate', dat3);
        editor.commit();
        editor.putInt("templateSize", dat4);           // Lockr.sadd('templateSize', dat4);
        editor.commit();
        editor.putString("templateColor", dat5);       // Lockr.sadd('templateColor', dat5);
        editor.commit();
        editor.putString("templateBEColor", dat6);     // Lockr.sadd('templateBEColor', dat6);
        editor.commit();
        editor.putInt("templateXDim", TdimX);          // Lockr.sadd('templateXDim', TdimX);
        editor.commit();
        editor.putInt("templateYDim", TdimY);          // Lockr.sadd('templateYDim', $scope.dimY);
        editor.commit();

        double[] t1 = TargetRings[mIndx];
        editor.putInt("ringCount", rCnt);   // Lockr.sadd('ringCount', rCnt);
        ///////////// Need to check t1 validity before proceeding.....
        if (mIndx >= 0 && t1.length > 0) {
            int   t2 = (int)t1[0];
            float fdat1 = (float)t1[1];
            float fdat2 = (float)t1[2];
            editor.putFloat("ring0", fdat1);          // Lockr.sadd('ring0', dat1);
            editor.commit();
            editor.putFloat("ring1", fdat2);          // Lockr.sadd('ring1', dat2);
            editor.commit();
            if (t2 > 2) {
                float fdat3 = (float)t1[3];
                float fdat4 = (float)t1[4];
                editor.putFloat("ring2", fdat3);          // Lockr.sadd('ring2', dat3);
                editor.commit();
                editor.putFloat("ring3", fdat4);          // Lockr.sadd('ring3', dat4);
                editor.commit();
                if (t2 > 4) {
                    float fdat5 = (float)t1[5];
                    editor.putFloat("ring4", fdat5);          // Lockr.sadd('ring4', dat5);
                    editor.commit();
                    if (t2 > 5) {
                        float fdat6 = (float)t1[6];
                        float fdat7 = (float)t1[7];
                        float fdat8 = (float)t1[8];
                        editor.putFloat("ring5", fdat6);   // Lockr.sadd('ring5', dat6);
                        editor.commit();
                        editor.putFloat("ring6", fdat7);   // Lockr.sadd('ring6', dat7);
                        editor.commit();
                        editor.putFloat("ring7", fdat8);   // Lockr.sadd('ring7', dat8);
                        editor.commit();
                    }
                }
            }

            int[] tp = {0, 0, 0, 0, 0, 0, 0, 0};
            ///// Need to record the transition profiles from the given (Rings)
            if (Rings.length > 0) { // record the transition profiles
                for (int i = 0; i < Rings.length; i++) {
                    for (int j = 0; j < 8; j++) {
                        tp[i] += Rings[i].prof[j];
                    }
                    tp[i] = (tp[i] / 8);
                }
            }
            editor.putInt("rProf0", tp[0]);   // Lockr.sadd('rProf0', tp[0]);
            editor.commit();
            editor.putInt("rProf1", tp[1]);   // Lockr.sadd('rProf1', tp[1]);
            editor.commit();
            editor.putInt("rProf2", tp[2]);   // Lockr.sadd('rProf2', tp[2]);
            editor.commit();
            editor.putInt("rProf3", tp[3]);   // Lockr.sadd('rProf3', tp[3]);
            editor.commit();
            editor.putInt("rProf4", tp[4]);   // Lockr.sadd('rProf4', tp[4]);
            editor.commit();
            editor.putInt("rProf5", tp[5]);   // Lockr.sadd('rProf5', tp[5]);
            editor.commit();
            editor.putInt("rProf6", tp[6]);   // Lockr.sadd('rProf6', tp[6]);
            editor.commit();
            editor.putInt("rProf7", tp[7]);   // Lockr.sadd('rProf7', tp[7]);
            editor.commit();

            nTmpl++;
            editor.putInt("TemplateCount", nTmpl);   // Lockr.set('TemplateCount', nTmpl);
            editor.commit();
        }
        if (toFile) { // want to save template into a file
////            setupStage = 2;
        } else { // clear up screen and reset
////            setupStage = 0;
        }
        return true;
    }

    public void logRings(TRing[] sRings) {

        if (sRings.length < 1) {
            return;
        }

        int  len = sRings.length;

        Log.d(TAG, "  sRing Dump:");
        for (int j = 0; j < len; j++) {
            TRing  ring = sRings[j];
            SpannableStringBuilder builder = new SpannableStringBuilder();

            builder.append("   -- Ring ").append(j+": ").append(ring.centerX+", ").append(ring.centerY+", ");
            builder.append(ring.diameter+", ").append(ring.type+", ").append(ring.concentric+" | ");
            for(int k=0; k<ring.prof.length; k++) {
                builder.append(ring.prof[k] +", ");
            }
            Log.d(TAG, "     "+builder);
        }
    }

}
