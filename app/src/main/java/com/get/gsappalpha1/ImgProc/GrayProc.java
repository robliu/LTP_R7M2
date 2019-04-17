package com.get.gsappalpha1.ImgProc;


/**
 *  ===== This work is copyrighted! Do not copy or reuse without Guidance permission! ======
 *
 *    For detail, contact www.guidance-edu.com
 *
 *
 *     Created by Rob on 3/8/2017.
 *
 *  Copy Rights 2017, Guidance Education Technologies, All Rights Reserved.
 *
 */

import android.util.Log;

import java.util.Arrays;

public class GrayProc {
//    private Blob mblob;

    private long startTime;
    private long endTime;
    private int blobrun;
    private boolean inProg;
    public int[] iAry;
    public byte[] bAry;

    private static final int G_MinBRinglevel = 400;
    private static final int BlobRunLimit = 30000;
    private static final int G_MAXTRACELEN = 6000;
    private static final int MAXPATHS = 600;
    private static final String TAG = "GPc<>";

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

    public static double[][] TargetRingSizes = {
            {5.0, 1.0, 2.6, 3.9, 5.8, 9.3},     // BC 6"
            {5.0, 1.0, 3.25, 5.2, 7.45, 10.15},   // BC 8"
            {4.0, 1.0, 3.99, 9.2, 10.85}           // BC 3"
    };


    public static double[][][] sRanges = {
            {{0.8, 1.2}, {2.3, 2.8}, {2.5, 3.0}, {3.8, 4.6}, {4.15, 4.75}, {5.2, 6.2}, {5.5, 6.5}, {8.6, 10.70}}, // BC 6"
//            {{0.8, 1.2}, {2.8, 3.7}, {3.0, 3.9}, {4.4, 5.7}, {4.75, 5.9}, {6.3, 7.95}, {7.1, 8.3}, {9.05, 11.25}}, // BC 8"
            {{0.8, 1.2}, {2.1, 2.6}, {2.2, 3.2}, {3.8, 5.0}, {3.9, 5.0}, {6.3, 7.95}, {7.1, 8.3}, {9.05, 11.25}}, // BC 8"
            {{0.8, 1.2}, {3.5, 4.3}, {3.6, 4.6}, {7.8, 10.6}, {8.1, 10.98}, {9.1, 15.9}, {-100.0, -100.0}, {-100.0, -100.0}}  // BC 3"
    };


    public GrayProc() {
//        mblob = null;
        inProg = false;
        iAry = null;
        bAry = null;
    }


    public boolean inProg() {
        return inProg;
    }

    public void start() {
        inProg = true;
    }

    public void stop() {
        inProg = false;
    }

    public void reset() {
//        mblob = null;
        inProg = false;
        iAry = null;
        bAry = null;
    }

    public Blob BlobSeparate(int[] inData, int[] bmap, int w, int h, int Thresh, int minSize, int maxSize, boolean useDark) {
        //    Separate patches in inData and mark them with distinct values using X-/Y- grow-and-merge method. The
        //  objective is to return the center-most blob.
        //
        //  INPUT:
        //   inData  -  GrayScale original patch map to be analyzed on; will be modified to remove all non-center blobs
        //
        //   bmap    -  resulting blobs map (typically set up to be the same as mGrayProc.iAry)
        //
        //   thresh  -  Threshold for determining transitions between blobs and the background
        //
        //   minSize -  minimum pixel-count size for care patches (blob); used to remove noises (don't-care patches).
        //              This value is typically set to match the size of the Center BE. Therefore equals 2*PI*r_BE .
        //
        //   useDark -  true:  blobs identified based on intensity below Thresh
        //              false: blobs identified based on intensity above Thresh
        //
        //  RETURN:
        //     Primary -   Blob  - the center-most blob identified
        //
        //=================================================================================================
        Log.d(TAG, "BlobSeparate() entered...");

        Blob mblob = new Blob();
        mblob.setQuality(-999);

        if (inData == null || inData.length < 100) {
            Log.d(TAG, "BlobSeparate() aborted due to invalid raw data!");
            return mblob;
        }
        if (w < 6 || h < 6 || w * h != inData.length) {
            Log.d(TAG, "BlobSeparate() aborted due to bad data: size / w / h = " + inData.length + " / " + w + " / " + h);
            return mblob;
        }

        final int MaxLabel = 2001;

        int     len = inData.length;
        int    indx = 0, mcnt = 0;
        int    Mark = MaxLabel;
        int     hq1 = h / 4;
        int     hq3 = (h * 3) / 4;
        int     wq1 = w / 4;
        int     wq3 = (w * 3) / 4;
        boolean  on = false;

        Log.d(TAG, "BlobSeparate() in data: size / w / h / useDark / Thresh / minSize / maxSize = " + len + " / " + w + " / " + h + " / " +
                useDark + " / " + Thresh + " / " + minSize + " / " + maxSize);

        for (int i = 0; i < len; i++) iAry[i] = 0;

        if (useDark) { // analyze the low intensity patches
            // First round for labeling in each row
            for (int y = 0; y < h; y++) {
                if (on) { // force to reset if last row was live at the end
                    on = false;
                    mcnt = 0;
                    Mark = Mark - 1;
                    if (Mark < 1) Mark = MaxLabel; // recycle the label as needed
                }
                for (int x = 0; x < w; x++, indx++) {
                    if (inData[indx] < Thresh) {
                        // Mark pixel and expand its region
                        iAry[indx] = Mark;
                        on = true;
                        mcnt++;
                    } else {
                        if (on) {
//                            Log.d(TAG, "BlobSeparate() X-final size for Mark <" + Mark + "> = " + mcnt);
                            mcnt = 0;
                            on = false;
                            Mark = Mark - 1;
                            if (Mark < 1) Mark = MaxLabel;
                        }
                    }
                }
            }
        } else { // analyze the high intensity patches
            // First round for labeling in each row
            for (int y = 0; y < h; y++) {
                if (on) { // force to reset if last row was live at the end
                    on = false;
                    mcnt = 0;
                    Mark = Mark - 1;
                    if (Mark < 1) Mark = MaxLabel;
                }
                for (int x = 0; x < w; x++, indx++) {
                    if (inData[indx] > Thresh) {
                        // Mark pixel and expand its region
                        iAry[indx] = Mark;
                        on = true;
                        mcnt++;
                    } else {
                        if (on) {
//                            Log.d(TAG, "BlobSeparate() X-final size for Mark <" + Mark + "> = " + mcnt);
                            mcnt = 0;
                            on = false;
                            Mark = Mark - 1;
                            if (Mark < 1) Mark = MaxLabel;
                        }
                    }
                }
            }
        }

        // Second round for merging in Y- direction
        // - we favor using the greater labels, which should appear in the lower index rows!
        //
        int lenw = len - w;
        for (int i = 0; i < lenw; i++) {
            int j = i + w;
            if (iAry[i] > 0 && iAry[j] > 0) {
                if (iAry[i] != iAry[j]) {
                    Mark = iAry[i];
                    int ste = iAry[j];

                    for (int k = 0; k < len; k++) {
                        if (iAry[k] == ste) iAry[k] = Mark;
                    }
                }
            }
        }
        // Third round to merge in X-direction
        int lenm = len - 1;
        for (int i = 0; i < lenm; i++) {
            int ii = i + 1;
            if (iAry[i] > 0 && iAry[ii] > 0) {
                if (iAry[i] != iAry[ii]) {
                    // relabel bmap[indx+1] labeled pixels to iAry[indx]
                    Mark = iAry[i];
                    int ste = iAry[ii];

                    for (int j = 0; j < len; j++) {
                        if (iAry[j] == ste) iAry[j] = Mark;
                    }
                }
            }
        }

        int[] ss = new int[MaxLabel + 1];
        int bcnt = 0;
        for (int i = 0; i < MaxLabel; i++) ss[i] = 0;

        for (indx = 0; indx < len; indx++) {
            if (iAry[indx] > 0) ss[iAry[indx]]++;
        }

        for (int i = 0; i <= MaxLabel; i++) {
            if (ss[i] > 0) {
                bcnt++;
                Log.d(TAG, "BlobSeparate() final merged blob size( " + i + " ) = " + ss[i]);
            }
        }

        // compress ss[] array down to bSizeIndx[] and bSizes[] arrays of size bcnt!
        int[] bSizeIndx = new int[bcnt];
        int[] bSizes = new int[bcnt];
        int blobCount = 0;

        for (int i = 0; i <= MaxLabel; i++) {
            if (ss[i] >= minSize && ss[i] <= maxSize) {  // if (ss[i] > 0) {
                bSizeIndx[blobCount] = i;
                bSizes[blobCount] = ss[i];
                blobCount++;

                Log.d(TAG, "BlobSeparate() final candidate blob size( " + (blobCount - 1) + " ) = " + ss[i] + ", label = " + i);
            }
        }

        Log.d(TAG, " BlobSeparate() final blob count (blobCount) = " + blobCount);

        Blob[] blobs = new Blob[blobCount];
        for (int i = 0; i < blobCount; i++) {
            blobs[i] = new Blob();
            blobs[i].setQuality(-999);
        }

        //// -- evaluate the quality -- ////
        // factors considered:
        //   - number of small speckles caught; the fewer the better
        //   - closeness of the small speckles; the farther the better
        //   - roundness of the large blobs; at least one should be round
        //   - holes in the large blobs; the fewer the better
        //   - Bulls Eye location; should be in the center of a round blob

        // min blob size is 2000-pix for up to 4 ft camera distance

        int mquality = 0; // this records the "distance-to-frame-border"

        for (int i = 0; i < blobCount; i++) {
            Log.d(TAG, "\n <----> BlobSeparate() ready for evaluating blob[" + i + "]: size = " + bSizes[i] + "\n");

//            if (bSizes[i] < maxSize) {

            blobs[i].Evaluate(bmap, w, h, bSizeIndx[i], bSizeIndx[i]);

            blobs[i].logging();

            if (blobs[i].x < wq1 || blobs[i].x > wq3 || blobs[i].y < hq1 || blobs[i].y > hq3) {
                //  center is not in the expected area!
                Log.d(TAG, "\n <----> BlobSeparate() evaluated blob[" + i + "] center off too far!");
                blobs[i].byteval = -100;
                blobs[i].setQuality(wq1 + hq1);
                continue;
            }

            if ((blobs[i].density / 0.785) > 0.9 && (blobs[i].density / 0.785 < 1.1) &&
                    (blobs[i].aspRat > 0.8) && (blobs[i].aspRat < 1.2) &&
                    blobs[i].perem < (blobs[i].xDim + blobs[i].yDim) * 2
                    ) {
                blobs[i].byteval = 95;
                mquality = Math.abs(blobs[i].x - w / 2) + Math.abs(blobs[i].y - h / 2);
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobSeparate() setting blob[" + i + "] Quality to 95!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.85 && (blobs[i].density / 0.785 < 1.15) &&
                    (blobs[i].aspRat > 0.7) && (blobs[i].aspRat < 1.3) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.1)
                    ) {
                blobs[i].byteval = 95;
                mquality = Math.abs(blobs[i].x - w / 2) + Math.abs(blobs[i].y - h / 2);
                ;
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobSeparate() setting blob[" + i + "]  Quality to 90!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.8 && (blobs[i].density / 0.785 < 1.23) &&
                    (blobs[i].aspRat > 0.6) && (blobs[i].aspRat < 1.4) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.15)
                    ) {
                blobs[i].byteval = 85;
                mquality = Math.abs(blobs[i].x - w / 2) + Math.abs(blobs[i].y - h / 2);
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobSeparate() setting blob[" + i + "]  Quality to 85!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.8 && (blobs[i].density / 0.785 < 1.23) &&
                    (blobs[i].aspRat > 0.6) && (blobs[i].aspRat < 1.4) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.22)
                    ) {
                blobs[i].byteval = 80;

                mquality = Math.abs(blobs[i].x - w / 2) + Math.abs(blobs[i].y - h / 2);
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobSeparate() setting blob[" + i + "]  Quality to 80!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.8 && (blobs[i].density / 0.785 < 1.23) &&
                    (blobs[i].aspRat > 0.6) && (blobs[i].aspRat < 1.4) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.35)
                    ) {
                blobs[i].byteval = 75;

                mquality = Math.abs(blobs[i].x - w / 2) + Math.abs(blobs[i].y - h / 2);
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobSeparate() setting blob[" + i + "]  Quality to 75!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.8 && (blobs[i].density / 0.785 < 1.23) &&
                    (blobs[i].aspRat > 0.6) && (blobs[i].aspRat < 1.4) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.55)
                    ) {
                blobs[i].byteval = 72;

                mquality = Math.abs(blobs[i].x - w / 2) + Math.abs(blobs[i].y - h / 2);
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobSeparate() setting blob[" + i + "]  Quality to 72!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.8 && (blobs[i].density / 0.785 < 1.23) &&
                    (blobs[i].aspRat > 0.6) && (blobs[i].aspRat < 1.4) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.95)
                    ) {
                blobs[i].byteval = 70;

                mquality = Math.abs(blobs[i].x - w / 2) + Math.abs(blobs[i].y - h / 2);
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobSeparate() setting blob[" + i + "]  Quality to 70!");

                // break;
            } else {
                blobs[i].byteval = 95;

                mquality = Math.abs(blobs[i].x - w / 2) + Math.abs(blobs[i].y - h / 2);
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobSeparate() setting blob[" + i + "]  Quality to 55! Label = " + bSizeIndx[i]);
            }
//            }

            Log.d(TAG, " >> BlobSeparate() blob[ " + i + " ] 'quality' = " + blobs[i].quality);
        }

        Log.d(TAG, " >> BlobSeparate() center ordering began...");

        ////  order blobs by their closeness to edge of frame. We then take the last one as the center-most one
        //    to be our BE!
        //
        int   centerN = -1;
        int     cenX = 0;
        int      cenY = 0;
        int minCenOff = 999;

        for (int i = 0; i < blobCount; i++) {
            Log.d(TAG, "  >>> BlobSeparate() validating blob to index #" + i + " of bval = " + blobs[i].byteval + " | off-center = " + blobs[i].quality);
            if (blobs[i].byteval >= 50) {
                if (blobs[i].quality < minCenOff) {

                    minCenOff = blobs[i].quality;
                    cenX = blobs[i].x;
                    cenY = blobs[i].y;
                    centerN = i;

                    Log.d(TAG, "  >>> BlobSeparate() validated primary blob to index #" + i + "; location = " + blobs[i].x + ", " + blobs[i].y
                            + " | dist = " + minCenOff);
                }
            }
        }

        // return the highest quality blob
        if (centerN >= 0) {
            mblob.copy(blobs[centerN]);

            int cLabel = blobs[centerN].Index;
            int  cSize = blobs[centerN].Index;
            Log.d(TAG, "  >>> BlobSeparate() final cenX / centY = " + cenX + " / " + cenY + " of index " + cLabel);


            int[] posAry = new int[cSize];
            int[] nbrAry = new int[cSize];
            int    pindx = 0;
            for (int m = 0; m < len; m++) {
                if (iAry[m] == cLabel) {
                    iAry[m] = 11111;
                    inData[m] = 0;
                } else {
                    iAry[m] = 0;
                }
            }

            int ws = w + 1;
            int we = len - w - 1;

            // Build outer neighbor ring for the next score
            for (int j = ws; j < we; j++) {
                if (iAry[j] == 11111) {
                    if (iAry[j - 1] < 1) {
                        iAry[j - 1] = 22222;
//                        Log.d(TAG, "  >>> BlobSeparate() adding Ring 1 point");
                    }
                    if (iAry[j + 1] < 1) {
                        iAry[j + 1] = 22222;
//                        Log.d(TAG, "  >>> BlobSeparate() adding Ring 1 point");
                    }
                    if (iAry[j - w] < 1) {
                        iAry[j - w] = 22222;
//                        Log.d(TAG, "  >>> BlobSeparate() adding Ring 1 point");
                    }
                    if (iAry[j + w] < 1) {
                        iAry[j + w] = 22222;
//                        Log.d(TAG, "  >>> BlobSeparate() adding Ring 1 point");
                    }
                }
            }

        }
        return mblob;
    }


    public Blob BlobAnalysis(int[] inData, int[] bmap, int w, int h, int Thresh, int minSize, int maxSize, boolean useDark) {
        // Identify patches in inData and mark them with distinct values using X-/Y- grow-and-merge method
        //
        //  INPUT:
        //   inData  -  GrayScale original patch map to be analyzed on
        //
        //   bmap    -  resulting blobs map (set up to be the same as mGrayProc.iAry)
        //
        //   thresh  -  Threshold for determining transitions
        //
        //   minSize -  minimum pixel-count size for care patches; used to remove noises (don't-care patches).
        //              This value is typically set to be the size of the Center BE. Therefore equals PI*(r_BE**2) .
        //
        //   useDark -  true: analyze areas of intensity below Thresh
        //              false: analyze areas of intensity above Thresh
        //
        //  RETURN:
        //   quality - analysis quality:
        //                  -1: failed analysis
        //             0 - 100: quality level; 100 being 'a perfect disc was detected'
        //
        //=================================================================================================
        Log.d(TAG, "BlobAnalysis() entered...");

        Blob mblob = new Blob();
        mblob.setQuality(-999);

        if (inData == null || inData.length < 100) {
            Log.d(TAG, "BlobAnalysis() aborted due to invalid raw data!");
            return mblob;
        }
        if (w < 6 || h < 6 || w * h != inData.length) {
            Log.d(TAG, "BlobAnalysis() aborted due to bad data: size / w / h = " + inData.length + " / " + w + " / " + h);
            return mblob;
        }

        final int MaxLabel = 2001;

        int     len = inData.length;
        int    indx = 0, mcnt = 0;
        int    Mark = MaxLabel;
        int     hq1 = h / 4;
        int     hq3 = (h * 3) / 4;
        int     wq1 = w / 4;
        int     wq3 = (w * 3) / 4;
        boolean  on = false;

        Log.d(TAG, "BlobAnalysis() in data: size / w / h / useDark / Thresh / minSize / maxSize = " + len + " / " + w + " / " + h + " / " +
                useDark + " / " + Thresh + " / " + minSize + " / " + maxSize);

        for (int i = 0; i < len; i++) iAry[i] = 0;

        if (useDark) { // analyze the low intensity patches
            // First round for labeling in each row
            for (int y = 0; y < h; y++) {
                if (on) { // force to reset if last row was live at the end
                    on   = false;
                    mcnt = 0;
                    Mark = Mark--;
                    if (Mark < 1) Mark = MaxLabel; // recycle the label as needed
                }
                for (int x = 0; x < w; x++, indx++) {
                    if (inData[indx] < Thresh) {
                        // Mark pixel and expand its region
                        iAry[indx] = Mark;
                        on = true;
                        mcnt++;
                    } else {
                        if (on) {
//                            Log.d(TAG, "BlobAnalysis() X-final size for Mark <" + Mark + "> = " + mcnt);
                            mcnt = 0;
                            on = false;
                            Mark--;
                            if (Mark < 1) Mark = MaxLabel;
                        }
                    }
                }
            }
        } else { // analyze the high intensity patches
            // First round for labeling in each row
            for (int y = 0; y < h; y++) {
                if (on) { // force to reset if last row was live at the end
                    on   = false;
                    mcnt = 0;
                    Mark = Mark--;
                    if (Mark < 1) Mark = MaxLabel;
                }
                for (int x = 0; x < w; x++, indx++) {
                    if (inData[indx] > Thresh) {
                        // Mark pixel and expand its region
                        iAry[indx] = Mark;
                        on = true;
                        mcnt++;
                    } else {
                        if (on) {
//                            Log.d(TAG, "BlobAnalysis() X-final size for Mark <" + Mark + "> = " + mcnt);
                            mcnt = 0;
                            on = false;
                            Mark--;
                            if (Mark < 1) Mark = MaxLabel;
                        }
                    }
                }
            }
        }

        // Second round for merging in Y- direction
        // - we favor using the greater labels, which should appear in the lower index rows!
        //
        int lenw = len - w;
        for (int i = 0; i < lenw; i++) {
            int j = i + w;
            if (iAry[i] > 0 && iAry[j] > 0) {
                if (iAry[i] != iAry[j]) {
                    Mark = iAry[i];
                    int ste = iAry[j];

                    for (int k = 0; k < len; k++) {
                        if (iAry[k] == ste) iAry[k] = Mark;
                    }
                }
            }
        }
        // Third round to merge in X-direction
        int lenm = len - 1;
        for (int i = 0; i < lenm; i++) {
            int ii = i + 1;
            if (iAry[i] > 0 && iAry[ii] > 0) {
                if (iAry[i] != iAry[ii]) {
                    Mark = iAry[i];
                    int ste = iAry[ii];

                    for (int j = 0; j < len; j++) {
                        if (iAry[j] == ste) iAry[j] = Mark;
                    }
                }
            }
        }

        int[] ss = new int[MaxLabel + 1];
        int bcnt = 0;
        for (int i = 0; i <= MaxLabel; i++) ss[i] = 0;

        for (indx = 0; indx < len; indx++) {
            if (iAry[indx] > 0) ss[iAry[indx]]++;
        }

        for (int i = 0; i <= MaxLabel; i++) {
            if (ss[i] > 0) {
                bcnt++;
                Log.d(TAG, "BlobAnalysis() final merged blob size( " + i + " ) = " + ss[i]);
            }
        }


        // compress ss[] array down to bSizeIndx[] and bSizes[] arrays of new size bcnt,
        // where they are sorted by the total pixel count!
        int[] bSizeIndx = new int[bcnt];
        int[] bSizes = new int[bcnt];
        int blobCount = 0;

        for (int i = 0; i <= MaxLabel; i++) {
            if (ss[i] >= minSize && ss[i] <= maxSize) {
                bSizeIndx[blobCount] = i;
                bSizes[blobCount] = ss[i];
                blobCount++;
            }
        }

        Log.d(TAG, " BlobAnalysis() initial blob count (blobCount) = " + blobCount);
        if (blobCount < 1) {
            // failed - early return!
            Log.d(TAG, " XXXX BlobAnalysis() failed finding blobs!");
            return mblob;
        }

        int bCountm = blobCount - 1;

        for (int i = 0; i < bCountm; i++) {
            for (int j = i + 1; j < blobCount; j++) {
                if (bSizes[i] > bSizes[j]) {
                    int bb = bSizes[i];
                    bSizes[i] = bSizes[j];
                    bSizes[j] = bb;

                    bb = bSizeIndx[i];
                    bSizeIndx[i] = bSizeIndx[j];
                    bSizeIndx[j] = bb;
                }
            }
        }

        Log.d(TAG, " BlobAnalysis() final blob count (blobCount) = " + blobCount);

        Blob[] blobs = new Blob[blobCount];
        for (int i = 0; i < blobCount; i++) {
            blobs[i] = new Blob();
            blobs[i].setQuality(-999);
        }

        //// -- evaluate the quality -- ////
        // factors considered:
        //   - number of small speckles caught; the fewer the better
        //   - closeness of the small speckles; the farther the better
        //   - roundness of the large blobs; at least one should be round
        //   - holes in the large blobs; the fewer the better
        //   - Bulls Eye location; should be in the center of a round blob

        // min blob size is 2000-pix for up to 4 ft camera distance
        int mquality = 0;
        for (int i = 0; i < blobCount; i++) {
            Log.d(TAG, "\n <----> BlobAnalysis() ready for evaluating blob[" + i + "]: size = " + bSizes[i] + "\n");

//            if (bSizes[i] < maxSize) {

            blobs[i].Evaluate(bmap, w, h, bSizeIndx[i], bSizeIndx[i]);

            blobs[i].logging();

            if (blobs[i].x < wq1 || blobs[i].x > wq3 || blobs[i].y < hq1 || blobs[i].y > hq3) {
                //  center is not in the expected area!
                Log.d(TAG, "\n <----> BlobAnalysis() evaluated blob[" + i + "] center off too far!");
                continue;
            }

            if ((blobs[i].density / 0.785) > 0.9 && (blobs[i].density / 0.785 < 1.1) &&
                    (blobs[i].aspRat > 0.8) && (blobs[i].aspRat < 1.2) &&
                    blobs[i].perem < (blobs[i].xDim + blobs[i].yDim) * 2
                    ) {
                mquality = 95;
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobAnalysis() setting blob[" + i + "] Quality to 95!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.85 && (blobs[i].density / 0.785 < 1.15) &&
                    (blobs[i].aspRat > 0.7) && (blobs[i].aspRat < 1.3) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.1)
                    ) {
                mquality = 90;
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobAnalysis() setting blob[" + i + "]  Quality to 90!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.8 && (blobs[i].density / 0.785 < 1.23) &&
                    (blobs[i].aspRat > 0.6) && (blobs[i].aspRat < 1.4) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.15)
                    ) {
                mquality = 85;
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobAnalysis() setting blob[" + i + "]  Quality to 85!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.8 && (blobs[i].density / 0.785 < 1.23) &&
                    (blobs[i].aspRat > 0.6) && (blobs[i].aspRat < 1.4) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.22)
                    ) {
                mquality = 80;
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobAnalysis() setting blob[" + i + "]  Quality to 80!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.8 && (blobs[i].density / 0.785 < 1.23) &&
                    (blobs[i].aspRat > 0.6) && (blobs[i].aspRat < 1.4) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.35)
                    ) {
                mquality = 75;
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobAnalysis() setting blob[" + i + "]  Quality to 75!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.8 && (blobs[i].density / 0.785 < 1.23) &&
                    (blobs[i].aspRat > 0.6) && (blobs[i].aspRat < 1.4) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.55)
                    ) {
                mquality = 72;
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobAnalysis() setting blob[" + i + "]  Quality to 72!");

                // break;
            } else if ((blobs[i].density / 0.785) > 0.8 && (blobs[i].density / 0.785 < 1.23) &&
                    (blobs[i].aspRat > 0.6) && (blobs[i].aspRat < 1.4) &&
                    (blobs[i].perem / ((blobs[i].xDim + blobs[i].yDim) * 2) < 1.95)
                    ) {
                mquality = 70;
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobAnalysis() setting blob[" + i + "]  Quality to 70!");

                // break;
            } else {
                mquality = 55;
                blobs[i].setQuality(mquality);
                Log.d(TAG, "BlobAnalysis() setting blob[" + i + "]  Quality to 55! Label = " + bSizeIndx[i]);
            }
//            }

            Log.d(TAG, " >> BlobAnalysis() blob[ " + i + " ] quality = " + blobs[i].quality);
        }

        Log.d(TAG, " >> BlobAnalysis() center reduction began with count = " + blobCount);

        ////  find the BE center
        int   centerN = 0;
        int      cenX = 0;
        int      cenY = 0;
        int cenMaxOff = (int) (minSize / (2 * 3.14159));

        Log.d(TAG, "  >>> BlobAnalysis() - cenMaxOff = " + cenMaxOff);

        for (int i = 0; i < blobCount; i++) {
            if (blobs[i].quality >= 50) {
                cenX += blobs[i].x;
                cenY += blobs[i].y;
                centerN++;

                Log.d(TAG, "  >>> BlobAnalysis() validated blob index #" + i + "; location = " + blobs[i].x + ", " + blobs[i].y
                        + " |  cenX/centY = " + cenX + " / " + cenY);
            }
        }

        cenX /= centerN;
        cenY /= centerN;

        Log.d(TAG, "  >>> BlobAnalysis() - cenX / centY = " + cenX + " / " + cenY);

        // center location consistency check - assuming that most are in good shape
        for (int i = 0; i < blobCount; i++) {
            if (blobs[i].quality >= 50) {
                if (cenX - blobs[i].x > cenMaxOff || cenX - blobs[i].x < -cenMaxOff) {
                    blobs[i].quality = -99;
                } else if (cenY - blobs[i].y > cenMaxOff || cenY - blobs[i].y < -cenMaxOff) {
                    blobs[i].quality = -99;
                }
            }

            Log.d(TAG, "   >>>> BlobAnalysis() index #" + i + "; cenX / centY = " + cenX + " / " + cenY);
        }

        // 1. prep location array for each care patch so that we can determine the neighbor count for each pixel

        // Needs update!
        //  mblob is changed to blobs[]!!!!
        //


        int[][]    posAry = new int[blobCount][];
        byte[][]   nbrAry = new byte[blobCount][];
        final int      wp = w + 1;
        final int      wm = w - 1;
        int          maxQ = 0;
        int        maxQID = -1;

        for (int i = 0; i < blobCount; i++) {
            Log.d(TAG, " - BlobAnalysis() processing indx: " + i + " quality = " + blobs[i].quality + " of length " + bSizes[i]);

            if (blobs[i].quality >= 50) {
                final int tbsize = bSizes[i];
                final int rMarker = i * 11 + 22 + 100000; // scoring ring marker
                posAry[i] = new int[tbsize];
                nbrAry[i] = new byte[tbsize];

                int pLab  = bSizeIndx[i];
                int pindx = 0;
                for (int m = 0; m < len && pindx < tbsize; m++) {
                    if (iAry[m] == pLab) posAry[i][pindx++] = m;
                }

                Log.d(TAG, " - BlobAnalysis() processing label " + pLab + " of length " + pindx);

                // find 8-nbr count of each care patch pixel
                for (int j = 0; j < pindx; j++) {
                    int pos1 = posAry[i][j];
                    nbrAry[i][j] = 0;
                    for (int k = 0; k < pindx; k++) {
                        int pos2 = posAry[i][k];

                        if (pos1 - pos2 == 1) {
                            nbrAry[i][j]++;
                        } else if (pos1 - pos2 == -1) {
                            nbrAry[i][j]++;
                        } else if (pos1 - pos2 == w) {
                            nbrAry[i][j]++;
                        } else if (pos1 - pos2 == -w) {
                            nbrAry[i][j]++;
                        } else if (pos1 - pos2 == wp) {
                            nbrAry[i][j]++;
                            if (nbrAry[i][j] > 5) break;
                        } else if (pos1 - pos2 == wm) {
                            nbrAry[i][j]++;
                            if (nbrAry[i][j] > 5) break;
                        } else if (pos1 - pos2 == -wp) {
                            nbrAry[i][j]++;
                            if (nbrAry[i][j] > 5) break;
                        } else if (pos1 - pos2 == -wm) {
                            nbrAry[i][j]++;
                        }
                    }

                    // identify 1- and 2- nbr pixels in the patches and mark them by Ring Number
                    if (nbrAry[i][j] == 4 || nbrAry[i][j] == 5) {
//                        Log.d(TAG, " -- BlobAnalysis() assign < "+pos1+" > value of "+pLab);
                        iAry[pos1] = rMarker; // scoring ring marker
                    }
                }

                if (maxQ < blobs[i].quality) {
                    maxQ = blobs[i].quality;
                    maxQID = i;
                }
            } else {
                bSizeIndx[i] = -1;
            }
        }

        // return the highest quality blob
        if (maxQID >= 0) mblob.copy(blobs[maxQID]);

        return mblob;
    }


    public void GrowBlob(int[] inData, byte[] bmap, int w, int h, int x, int y, int Thresh, boolean useDark, byte Mark) {

        // Recursive function to grow the patch of label 'Mark' from the given (x, y) location under the times limit 'BlobRunLimit'.
        //
        //  inData  - the intensity 2-D map in 1-D format
        //  bmap    - the label map; to be modified by the GrowBlob() function
        //  useDark - use the areas of intensity value less than the threshold 'Thresh', if true
        //            use the areas of intensity value greater than the threshold 'Thresh', if false
        //


        int       i, j;
        final int indx = y * w + x;

        Log.d(TAG, "GrowBlob() entered at ( " + x + ", " + y + " )");

        if (bmap[indx] < 1 || blobrun >= BlobRunLimit) return;

        Log.d(TAG, "GrowBlob() entered with blobrun / Mark = " + blobrun + " / " + Mark);

        if (useDark) {
            if (x > 0 && bmap[indx - 1] < 1 && inData[indx - 1] < Thresh) {
                bmap[indx - 1] = Mark;
                blobrun++;
                if (blobrun < BlobRunLimit) {
                    GrowBlob(inData, bmap, w, h, x - 1, y, Thresh, useDark, Mark);
                }
            }
            if (x < (w - 1) && bmap[indx + 1] < 1 && inData[indx + 1] < Thresh) {
                bmap[indx + 1] = Mark;
                blobrun++;
                if (blobrun < BlobRunLimit) {
                    GrowBlob(inData, bmap, w, h, x + 1, y, Thresh, useDark, Mark);
                }
            }
            if (y > 0 && bmap[indx - w] < 1 && inData[indx - w] < Thresh) {
                bmap[indx - w] = Mark;
                blobrun++;
                if (blobrun < BlobRunLimit) {
                    GrowBlob(inData, bmap, w, h, x, y - 1, Thresh, useDark, Mark);
                }
            }
            if (y < (h - 1) && bmap[indx + w] < 1 && inData[indx + w] < Thresh) {
                bmap[indx + w] = Mark;
                blobrun++;
                if (blobrun < BlobRunLimit) {
                    GrowBlob(inData, bmap, w, h, x, y + 1, Thresh, useDark, Mark);
                }
            }
        } else {
            if (x > 0 && bmap[indx - 1] < 1 && inData[indx - 1] > Thresh) {
                bmap[indx - 1] = Mark;
                blobrun++;
                if (blobrun < BlobRunLimit) {
                    GrowBlob(inData, bmap, w, h, x - 1, y, Thresh, useDark, Mark);
                }
            }
            if (x < (w - 1) && bmap[indx + 1] < 1 && inData[indx + 1] > Thresh) {
                bmap[indx + 1] = Mark;
                blobrun++;
                if (blobrun < BlobRunLimit) {
                    GrowBlob(inData, bmap, w, h, x + 1, y, Thresh, useDark, Mark);
                }
            }
            if (y > 0 && bmap[indx - w] < 1 && inData[indx - w] > Thresh) {
                bmap[indx - w] = Mark;
                blobrun++;
                if (blobrun < BlobRunLimit) {
                    GrowBlob(inData, bmap, w, h, x, y - 1, Thresh, useDark, Mark);
                }
            }
            if (y < (h - 1) && bmap[indx + w] < 1 && inData[indx + w] > Thresh) {
                bmap[indx + w] = Mark;
                blobrun++;
                if (blobrun < BlobRunLimit) {
                    GrowBlob(inData, bmap, w, h, x, y + 1, Thresh, useDark, Mark);
                }
            }
        }

        return;
    }


    public int[] erodeDilateBin(int[] inData, int nCols, int kernelSize, int mode)
    // Erode or Dilate function, assuming 8-bit Gray Scale image of values of 0 or 255 only.
    // mode:  1 for Dilate
    //       -1 for Erode
    {
        if (inData.length < 25 || nCols < 2) return null;
        startTime = System.currentTimeMillis();

        Log.d(TAG, "erodeDilateBin() - mode | StartTime = " + mode + " | " + startTime);

        if (kernelSize < 3) kernelSize = 3;

        int KSize2 = (int) (kernelSize * 0.5);
        if (KSize2 < 2) KSize2 = 2;

        int sz = inData.length;
        int[] outData = new int[sz];
        for (int i = 0; i < sz; i++) outData[i] = inData[i];

        int wid = nCols;
        int hgt = sz / wid;
        int indx, yindx;
        int hgtm = hgt - KSize2, widm = wid - KSize2;
        ;

        if (mode < 0) { // Erode
            for (int y = 1; y < hgtm; y++) {
                indx = y * wid + 1;
                for (int x = 1; x < widm; x++, indx++) {
                    if (inData[indx] > 199) { // this pixel is bright so needs to be processed
                        erodeOuterLoop:
                        for (int yy = y - KSize2; yy <= y + KSize2; yy++) {
                            yindx = yy * wid + x - KSize2;
                            for (int xx = x - KSize2; xx <= x + KSize2; xx++, yindx++) {
                                if (inData[yindx] < 199) {   // pixel at x,y needs to be remove
                                    outData[indx] = 0;
                                    break erodeOuterLoop;     // exit xx and yy loops
                                }
                            }
                        }
                    }
                }
            }
        } else { // Dilate
            for (int y = 1; y < hgtm; y++) {
                indx = y * wid + 1;
                for (int x = 1; x < widm; x++, indx++) {
                    if (inData[indx] < 199) { // this pixel is dark so needs to be processed
                        dilateOuterLoop:
                        for (int yy = y - KSize2; yy <= y + KSize2; yy++) {
                            yindx = yy * wid + x - KSize2;
                            for (int xx = x - KSize2; xx <= x + KSize2; xx++, yindx++) {
                                if (inData[yindx] > 199) {   // pixel at x,y needs to be remove
                                    outData[indx] = 255; // set all 3 bands
                                    break dilateOuterLoop;     // exit xx and yy loops
                                }
                            }
                        }
                    }
                }
            }
        }
        // copy data from output array to original array
        //for (i = 0; i < sz; i++) inData[i] = outData[i];

        endTime = System.currentTimeMillis();
        Log.d("  ===erodeDilateBin() ", "- mode | endTime = " + mode + " | " + endTime);
        return outData;

    }

    public int[] erodeDilateBinWithEdgeMap(int[] inData, byte[] edgeMap, byte[] maskMap, int nCols, int kernelSize, int mode, boolean allowEdge, int biThresh)
    // Erode or Dilate function, assuming 8-bit Gray Scale image of binary values of 0 or 255 only.
    // mode:  1 for Dilate
    //       -1 for Erode
    {
        if (inData.length < 25 || nCols < 2) return null;
        startTime = System.currentTimeMillis();

        Log.d("===erodeDilateBinWEMap", "- mode | startTime = " + mode + " | " + startTime);
        if (kernelSize < 3) kernelSize = 3;

        int KSize2 = (int) (kernelSize * 0.5);
        if (KSize2 < 2) KSize2 = 2;

        int sz = inData.length;
        int[] outData = new int[sz];
        for (int i = 0; i < sz; i++) outData[i] = inData[i];

        int wid = nCols;
        int hgt = sz / wid;
        int indx, yindx;
        int hgtm = hgt - KSize2, widm = wid - KSize2;
        ;

        if (mode < 0) { // Erode
            for (int y = 1; y < hgtm; y++) {
                indx = y * wid + 1;
                for (int x = 1; x < widm; x++, indx++) {
                    if (edgeMap[indx] > 100 && !allowEdge) continue; // on an Edge Point

                    if (inData[indx] > biThresh) { // this pixel is bright so needs to be processed
                        erodeOuterLoop:
                        for (int yy = y - KSize2; yy <= y + KSize2; yy++) {
                            yindx = yy * wid + x - KSize2;
                            for (int xx = x - KSize2; xx <= x + KSize2; xx++, yindx++) {
                                if (edgeMap[yindx] > 100 && !allowEdge)
                                    continue; // on an Edge Point

                                if (inData[yindx] < biThresh) {   // pixel at x,y needs to be remove
                                    outData[indx] = inData[yindx];
                                    maskMap[indx] = 0;
                                    break erodeOuterLoop;     // exit xx and yy loops
                                }
                            }
                        }
                    }
                }
            }
        } else { // Dilate
            for (int y = 1; y < hgtm; y++) {
                indx = y * wid + 1;
                for (int x = 1; x < widm; x++, indx++) {
                    if (edgeMap[indx] > 100 && !allowEdge) continue; // on an Edge Point

                    if (inData[indx] < biThresh) { // this pixel is dark so needs to be processed
                        dilateOuterLoop:
                        for (int yy = y - KSize2; yy <= y + KSize2; yy++) {
                            yindx = yy * wid + x - KSize2;
                            for (int xx = x - KSize2; xx <= x + KSize2; xx++, yindx++) {
                                if (edgeMap[yindx] > 100 && !allowEdge)
                                    continue; // on an Edge Point

                                if (inData[yindx] > biThresh) {   // pixel at x,y needs to be remove
                                    outData[indx] = inData[yindx];
                                    maskMap[indx] = 127; // set all 3 bands
                                    break dilateOuterLoop;     // exit xx and yy loops
                                }
                            }
                        }
                    }
                }
            }
        }
        // copy data from output array to original array
        //for (i = 0; i < sz; i++) inData[i] = outData[i];

        endTime = System.currentTimeMillis();

        Log.d(" ===erodeDilateBinWEMap", "- mode | endTime = " + mode + " | " + endTime);
        return outData;

    }

    public boolean convolveGray3x3(int[] inData, int[] outData, int width, int height, float[][] krnl, boolean invert) {
        /// Gray Scale image convolution - 8 bit unsigned

        int size = width * height;
        if (inData.length != size || outData.length != size) return false;

        startTime = System.currentTimeMillis();

        Log.d("===convolveGray3x3", "- startTime = " + startTime);

        int idx, pyc, pyp, pyn, pxc, pxp, pxn;
        int x, y, heightm = height - 1, widthm = width - 1;
        float k00 = krnl[0][0], k01 = krnl[0][1], k02 = krnl[0][2],
                k10 = krnl[1][0], k11 = krnl[1][1], k12 = krnl[1][2],
                k20 = krnl[2][0], k21 = krnl[2][1], k22 = krnl[2][2];

        int p00, p01, p02,
                p10, p11, p12,
                p20, p21, p22;

        float g;

        for (y = 0; y < height; ++y) {
            pyc = y * width;
            pyp = pyc - width;
            pyn = pyc + width;

            if (y < 1) pyp = pyc;
            if (y >= heightm) pyn = pyc;

            for (x = 0, idx = pyc; x < width; ++x, ++idx) {
                pxc = x;
                pxp = pxc - 1;
                pxn = pxc + 1;

                if (x < 1) pxp = pxc;
                if (x >= widthm) pxn = pxc;

                p00 = pyp + pxp;
                p01 = pyp + pxc;
                p02 = pyp + pxn;
                p10 = pyc + pxp;
                p11 = pyc + pxc;
                p12 = pyc + pxn;
                p20 = pyn + pxp;
                p21 = pyn + pxc;
                p22 = pyn + pxn;

                g = inData[p00] * k00 + inData[p01] * k01 + inData[p02] * k02
                        + inData[p10] * k10 + inData[p11] * k11 + inData[p12] * k12
                        + inData[p20] * k20 + inData[p21] * k21 + inData[p22] * k22;

                if (invert) {
                    g = 255 - g;
                }

                outData[idx] = (int) g;
            }
        }
        endTime = System.currentTimeMillis();

        Log.d("  ===convolveGray3x3", "- endTime = " + endTime);

        return true;
    }

    public boolean convolveGray5x5(int[] inData, int[] outData, int width, int height, float[][] krnl, boolean invert) {
        /// 8-bit Gray Scale Image Convolution

        startTime = System.currentTimeMillis();

        Log.d(TAG, "===  convolveGray5x5() - startTime = " + startTime);

        int pyc, pyp, pyn, pypp, pynn,
                pxc, pxp, pxn, pxpp, pxnn;
        int idx, x, y;
        int widthm = width - 1, widthmm = width - 2, heightm = height - 1, heightmm = height - 2;

        float g;
        float k00 = krnl[0][0], k01 = krnl[0][1], k02 = krnl[0][2], k03 = krnl[0][3], k04 = krnl[0][4],
                k10 = krnl[1][0], k11 = krnl[1][1], k12 = krnl[1][2], k13 = krnl[1][3], k14 = krnl[1][4],
                k20 = krnl[2][0], k21 = krnl[2][1], k22 = krnl[2][2], k23 = krnl[2][3], k24 = krnl[2][4],
                k30 = krnl[3][0], k31 = krnl[3][1], k32 = krnl[3][2], k33 = krnl[3][3], k34 = krnl[3][4],
                k40 = krnl[4][0], k41 = krnl[4][1], k42 = krnl[4][2], k43 = krnl[4][3], k44 = krnl[4][4];

        int size = width * height;
        int safeStart = width * 2 + 2;
        int safeEnd = size - width * 2 - 2;
        int[] dat00 = new int[size];
        int[] dat01 = new int[size];
        int[] dat02 = new int[size];
        int[] dat03 = new int[size];
        int[] dat04 = new int[size];
        int[] dat10 = new int[size];
        int[] dat11 = new int[size];
        int[] dat12 = new int[size];
        int[] dat13 = new int[size];
        int[] dat14 = new int[size];
        int[] dat20 = new int[size];
        int[] dat21 = new int[size];
//        int[] dat22 = new int[size];
        int[] dat23 = new int[size];
        int[] dat24 = new int[size];
        int[] dat30 = new int[size];
        int[] dat31 = new int[size];
        int[] dat32 = new int[size];
        int[] dat33 = new int[size];
        int[] dat34 = new int[size];
        int[] dat40 = new int[size];
        int[] dat41 = new int[size];
        int[] dat42 = new int[size];
        int[] dat43 = new int[size];
        int[] dat44 = new int[size];

        int fset00 = -width * 2 - 2;
        int fset01 = fset00 + 1;
        int fset02 = fset01 + 1;
        int fset03 = fset02 + 1;
        int fset04 = fset03 + 1;
        int fset10 = -width - 2;
        int fset11 = fset10 + 1;
        int fset12 = fset11 + 1;
        int fset13 = fset12 + 1;
        int fset14 = fset13 + 1;
        int fset20 = -2;
        int fset21 = -1;
//        int  fset22 = fset21 + 1; // self
        int fset23 = +1;
        int fset24 = +2;
        int fset30 = width - 2;
        int fset31 = fset30 + 1;
        int fset32 = fset31 + 1;
        int fset33 = fset32 + 1;
        int fset34 = fset33 + 1;
        int fset40 = width * 2 - 2;
        int fset41 = fset40 + 1;
        int fset42 = fset41 + 1;
        int fset43 = fset42 + 1;
        int fset44 = fset43 + 1;

        for (int i = safeStart; i < safeEnd; i++) {
            dat00[i] = inData[i + fset00];
            dat01[i] = inData[i + fset01];
            dat02[i] = inData[i + fset02];
            dat03[i] = inData[i + fset03];
            dat04[i] = inData[i + fset04];
            dat10[i] = inData[i + fset10];
            dat11[i] = inData[i + fset11];
            dat12[i] = inData[i + fset12];
            dat13[i] = inData[i + fset13];
            dat14[i] = inData[i + fset14];
            dat20[i] = inData[i + fset20];
            dat21[i] = inData[i + fset21];
//            dat22[i] = inData[i+fset00];
            dat23[i] = inData[i + fset23];
            dat24[i] = inData[i + fset24];
            dat30[i] = inData[i + fset30];
            dat31[i] = inData[i + fset31];
            dat32[i] = inData[i + fset32];
            dat33[i] = inData[i + fset33];
            dat34[i] = inData[i + fset34];
            dat40[i] = inData[i + fset40];
            dat41[i] = inData[i + fset41];
            dat42[i] = inData[i + fset42];
            dat43[i] = inData[i + fset43];
            dat44[i] = inData[i + fset44];
        }

        for (int i = safeStart; i < safeEnd; ++i) {
            g = dat00[i] * k00 + dat01[i] * k01 + dat02[i] * k02 + dat03[i] * k03 + dat04[i] * k04
                    + dat10[i] * k10 + dat11[i] * k11 + dat12[i] * k12 + dat13[i] * k13 + dat14[i] * k14
                    + dat20[i] * k20 + dat21[i] * k21 + inData[i] * k22 + dat23[i] * k23 + dat24[i] * k24
                    + dat30[i] * k30 + dat31[i] * k31 + dat32[i] * k32 + dat33[i] * k33 + dat34[i] * k34
                    + dat40[i] * k40 + dat41[i] * k41 + dat42[i] * k42 + dat43[i] * k43 + dat44[i] * k44;

            if (invert) {
                g = 255 - g;
            }

            outData[i] = (int) g;
        }
        endTime = System.currentTimeMillis();

        Log.d("  ===convolveGray5x5", "- endTime = " + endTime);
        return true;
    }

    public boolean gaussian(int[] inData, int[] outData, int width, int height, int krnlSize, boolean isGrayScale) {
        int r, g, b, a, idx;
        int size = width * height;
        int size4 = size * 4; // rgba

        int x, y, i, j, inx, iny;
        float w = 0f;
//        int[] tmpData = new int[n];
        int maxkrnlSize = 13;

        startTime = System.currentTimeMillis();

        if (isGrayScale) Log.d(TAG, " gaussian(GrayScale) - startTime = " + startTime);
        else Log.d(TAG, " gaussian( Color ) - startTime = " + startTime);

        krnlSize = (krnlSize > maxkrnlSize) ? maxkrnlSize : krnlSize;
        if (krnlSize < 3) krnlSize = 3;

        int   krnlSizem = krnlSize - 1;
        int          k1 = -krnlSizem / 2;
        int          k2 = krnlSize + k1;
        float[] weights = new float[krnlSize];
        int[][]   krnls = new int[maxkrnlSize][maxkrnlSize];

        krnls[0][0] = 1;
        for (i = 1; i < maxkrnlSize; ++i) {
            krnls[0][i] = 0;
        }

        for (i = 1; i < maxkrnlSize; ++i) {
            krnls[i][0] = 1;
            for (j = 1; j < maxkrnlSize; ++j) {
                krnls[i][j] = krnls[i - 1][j] + krnls[i - 1][j - 1];
            }
        }
/*
        Log.d(TAG, " gaussian( ) - kernel[0] = " + krnls[0]);
        Log.d(TAG, " gaussian( ) - kernel[1] = " + krnls[1]);
        Log.d(TAG, " gaussian( ) - kernel[2] = " + krnls[2]);
        Log.d(TAG, " gaussian( ) - kernel[3] = " + krnls[3]);
        Log.d(TAG, " gaussian( ) - kernel[4] = " + krnls[4]);
        Log.d(TAG, " gaussian( ) - kernel[5] = " + krnls[5]);
        Log.d(TAG, " gaussian( ) - kernel[6] = " + krnls[6]);
*/
        for (i = 0; i < krnlSize; ++i) {
            weights[i] = krnls[krnlSizem][i];
            w += weights[i];
        }
        for (i = 0; i < krnlSize; ++i) {
            weights[i] /= w;
        }

        if (krnlSize == 5) {
            Log.d(TAG, " gaussian( ) - weights[]: " + weights[0] + ", " + weights[1] + ", " + weights[2] + ", " + weights[3] + ", " + weights[4]);
        } else if (krnlSize == 7) {
            Log.d(TAG, " gaussian( ) - weights[]: " + weights[0] + ", " + weights[1] + ", " + weights[2] + ", " + weights[3] + ", " + weights[4] +
                    ", " + weights[5] + ", " + weights[6]);
        } else if (krnlSize >= 9) {
            Log.d(TAG, " gaussian( ) - weights[]: " + weights[0] + ", " + weights[1] + ", " + weights[2] + ", " + weights[3] + ", " + weights[4] +
                    ", " + weights[5] + ", " + weights[6] + ", " + weights[7] + ", " + weights[8]);
        }

        int indxy;
        int widthm = width - 1, heightm = height - 1;
        int ii, hOffset;

        int bugfix = 5 * width;

        if (isGrayScale) {  //// GrayScale ////
            int[] tmpData = new int[size];

            // pass 1 - X-direction
            for (y = 0; y < height; ++y) {
                indxy = y * width;
                hOffset = indxy;
                for (x = 0; x < width; ++x, ++indxy) {
                    r = 0;

                    for (i = k1, ii = 0; i < k2; ++i, ++ii) {
                        w = weights[ii];
                        inx = x + i;
                        if (inx < 0) {
                            inx = 0;
                        } else if (inx > widthm) {
                            inx = widthm;
                        }
                        idx = (hOffset + inx);
                        r += inData[idx] * w;
                    }
                    tmpData[indxy] = r;  // tmpData[idx] = r;
                }
            }

            // pass 2 - Y-direction

            for (y = 0; y < height; ++y) {
                indxy = y * width;
                for (x = 0; x < width; ++x, ++indxy) {
                    r = 0;

                    for (i = k1, ii = 0; i < k2; ++i, ++ii) {
                        w = weights[ii];
                        iny = y + i;
                        if (iny < 0) {
                            iny = 0;
                        } else if (iny > heightm) {
                            iny = heightm;
                        }
                        idx = (iny * width + x);
                        r += tmpData[idx] * w;
                    }
                    outData[indxy] = r;  // outData[idx] = r;

                }
            }

        } else { //// Color /////
            int[] tmpData = new int[size4];

            for (y = 0; y < height; ++y) {
                indxy = y * width;
                for (x = 0; x < width; ++x, indxy++) {
                    r = g = b = a = 0;

                    for (i = k1; i < k2; ++i) {
                        inx = x + i;
                        iny = y;
                        w = weights[i - k1];

                        if (inx < 0) {
                            inx = 0;
                        } else if (inx >= width) {
                            inx = widthm;
                        }

                        idx = (iny * width + inx) * 4;

                        r += inData[idx] * w;
                        g += inData[idx + 1] * w;
                        b += inData[idx + 2] * w;
                        a += inData[idx + 3] * w;
                    }

                    idx = indxy * 4;

                    tmpData[idx] = r;
                    tmpData[idx + 1] = g;
                    tmpData[idx + 2] = b;
                    tmpData[idx + 3] = a;
                }
            }

            // pass 2
            for (y = 0; y < height; ++y) {
                indxy = y * width;
                for (x = 0; x < width; ++x, indxy++) {
                    r = g = b = a = 0;

                    for (i = k1; i < k2; ++i) {
                        inx = x;
                        iny = y + i;
                        w = weights[i - k1];

                        if (iny < 0) {
                            iny = 0;
                        } else if (iny >= height) {
                            iny = heightm;
                        }

                        idx = (iny * width + inx) * 4;

                        r += tmpData[idx] * w;
                        g += tmpData[idx + 1] * w;
                        b += tmpData[idx + 2] * w;
                        a += tmpData[idx + 3] * w;
                    }

                    idx = indxy * 4;

                    outData[idx] = r;
                    outData[idx + 1] = g;
                    outData[idx + 2] = b;
                    outData[idx + 3] = a;

                }
            }
        }
        endTime = System.currentTimeMillis();

        Log.d(TAG, "  === gaussian() endTime = " + endTime);
        return true;
    }


    public boolean edgeEnhance3x3(int[] inData, int[] outData, int width, int height) {

        if (width < 5 || height < 5 || inData.length < 25) return false;

        float[][] kern = new float[3][3];

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                kern[i][j] = -1 / 9.0F;
            }

        kern[1][1] = 17 / 9.0f; // (8 + 9) / 9.0

        boolean res = convolveGray3x3(inData, outData, width, height, kern, false);

        return res;
    }

    public boolean edgeEnhance5x5(int[] inData, int[] outData, int width, int height) {
        Log.d(TAG, "  === edgeEnhance5x5() entered...");

        if (width < 5 || height < 5 || inData.length < 25) {
            Log.d(TAG, "  === edgeEnhance5x5() aborted due to bad param...");

            return false;
        }

        float[][] kern = new float[5][5];

        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 5; j++) {
                kern[i][j] = -1 / 25.0F;
            }

        kern[3][3] = 49 / 25.0f; // (24 + 25) / 25.0

        boolean res = convolveGray5x5(inData, outData, width, height, kern, false);

        Log.d(TAG, "  === edgeEnhance5x5() completed with state = " + res);
        return res;
    }

    public int[] calcBinaryThreshold(int[] rData, int[][] bMap, int dimx, int dimy, tTrace[] contours, TRing[] tRings, TRing[][] shotSets,
                                     int tCtx, int[] imgData, int[] tCdd) {
        // function sets '$scope.morphThresholdLow' by examining the gray level of 3 regions:
        //  region 0 - between the Outer Ring and the Inner Ring
        //  region 1 - inside of the Inner Ring
        //  region 2 - ribbon around the Outer Ring
        //
        // !!! epntT index is off by 1 from tRings trcID !!!
        //
        //   iData - 1-D data stream of the original GaryScale image
        //   bMap  - 2-D GaryScale image of the center region (where the BullsEye exists)
        //   contours - the contour set returned from TraceEdge() function call
        //   tRings   -
        //   shotSets - detected rings
        //   tCtx     -
        //   imgData  -
        //   tCdd     - contour map in 1-D stream format; marked to show process results in each region
        //
        int i, j, k;
        int[][]  regn_min = new int[shotSets.length][3];
        int[][]  regn_max = new int[shotSets.length][3];
        int     NRingSets = shotSets.length;

        for (i = 0; i < NRingSets; i++) {
//            regn_min[i] = new int[3];  ///  Array(3);  // min, max gray levels of all 3 regions for all traces
//            regn_max[i] = new int[3]; ///  Array(3);
            for (int ii = 0; ii < 3; ii++) {
                regn_min[i][ii] = 300;
                regn_max[i][ii] = 5;
            }
        }

        int[][] RgnMap = new int[dimy][dimx];
        for (i = 0; i < dimy; i++) {
            for (j = 0; j < dimx; j++) RgnMap[i][j] = 0;
        }

        for (i = 0; i < NRingSets; i++) {
//            int[][]    shot = shotSets[i];
            int nRings = shotSets[i].length;
            if (nRings != 2) continue;                   // only group the complement rings

            double r0 = 0.5 * shotSets[i][0].diameter, r1 = 0.5 * shotSets[i][1].diameter;
            if (r0 > r1 * 3 || r0 < r1 * 1.5) continue;  // size incompatible

            int[] x0 = null, y0 = null;
            int[] indx = {(shotSets[i][0].trcID - 1), (shotSets[i][1].trcID - 1)};

///            int[][] orgPaths = [contours[indx[0]].tAry, contours[indx[1]].tAry];

            int[][][] paths = null;  // new int[][][];

//	          console.log("calcBinaryThreshold() processing contours " + indx[0] + " and " + indx[1]+" | r0/r1 = "+r0+"/"+r1);

            int[]     aLen = {contours[indx[0]].tAry.length, contours[indx[1]].tAry.length};
            int[]  xProLen = null;
            int[]  yProLen = null;
            int[][]   xPro = null;

            for (k = 0; k < 2; k++) {
                int idx = indx[k];
                paths[k] = new int[aLen[k]][2];

//                int[]  tOPa = orgPaths[k], tNPa = paths[k]; // tOPa = contours[indx[0]].tAry[k], tNPa = paths[k];

                int ii = 0;
                int px0 = 99999, px1 = 0, py0 = 99999, py1 = 0;
                for (j = 0; j < aLen[k]; j++) {
//                    int[]   tPt = contours[indx[0]].tAry[j];  // tOPa[j];
//                    int[] tNPa[j]  = new int[2];
//                    int   nPt = paths[k][j];

//                    int    xx = orgPaths[k][j][1], yy = orgPaths[k][j][0];
                    int xx = contours[indx[0]].tAry[j][1];
                    int yy = contours[indx[0]].tAry[j][0];

                    RgnMap[yy][xx] = idx;
                    paths[k][j][1] = xx;
                    paths[k][j][0] = yy;
                    if (yy < py0) {
                        py0 = yy;
                    } else if (yy > py1) {
                        py1 = yy;
                    }

                    if (xx < px0) {
                        px0 = xx;
                    } else if (xx > px1) {
                        px1 = xx;
                    }
                }
                x0[k] = px0;
                y0[k] = py0;
                xProLen[k] = px1 - px0 + 1;
                yProLen[k] = py1 - py0 + 1;
                xPro[k] = null;
                for (j = 0; j < xProLen[k]; j++) {
                    xPro[k][j] = 0;
                }
                for (j = 0; j < aLen[k]; j++) { // get the projection profiles in both dimensions and make a copy of the trace locations
                    int[] tPt = contours[indx[0]].tAry[j];  // tOPa[j];
                    int xx = tPt[1];
                    xPro[k][(xx - px0)]++;
                }

//                paths[k].sort(function (a, b) { if (a[1] == b[1]) return (a[0] < b[0]) ? -1 : 1; return a[1] - b[1]; });  // sort by x first


            }

            ////////////////////////////////////////////
            ///// two regions/two contours used together
            ////////////////////////////////////////////
            int xoff = x0[1] - x0[0], yoff = y0[1] - y0[0];
            int xc = shotSets[i][1].centerX, yc = shotSets[i][1].centerY;
            int xx0 = paths[1][0][1], yy0 = paths[1][0][0], xx1, yy1;
            int idx = indx[1];
            //// for debug -
/*
            int[] cPatch = null;
            int[] cpProp = {0,0, -1};   /// {x: 0, y: 0, gl: -1};
*/

            RgnMap[yy0][xx0] = idx;
            // Region 1 Process -  we intetionally expand the ineterested region by 1 pixel
            regn_min[i][1] = bMap[yc][xc];
            regn_max[i][1] = bMap[yc][xc];
            int clnIdx = 0, cIndxLast = aLen[1] - 1;
            for (j = 1; j < aLen[1]; j++) { // start with the inner circle
                int tx = paths[1][j][1];
                int ty = paths[1][j][0];
                RgnMap[ty][tx] = idx;
                // count the unoccupied pixels inside the circle
                if (tx != xx0) { // new column starts
                    // 1-pix expansion on both ending points
                    clnIdx++;
                    RgnMap[ty - 1][tx] = idx;
                    RgnMap[ty - 1][tx - 1] = idx;
                    RgnMap[ty + 1][tx] = idx;
                    RgnMap[ty][tx - 1] = idx;
                    RgnMap[ty + 1][tx - 1] = idx;
                    yy1 = paths[1][j - 1][0] + 1;
                    RgnMap[yy1 + 1][xx0 + 1] = idx;
                    RgnMap[yy1 + 1][xx0 + 1] = idx;
                    RgnMap[yy1 + 1][xx0 + 1] = idx;
                    xx0 = tx;
                    continue;
                } else if (ty - paths[1][j - 1][0] > 1) {
                    // update region min/max only for the non-edge columns, otherwise we may go out of region
                    int tym = paths[1][j - 1][0];
                    for (int ii = tym + 1; ii < ty; ii++) {
                        if (clnIdx != 0 && clnIdx != cIndxLast && (RgnMap[ii][xx0] == 0 || RgnMap[ii][xx0] == idx)) {
                            if (regn_min[i][1] > bMap[ii][xx0]) regn_min[i][1] = bMap[ii][xx0];
                            if (regn_max[i][1] < bMap[ii][xx0]) regn_max[i][1] = bMap[ii][xx0];

                            /*
                            ///// for debug -
                            var cpp = { x: xx0, y: ii, gl: bMap[ii][xx0] };
                            cPatch.push(cpp);
                            */
                        }
                        RgnMap[ii][tx] = idx;
                        RgnMap[ii - 1][tx] = idx;
                        RgnMap[ii + 1][tx] = idx;
                    }
                } else { // just expand
                    RgnMap[ty - 1][tx] = idx;
                    RgnMap[ty + 1][tx] = idx;
                }
            }
            ////// for debug -
/*
              console.log("  - cpp details of Trace: " + idx);
	    	  for(j=0; j<cPatch.length; j++) {
		    	  console.log("   -- cpp details: x/y/int = "+cPatch[j].x+"/"+cPatch[j].y+"/"+cPatch[j].gl);
		      }
*/
            // Region 0 Process
            xc  = shotSets[i][0].centerX;
            yc  = shotSets[i][0].centerY;
            xx0 = paths[0][0][1];
            yy0 = paths[0][0][0];
            regn_min[i][0] = 300;
            regn_max[i][0] = 5;
            idx = indx[0];
            RgnMap[yy0][xx0] = idx;
            clnIdx = 0;
            cIndxLast = aLen[0] - 1;
            for (j = 1; j < aLen[0]; j++) { // start with the inner circle
                // count the unoccupied pixels inside the circle
                int tx = paths[0][j][1];
                int ty = paths[0][j][0];
                RgnMap[ty][tx] = idx;
                if (tx != xx0) { // new colume starts
                    clnIdx++;
                    xx0 = tx;
                    continue;
                } else if (ty - paths[0][j - 1][0] > 3) { // update graySum of the center hole
                    int tym = paths[0][j - 1][0] + 2, typ = ty - 1;
                    for (int ii = tym; ii < typ; ii++) {
                        if (RgnMap[ii][tx] == 0) { // good pixel
                            RgnMap[ii][tx] = idx;
                            if (clnIdx != 0 && clnIdx != cIndxLast) {
                                if (regn_min[i][0] > bMap[ii][xx0]) regn_min[i][0] = bMap[ii][xx0];
                                if (regn_max[i][0] < bMap[ii][xx0]) regn_max[i][0] = bMap[ii][xx0];
                            }
                        }
                    }
                }
            }
//            Log.d(TAG, "calcBinaryThreshold() regn(" + i + ") Min/Max : " + regn_min[i].join(",") + "/" + regn_max[i].join(","));

        }

        int[] glb_min = null, glb_max = null;
        for (i = 0; i < 3; i++) {
            glb_min[i] = (regn_min[0][i] < 125) ? regn_min[0][i] : 125;
            glb_max[i] = (regn_max[0][i] > 100) ? regn_max[0][i] : 100;
//            Log.d(TAG, "calcBinaryThreshold() regn(" + i + ") Min/Max : " + regn_min[0].join(",") + "/" + regn_max[0].join(","));
        }
        for (i = 1; i < shotSets.length; i++) {
            for (j = 0; j < 2; j++) {
                if (glb_min[j] > regn_min[i][j]) glb_min[j] = regn_min[i][j];
                if (glb_max[j] < regn_max[i][j]) glb_max[j] = regn_max[i][j];
            }
        }

        /// Show the Region Map
        int pix, alpha = 0xff << 24;
//	      var tCdd = imgData.data.buffer;
//        int[] tCdd = new int[dimy * dimx];

        for (i = 0, j = 0; i < dimy; i++) {
//            int tp = RgnMap[i];
            for (k = 0; k < dimx; k++) {
                pix = Math.min(RgnMap[i][k] * 10 + 50, 255);
                tCdd[j++] = alpha | (pix << 16) | (pix << 8) | pix;
            }
        }

/*
        // saving Full-size Blurred Grayscale image in trainCanvasF
        tCtx.putImageData(imgData, 0, 270);
        $timeout(function () { }, 0);
        alert("rm");
*/

        int[] thresh = {25, 125};         // {low: 25, high: 125};
        // determine the threshold now
        if (glb_min[0] > glb_max[1] && glb_min[0] > glb_max[2]) { // ideal condition!
            thresh[0] = (glb_max[1] > glb_max[2]) ? (int) (0.5 * (glb_min[0] + glb_max[1])) : (int) (0.5 * (glb_min[0] + glb_max[2]));

//          thresh.low = (glb_max[1] > glb_max[2]) ? 0.5*(glb_min[0]+glb_max[1]) : 0.5*(glb_min[0]+glb_max[2]);
        } else if (glb_min[0] < glb_max[1]) {
            thresh[0] = glb_min[0];
        } else { // need to comprise
            thresh[0] = (int) (0.5 * (glb_min[0] + glb_max[1]));
        }
        thresh[1] = (int) (Math.max(glb_max[1] * 1.15, thresh[0] + 10));

//        thresh.high = Math.max(glb_max[1] * 1.15, thresh.low + 10);

        Log.d(TAG, " == calcBinaryThreshold() set ThresholdLow to " + thresh[0] + " ThresholdHigh to " + thresh[1]);

        return thresh;
    }

    public tTrace traceEdge(int[] id, int[][] gMap, int[] sPos, int dimX, int dimY, int mark, int direction) {
        // contour tracing using the id[] and gMap[] starting at sPos.
        //
        //   We apply clock-wise movement to detect edge points marked in id[] and gMap[y][x].
        //  While the id[] is not changed, the detected contour is marked in gMap by the 'mark' value and
        //  returned by this function in an array of (x, y) position.
        //
        //   id - a linear 2-D map
        // sPos - starting position
        //
//        int[][] traceAry = new int[G_MAXTRACELEN][2];   // null;
        tTrace traceAry = new tTrace();
        int[] tPos = {0, 0};
        int tPosX, tPosY;
        int i = dimX * dimY;
        int maxLen = 4 * (dimX + dimY);
        int idxPos = dimX * sPos[0] + sPos[1]; // sPos(y, x)!!
        boolean done = false;
        int gap = 9999;

        if (id.length >= i && sPos[0] > 0 && sPos[1] > 0 && sPos[1] < dimY - 1 && sPos[0] < dimX - 1) {
            int[] idxPosAry = {(idxPos - dimX + 1), (idxPos + 1), (idxPos + dimX + 1), (idxPos + dimX), (idxPos + dimX - 1), (idxPos - 1), (idxPos - dimX - 1), (idxPos - dimX)};// 8-neighbor

//                console.log(" traceEdge("+mark+") in action..... Starting 3x3 map: " + gMap[sPos[0] + 1][sPos[1] - 1] + ", " + gMap[sPos[0] + 1][sPos[1]]
//                              + ", " + gMap[sPos[0] + 1][sPos[1] + 1] + ", " + gMap[sPos[0]][sPos[1] + 1] + ", " + gMap[sPos[0] - 1][sPos[1] + 1] + ", "
//                              + gMap[sPos[0] - 1][sPos[1]] + ", " + gMap[sPos[0] - 1][sPos[1] - 1] + ", " + gMap[sPos[0]][sPos[1] - 1] + " | "
//                              + gMap[sPos[0]][sPos[1]]);

            for (int j = 0; j < 8; j++) {
                int mIdx = idxPosAry[j];
                tPosX = mIdx % dimX;
                tPosY = (int) (mIdx / dimX);
                if (id[mIdx] > 199 && gMap[tPosY][tPosX] == 0) {
                    gMap[sPos[0]][sPos[1]] = mark;
                    gMap[tPosY][tPosX] = mark;

                    traceAry.push(sPos);
                    traceAry.push(tPos);

                    idxPos = mIdx;
                    break;
                }
            }

            if (traceAry.length() <= 2) { // early termination
//                    console.log(" ======= Early Termination traceAry[" + (mark - 1) + "] ========    < Length = " + traceAry.length + " >");
                traceAry.gap = gap;
                return traceAry;
            }

            int tGap;
            boolean foundNext = true;

            while (traceAry.length() < maxLen && foundNext) {
                idxPosAry[0] = idxPos - dimX + 1;
                idxPosAry[1] = idxPos + 1;
                idxPosAry[2] = idxPos + dimX + 1;
                idxPosAry[3] = idxPos + dimX;
                idxPosAry[4] = idxPos + dimX - 1;
                idxPosAry[5] = idxPos - 1;
                idxPosAry[6] = idxPos - dimX - 1;
                idxPosAry[7] = idxPos - dimX;

                foundNext = false;
                for (int j = 0; j < 8; j++) {
                    int mIdx = idxPosAry[j];
                    tPosX = mIdx % dimX;
                    tPosY = (int) (mIdx / dimX);
                    if (id[mIdx] > 199 && gMap[tPosY][tPosX] == 0) {
                        foundNext = true;
                        gMap[tPosY][tPosX] = mark;
                        tPos[0] = tPosY;
                        tPos[1] = tPosX;
                        //		                  console.log(tPos.join(","));
                        traceAry.push(tPos);
                        idxPos = mIdx;
                        int cLenm = traceAry.length() - 1;
                        tGap = Math.abs(traceAry.tAry[0][0] - traceAry.tAry[cLenm][0]) + Math.abs(traceAry.tAry[0][1] - traceAry.tAry[cLenm][1]);
                        if (cLenm >= 12 && tGap < gap) gap = tGap; // skip the immediate neighbors
                        break;
                    }
                }
            }

            //// check for closure and extend the tracing if needed
            tGap = Math.abs(traceAry.tAry[0][0] - traceAry.tAry[traceAry.length() - 1][0]) + Math.abs(traceAry.tAry[0][1] - traceAry.tAry[traceAry.length() - 1][1]);
            int nTry = 0;
            while (nTry < 20) { /// need more tracing
                nTry++;
                for (int k = traceAry.length() - 2; k >= 0; k--) {
                    foundNext = true;
                    idxPos = traceAry.tAry[k][0] * dimX + traceAry.tAry[k][1];
                    while (traceAry.length() < maxLen && foundNext) {
                        idxPosAry[0] = idxPos - dimX + 1;
                        idxPosAry[1] = idxPos + 1;
                        idxPosAry[2] = idxPos + dimX + 1;
                        idxPosAry[3] = idxPos + dimX;
                        idxPosAry[4] = idxPos + dimX - 1;
                        idxPosAry[5] = idxPos - 1;
                        idxPosAry[6] = idxPos - dimX - 1;
                        idxPosAry[7] = idxPos - dimX;

                        foundNext = false;
                        for (int j = 0; j < 8; j++) {
                            int mIdx = idxPosAry[j];
                            tPosX = mIdx % dimX;
                            tPosY = mIdx / dimX;
                            if (id[mIdx] > 199 && gMap[tPosY][tPosX] == 0) {
                                foundNext = true;
                                gMap[tPosY][tPosX] = mark;
                                tPos[0] = tPosY;
                                tPos[1] = tPosX;
                                //	                              console.log(tPos.join(","));
                                traceAry.push(tPos);
                                idxPos = mIdx;
                                tGap = Math.abs(traceAry.tAry[0][0] - traceAry.tAry[traceAry.length() - 1][0]) +
                                        Math.abs(traceAry.tAry[0][1] - traceAry.tAry[traceAry.length() - 1][1]);
                                if (traceAry.length() > 12 && tGap < gap)
                                    gap = tGap; // skip the immediate neighbors
                                break;
                            }
                        }
                    }
                }
                //	              if (!foundNext) done = true; // nothing new found
            } //while()
        } else { // nothing to work on
            tPos[0] = -1;
            tPos[1] = -1;

            traceAry.push(tPos);
//                Log.d(Tag, " traceEdge("+mark+") aborted with 0-length data");
        }

//            Log.d(TAG, " ========= end traceAry[" + (mark - 1) + "] ==========    < Gap = "+gap+" >");
        traceAry.gap = gap;
        return traceAry;
    } // traceEdge()

    //////////////////////////////////////////////////////////////////////////////////////

    public boolean laplace3x3(int[] inData, int[] outData, int width, int height) {

        if (width < 5 || height < 5 || inData.length < 25) return false;

        float kTotal = 0.0f;
        float[][] kern = new float[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                kern[i][j] = -1.0F;
                kTotal += kern[i][j];
            }
        }

        kern[1][1] = -kTotal; // 8.0f;

        boolean res = convolveGray3x3(inData, outData, width, height, kern, true);
        return res;
    }

    public boolean laplace5x5(int[] inData, int[] outData, int width, int height) {

        if (width < 10 || height < 10 || inData.length < 100) return false;

        float kTotal = 0.0f;
        float[][] kern = new float[5][5];

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                kern[i][j] = -1.0F;
                kTotal += kern[i][j];
            }
        }

        kern[3][3] = -kTotal; // 24.0f;

        boolean res = convolveGray5x5(inData, outData, width, height, kern, true);
        return res;
    }

    public int[] makeZCSMapMore(int[] rawmap, int[] rbuf, byte[] traceMap, int w, int h, Blob rBlob, int model) {
        //   Find and return the Zero-Crossing strength from the LG image indata[]assuming that the
        //    image contains concentric circles centered at (x0, y0).
        //
        //   1. Fine tune the BE boundary to get the best measurement of its center and size
        //
        //   2. Use known Ring Size Ratio to determine the rough range of  next Ring location
        //
        //   3. Find Anchor Points of the next Ring and then trace out the entire Ring from APs
        //
        //   4. Repeat Steps 2 and 3 for all other Rings.
        //
        //
        //  Input:
        //
        //     indata - LG map
        //
        //     rawmap - original Gaussian map
        //
        //      w, h - width, height of the image
        //
        //     rBlob - center BE
        //
        //     model - target model;   1: BC 8"
        //                             2: BC 6"
        // -----------------------------------------------------------------------------------------------------
        //
        //  Returns:
        //                      tracemap : marked by Trace Index
        //                        [ ]    : ZC strength
        //

        if (w * h < 25) return null;

        Log.d(TAG, " +++++++++++++++++ makeZCSMapMore() started ++++++++++++++++++++++");
        Log.d(TAG, "makeZCSMapMore() entered with ... w/h = " + w + " / " + h + " | BE x/y/xD/yD: " + rBlob.x + "/" + rBlob.y + "/" + rBlob.xDim + "/" + rBlob.yDim);

        final int         size = w * h;
        int            ring1x0 = -1;
        int            ring1y0 = -1;

        /////// NOT USED - should be removed!!!
        int[]              zcp = new int[size];      // ZC map

        boolean   RadiRing1Set = false;
        int[]        BBoxRing1 = new int[4];         // BBox of Ring 1 - to be used to restrain outer ring searches
        int[]       ring1Q1pos = new int[2];         //  Ring 1 Q1 Center Marker location
        int[]       ring1Q2pos = new int[2];         //  Ring 1 Q2 Center Marker location
        int[]       ring1Q3pos = new int[2];
        int[]       ring1Q4pos = new int[2];


//        Log.d(TAG, " -- makeZCSMapMore() original range: " + minzc + " - " + maxzc + " | Resolution = " + zchistoRes + "; Total: " + zcTotal);
//        System.out.println(Arrays.toString(zchisto));

        // get min/max of the raw image
        int  maxgbuf = 0;
        int  mingbuf = 999;
        for (int i = 0; i < size; i++) {
            if (maxgbuf < rawmap[i])      maxgbuf = rawmap[i];
            else if (mingbuf > rawmap[i]) mingbuf = rawmap[i];
        }

        Log.d(TAG, " -- makeZCSMapMore() rawmap min/max = "+mingbuf+"/"+maxgbuf);

        //// BE center; will be refined during process
        int   x0 = rBlob.x;
        int   y0 = rBlob.y;

        ///  TODO - supposedly to help restrain the search range!!


        int actRingxDim = -1; // active Ring dimension in X; may be the previous Ring (pre AP search,)or the current one (post AP search)
        int actRingyDim = -1; // active Ring dimension in Y; may be the previous Ring (pre AP search,)or the current one (post AP search)
        int actRingDim  = -1;

        // 1. determine the supposed scoring line distance from the BE center and
        //    the proper threshold for the scoring line detection.
        if (model == 1) { // BC 8"

            double[][] ringDist = new double[7][2]; // Edge Rings 1 to 7 size ratios; ring 0 (BE) is known
            double[]  ringSizes = new double[5];    // Target RingSize ratios
            double         radi = (rBlob.xDim + rBlob.yDim + 0.0001) / 4;
            final byte      b11 = (byte)11;
            final byte      b22 = (byte)22;
            Log.d(TAG, " - makeZCSMapMore() linear Rings distance ranges:");

            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 2; j++) {
                    ringDist[i][j] = sRanges[model][i + 1][j] * radi; // straight line search distance

              // sRange: {{0.8, 1.2}, {2.1, 2.6}, {2.2, 3.2}, {3.8, 5.0}, {3.9, 5.0}, {6.3, 7.95}, {7.1, 8.3}, {9.05, 11.25}}
                }
//                System.out.println(Arrays.toString(ringDist[i]));
            }

            for (int i = 0; i < 5; i++) { // skip 1st one; it's for array size
                ringSizes[i] = TargetRingSizes[1][i + 1];
            }

            Log.d(TAG, "\n");
            Log.d(TAG, " - makeZCSMapMore() Ring size ratios (center BE = 1):");
            System.out.println(Arrays.toString(ringSizes));

            //// refine BE first
            double  ratioBEtoR1 = 1.0;  // ratioBEtoR1 = ringSizes[0] / ringSizes[1];
            int     threshR5Bdr = -30;  // for Red BE
            boolean     markNbr = true; // false;
            int[]       BEQ1pos = new int[2];

            // make an estimate of the Q1 position for BE
            BEQ1pos[0] = x0 + (int)(rBlob.xDim * 0.727 * 0.5);
            BEQ1pos[1] = y0 - (int)(rBlob.xDim * 0.727 * 0.5);

            Log.d(TAG, "\n");
            Log.d(TAG, "++++ makeZCSMapMore() calling simpleEdgeFinder( "+w+", "+ h+", rawmap, rbuf, traceMap, "+ x0+", "+ y0
                    +", BEQ1pos, " + b11+" ,"+ b22+" ,"+ ratioBEtoR1+", "+ threshR5Bdr+" )" );

            Blob BEblob = SMPEdgeFinder(w, h, rawmap, rbuf, traceMap, x0, y0, BEQ1pos, b11, b22, ratioBEtoR1, threshR5Bdr, markNbr, true);

            if(BEblob.x > 0 && BEblob.y > 0) {
                x0 = BEblob.x;
                y0 = BEblob.y;

                Log.d(TAG, "++++ makeZCSMapMore() post-simpleEdgeFinder() updating x0/y0 to "+x0+"/"+y0);
            }

            double   RadiRing1 = 0.0;  // radius of Ring 1 - enclosing BE

            for (int ring = 0; ring < 3; ring++) {
                double threshPeak = (mingbuf - maxgbuf) * 0.125;    // must reset for each run!

                // search ranges for target rings; including center BE:
                //  sRange: {{0.8, 1.2}, {2.8, 3.5}, {3.0, 3.8}, {4.4, 5.7}, {4.75, 5.9}, {6.3, 7.95}, {7.1, 8.3}, {9.05, 11.25}}


                // 2. pick the starting Dark points for scoring line detection. We choose the 45-degree directions on purpose.

                ///   calculate the 45-degree distance in X and Y
                //----------------------------------------------
                //     < 1st quadrant > - upper right section
                final int      ringNo = ring + 1;
                final byte   ringMark = (byte) ((ring + 2) * 11);
                final byte  ringMarkn = (byte) ((ring + 3) * 11);

                if (ring == 0 && !RadiRing1Set) {
                    final double ringratio = ringSizes[1];
                    final double rdistance = (BEblob.xDim + BEblob.yDim) * ringratio * 0.25;
                    final int    sDist45   = (int) (rdistance * 0.727);

                    Log.d(TAG, "\n");
                    Log.d(TAG, " ----------- makeZCSMapMore() <Ring " + ringNo + "> Processing started; radius set to " + rdistance);
                    Log.d(TAG, " -------     makeZCSMapMore() using real dimensions -- ringratio / sDist45 = " + ringratio + " / "+sDist45+")");

                    final byte b33 = (byte)33;
                    ratioBEtoR1    = ringSizes[1];
                    threshR5Bdr    = (int)threshPeak;
                    markNbr        = true;
                    BEQ1pos[0]     = x0 + (int)(BEblob.xDim * 0.5 * 0.747);
                    BEQ1pos[1]     = y0 - (int)(BEblob.yDim * 0.5 * 0.747);

                    Blob BEblob1 = SMPEdgeFinder(w, h, rawmap, rbuf, traceMap, x0, y0, BEQ1pos, b22, b33, ratioBEtoR1, threshR5Bdr, markNbr, false);

                } else if (ring > 0 && RadiRing1Set) {
                    // use the actual known Ring1 radius for a more accurate search range
                    // For simplicity, we modify the ringDist[][] content

                    final double ringratio = ringSizes[ringNo] / ringSizes[1];
                    final double rdistance = RadiRing1 * ringratio;

                    Log.d(TAG, "\n");
                    Log.d(TAG, " ----------- makeZCSMapMore() <Ring " + ringNo + "> Processing started; radius set to " + rdistance);
                    Log.d(TAG, " -------     makeZCSMapMore() using real dimensions -- (RadiRing1 / ringratio = " + RadiRing1 + " / " + ringratio + ")");

                    ratioBEtoR1 = ringratio;
                    threshR5Bdr = (int)threshPeak;  // -35;
                    markNbr     = true; // false;

                    Blob BEblob1 = SMPEdgeFinder(w, h, rawmap, rbuf, traceMap, x0, y0, ring1Q1pos, ringMark, ringMarkn, ratioBEtoR1, threshR5Bdr, markNbr, false);

                } else {
                    Log.d(TAG, " XXX makeZCSMapMore() unknown setup condition for Ring "+ringNo+" - Quitting!!");
                    return zcp;
                }

                /// Mark wmp[]
                int ringxmin = 999, ringxmax = 0;
                int ringymin = 999, ringymax = 0;

                for (int i = 0; i < size; i++) {
                    if (traceMap[i] == ringMark) {
                        // estimate the radius for the next ring to use
                        if (i % w > ringxmax) ringxmax = i % w;
                        if (i % w < ringxmin) ringxmin = i % w;
                        if (i / w > ringymax) ringymax = i / w;
                        if (i / w < ringymin) ringymin = i / w;
                    }
                }

                actRingxDim = ringxmax - ringxmin;
                actRingyDim = ringymax - ringymin;

                Log.d(TAG, " -------------------  makeZCSMapMore() Ring " + ringNo + " Process Ended! X/Y Dimension = "+actRingxDim+"/"+actRingyDim);

                if (actRingxDim / (actRingyDim + 0.0001) > 1.3 || actRingyDim / (actRingxDim + 0.0001) > 1.3) {
                    Log.d(TAG, " !!!! makeZCSMapMore() Ring " + ringNo + " Aspect Ratio (dimy/dimx) too big: " + (actRingxDim / (actRingyDim + 0.0001)));
                }

                actRingDim = (int) ((actRingxDim + actRingyDim + 0.5) / 2);

                // Ring 1 Anchor Points recovery - these anchor points help recover the other rings of the template
                if (ring == 0) {
                    BBoxRing1[0] = ringxmin;
                    BBoxRing1[1] = ringymin;
                    BBoxRing1[2] = ringxmax;
                    BBoxRing1[3] = ringymax;

                    ring1x0      = (BBoxRing1[0] + BBoxRing1[2] + 1) / 2;
                    ring1y0      = (BBoxRing1[1] + BBoxRing1[3] + 1) / 2;

                    RadiRing1    = actRingDim / 2;
                    RadiRing1Set = true;

                    double q1sp = 999.9, q2sp = 999.9, q3sp = 999.9, q4sp = 999.9;

                    // find the 4 quadrant anchor points
                    // - Q1
                    int stt = (ringymin - 2) * w + ringxmin - 2;
                    int end = (ringymax + 2) * w + ringxmax + 2;
                    for (int i = stt; i < end; i++) {
                        // find closest ring points to the 4 quadrant center axises as defined by Slope
                        if (traceMap[i] == ringMark) {
                            int ctx = i % w;
                            int cty = i / w;
                            int delx = ctx - ring1x0;
                            int dely = cty - ring1y0;
                            double slopeD = Math.abs(Math.abs(dely) - Math.abs(delx));

//                            Log.d(TAG, " +++ makeZCSMapMore() Ring "+ringNo+" Anchor Points validating at: " + ctx + ", " + cty + "; slope = " + slopeD);

                            if (delx > 0 && dely < 0 && slopeD < q1sp) {
                                q1sp = slopeD;
                                ring1Q1pos[0] = ctx;
                                ring1Q1pos[1] = cty;
                            } else if (delx > 0 && dely > 0 && slopeD < q2sp) {
                                q2sp = slopeD;
                                ring1Q2pos[0] = ctx;
                                ring1Q2pos[1] = cty;
                            } else if (delx < 0 && dely > 0 && slopeD < q3sp) {
                                q3sp = slopeD;
                                ring1Q3pos[0] = ctx;
                                ring1Q3pos[1] = cty;
                            } else if (delx < 0 && dely < 0 && slopeD < q4sp) {
                                q4sp = slopeD;
                                ring1Q4pos[0] = ctx;
                                ring1Q4pos[1] = cty;
                            }
                        }
                    }
                    Log.d(TAG, " +++ makeZCSMapMore() <Ring 1 Final Anchor Points>: " + ring1Q1pos[0] + ", " + ring1Q1pos[1] + " / "
                            + ring1Q2pos[0] + ", " + ring1Q2pos[1] + " / " + ring1Q3pos[0] + ", " + ring1Q3pos[1] + " / "
                            + ring1Q4pos[0] + ", " + ring1Q4pos[1] + " / ");
                }

                //  TODO - close small gaps
                //   remove single pixels and use 4-nbrs to grow once and close small gaps






            } // for Rings 1 to 3

            // Attack the boarder Ring 4
            final byte        b55 = 55;
            final byte       bn11 = (byte)-11;
            double     ratior41   = ringSizes[4] / ringSizes[1];
            double     threshPeak = (mingbuf - maxgbuf) * 0.15;
            ratioBEtoR1 = ratior41;        // double  ratioBEtoR1 = ratior41;
            threshR5Bdr = (int)-(threshPeak * 1.5);
            markNbr     = true;                // boolean     markNbr = true; // false;

            Log.d(TAG, "\n");
            Log.d(TAG, "++++ makeZCSMapMore() calling simpleEdgeFinder( "+w+", "+ h+", rawmap, traceMap, "+ ring1x0+", "+ ring1y0
                       +", ring1Q1pos, " + bn11+" ,"+ ratioBEtoR1+", "+ threshR5Bdr+" )" );

            Blob BEblob1 = SMPEdgeFinder(w, h, rawmap, rbuf, traceMap, ring1x0, ring1y0, ring1Q1pos, b55, bn11, ratioBEtoR1, threshR5Bdr, markNbr, false);

        } // Model 1

        return zcp;
    }

    private Blob SMPEdgeFinder(int w, int h, int[] rawmap, int[] rbuf, byte[] traceMap, int ring1x0, int ring1y0,  int[] ring1Q1pos,
                                     byte ringMark, byte ringMarkn, double ratiorToRef,  int thresh, boolean markNbr, boolean isBE)
    {
        // Circular Edge Contour Finder based on a known concentric ring
        //
        //  - Input -
        //
        //              w, h : frame width and height
        //             rawmap: smoothed raw image in 1D format
        //       < traceMap >: 1D map storing the result using label 'ringMark'
        //           ringmark: a 2-digit Byte label to mark the ring of interest
        //   ring1x0, ring1y0:  center location of the known reference ring
        //         ring1Q1pos: stores the location [x, y] of the known point of ring1 in the first quadrant;
        //                     to be used to help quickly find the first point of the new ring
        //         ratioToRef: radius ratio of the new ring to the known reference ring1
        //            markNbr: want to mark the outer region by ringMarkn
        //               isBE: function is called for identify BE; this will ignore the rbuf[] zone avoidance check
        //
        //  - Output -
        //
        //           traceMap: 1D map storing the result using label 'ringMark'
        //

        // first find the 4 Quadrant Center Ring Markers using the known scale ratio to Ring 1
        Log.d(TAG, "+ simpleEdgeFinder() entered with x / y / w / h / ringMark / nbrringmark / threshold / ratio = "+ ring1x0 +" / "+
                ring1y0+ " / "+w+" / "+h+" / "+ ringMark+" / "+ ringMarkn+" / "+ thresh+" / "+ratiorToRef);

        Blob           mBlob = new Blob();
        final int       size = w*h;
        double    threshPeak = (double)thresh;
        int[]      rnewQ1pos = new int[2]; //  New Ring Q1 Center Marker
        int[]      rnewQ2pos = new int[2]; //  New Ring Q2 Center Marker
        int[]      rnewQ3pos = new int[2];
        int[]      rnewQ4pos = new int[2];

        // set the marker to search start position; we use only ring1Q1pos[] because it was setup properly!
        rnewQ1pos[0] = ring1x0 + (int) ((ring1Q1pos[0] - ring1x0) * ratiorToRef);
        rnewQ1pos[1] = ring1y0 + (int) ((ring1Q1pos[1] - ring1y0) * ratiorToRef);
        rnewQ2pos[0] = ring1x0 + (int) ((ring1Q1pos[0] - ring1x0) * ratiorToRef);
        rnewQ2pos[1] = ring1y0 - (int) ((ring1Q1pos[1] - ring1y0) * ratiorToRef);
        rnewQ3pos[0] = ring1x0 - (int) ((ring1Q1pos[0] - ring1x0) * ratiorToRef);
        rnewQ3pos[1] = ring1y0 - (int) ((ring1Q1pos[1] - ring1y0) * ratiorToRef);
        rnewQ4pos[0] = ring1x0 - (int) ((ring1Q1pos[0] - ring1x0) * ratiorToRef);
        rnewQ4pos[1] = ring1y0 + (int) ((ring1Q1pos[1] - ring1y0) * ratiorToRef);

        Log.d(TAG, "+ simpleEdgeFinder() Anchor Point start pos: " + rnewQ1pos[0] + ", " + rnewQ1pos[1] + "; "
                + rnewQ2pos[0] + ", " + rnewQ2pos[1] + "; " + rnewQ3pos[0] + ", " + rnewQ3pos[1] + "; "
                + rnewQ4pos[0] + ", " + rnewQ4pos[1]);

        // Trace and complete the Ring Circle Sections starting from each of the Ring Anchor Points
        int     sRange = 23;
        Log.d(TAG, "+ simpleEdgeFinder() AP search range = "+sRange);

        // Double-side Continuity Enforced Search Method; we want to locate the transition point of greatest strength
        //
        // Note - relationship between tloc and tlocm2 must remain the same
        // - Q1 -
        int fndIdx      = -1;
        int baseloc     = rnewQ1pos[1] * w + rnewQ1pos[0];
        int searchLen   = Math.min(w, h) / 12;               // frame size restricted
        int shift1      = w - 1;
        int measOffset  = -3*w + 3;                          // intensity measurement offset
        int ii = -1, tloc, tlocm2, tBEloc;
        boolean sidePos      = true;                         //  location is away from center
        boolean sideBEGo     = true;                         //  center-direction search OK
        double  brightDif    = threshPeak;

        if(searchLen < 14) searchLen = 14;

        Log.d(TAG, " +++ simpleEdgeFinder() AP search threshPeak / searchLen = "+brightDif+" / "+searchLen);

        // Double-side Continuity Enforced Search Method
        for (int i = 0; i < searchLen; i++) {
            // looking for the first down edge
            tloc   = baseloc - shift1 * i;
            tlocm2 = tloc + measOffset;
            tBEloc = tloc - measOffset;  // for BE region detection

//            Log.d(TAG, " +++ simpleEdgeFinder() AP search step "+i+" comparing: "+rawmap[tlocm2]+" & "+rawmap[tloc]);

            if (!isBE && rbuf[tBEloc] > 1) {
                sideBEGo = false;
//                    Log.d(TAG, " +++ simpleEdgeFinder() AP3 search B side stopping...");
                continue;
            } else if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                brightDif = rawmap[tlocm2] - rawmap[tloc];
                fndIdx  = tlocm2;
                sidePos = false;
                ii = i;
                break;
            }


            // BE Side search
            if(sideBEGo) {
                tloc   = baseloc + shift1 * i;
                tlocm2 = tloc + measOffset;
                tBEloc = tloc - measOffset * 3;

//                Log.d(TAG, " +++ simpleEdgeFinder() AP search B step " + i + " comparing: " + rawmap[tlocm2] + " & " + rawmap[tloc]);
                if (rbuf[tBEloc] > 1) {
                    sideBEGo = false;
//                    Log.d(TAG, " +++ simpleEdgeFinder() AP search B side stopping...");
                    continue;
                } else if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                    brightDif = rawmap[tlocm2] - rawmap[tloc];
                    fndIdx    = tlocm2;
                    sidePos   = false;
                    ii        = i;
                    break;
                }
            }
        }

        if (fndIdx > -1) { // continue on to find the real down transition point in the neighborhood
            Log.d(TAG, " +++ simpleEdgeFinder() Q1 Org Anchor Point: " + (fndIdx % w - 1) + ", " + (fndIdx / w + 1) + " ; strength = " + brightDif);

            shift1 = (sidePos)? -(w-1) : w-1;
            for (int j = ii+1; j < ii+6; j++) {
                tloc   = baseloc + shift1*j;
                tlocm2 = tloc + measOffset;

                if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                    fndIdx     = tlocm2;
                    brightDif  = rawmap[tlocm2] - rawmap[tloc];
                } else {
                    break;  // down slope
                }
            }
            int  nx = fndIdx % w;
            int  ny = fndIdx / w;
            rnewQ1pos[0] = nx;
            rnewQ1pos[1] = ny;

            Log.d(TAG, " +++ simpleEdgeFinder() Q1 Anchor Point found: " + rnewQ1pos[0] + ", " + rnewQ1pos[1] + " ; strength = " + brightDif);

            traceMap[fndIdx]         = ringMark;
            traceMap[fndIdx - w - 1] = ringMarkn;
/* for debug
            traceMap[fndIdx - 1]         = ringMark;
            traceMap[fndIdx + 1]         = ringMark;
            traceMap[fndIdx - w]         = ringMark;
            traceMap[fndIdx + w]         = ringMark;
            if(traceMap[fndIdx - w - 1 - 1] < 1) traceMap[fndIdx - w - 1 - 1] = ringMarkn;
            if(traceMap[fndIdx - w - 1 + 1] < 1) traceMap[fndIdx - w - 1 + 1] = ringMarkn;
            if(traceMap[fndIdx - w - 1 - w] < 1) traceMap[fndIdx - w - 1 - w] = ringMarkn;
            if(traceMap[fndIdx - w - 1 + w] < 1) traceMap[fndIdx - w - 1 + w] = ringMarkn;
*/
        } else {
            Log.d(TAG, " XXX simpleEdgeFinder() Q1 Anchor Point search failed!");
        }

        // - Q2 -
        fndIdx      = -1;
        baseloc     = rnewQ2pos[1] * w + rnewQ2pos[0];
        measOffset  = 3*w + 3;                     // intensity measurement offset
        shift1      = w + 1;
        sidePos     = true;                        //  location is away from center
        sideBEGo    = true;
        brightDif   = threshPeak;
        ii          = -1;

        for (int i = 0; i < searchLen; i++) { // looking for the first down edge
            tloc   = baseloc + shift1*i;
            tlocm2 = tloc + measOffset;

            if(tlocm2 < size) {
//                Log.d(TAG, " +++ simpleEdgeFinder() AP2 search step " + i + " comparing: " + rawmap[tlocm2] + " & " + rawmap[tloc]);
                if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                    brightDif = rawmap[tlocm2] - rawmap[tloc];
                    fndIdx = tlocm2;   // point at the outer side
                    ii = i;
                    break;
                }
            }

            // BE Side search
            if(sideBEGo) {
                tloc = baseloc - shift1 * i;
                tlocm2 = tloc + measOffset;
                tBEloc = tloc - measOffset * 3;

//                Log.d(TAG, " +++ simpleEdgeFinder() AP2 search B step " + i + " comparing: " + rawmap[tlocm2] + " & " + rawmap[tloc]);
                if (!isBE && rbuf[tBEloc] > 1) {
                    sideBEGo = false;
//                    Log.d(TAG, " +++ simpleEdgeFinder() AP2 search B side stopping...");
                    continue;
                } else if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                    brightDif = rawmap[tlocm2] - rawmap[tloc];
                    fndIdx = tlocm2;
                    sidePos = false;
                    ii = i;
                    break;
                }
            }
        }

        if (fndIdx > -1) { // continue on to find the real down transition point in the neighborhood
            Log.d(TAG, " +++ simpleEdgeFinder() Q2 Org Anchor Point: " + (fndIdx % w - 1) + ", " + (fndIdx / w - 1) + " ; strength = " + brightDif);

            shift1 = (sidePos)? w+1 : -(w+1);
            for (int j = ii+1; j < ii+6; j++) {
                tloc   = baseloc + shift1*j;
                tlocm2 = tloc + measOffset;

                if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                    fndIdx     = tlocm2;
                    brightDif  = rawmap[tlocm2] - rawmap[tloc];
                } else {
                    break;  // down slope
                }
            }

            int  nx = fndIdx % w;
            int  ny = fndIdx / w;
            rnewQ2pos[0] = nx;
            rnewQ2pos[1] = ny;

            Log.d(TAG, " +++ simpleEdgeFinder() Q2 Anchor Point found: " + rnewQ2pos[0] + ", " + rnewQ2pos[1] + " ; strength = " + brightDif);

            traceMap[fndIdx]         = ringMark;
            traceMap[fndIdx - w - 1] = ringMarkn;
/* for debug
            traceMap[fndIdx - 1]         = ringMark;
            traceMap[fndIdx + 1]         = ringMark;
            traceMap[fndIdx - w]         = ringMark;
            traceMap[fndIdx + w]         = ringMark;
            if(traceMap[fndIdx - w - 1 - 1] < 1) traceMap[fndIdx - w - 1 - 1] = ringMarkn;
            if(traceMap[fndIdx - w - 1 + 1] < 1) traceMap[fndIdx - w - 1 + 1] = ringMarkn;
            if(traceMap[fndIdx - w - 1 - w] < 1) traceMap[fndIdx - w - 1 - w] = ringMarkn;
            if(traceMap[fndIdx - w - 1 + w] < 1) traceMap[fndIdx - w - 1 + w] = ringMarkn;
*/
        } else {
            Log.d(TAG, " XXX simpleEdgeFinder() Q2 Anchor Point search failed!");
        }

        // - Q3 -
        fndIdx      = -1;
        baseloc     = rnewQ3pos[1] * w + rnewQ3pos[0];
        measOffset  = 3*w - 3;                     // intensity measurement offset
        shift1      = w-1;
        sidePos     = true;                        //  location is away from center
        sideBEGo    = true;
        brightDif   = threshPeak;
        ii          = -1;

        for (int i = 0; i < searchLen; i++) { // looking for the first down edge
            tloc   = baseloc + shift1*i;
            tlocm2 = tloc + measOffset;

            Log.d(TAG, " +++ simpleEdgeFinder() AP3 search step "+i+" comparing: "+rawmap[tlocm2]+" & "+rawmap[tloc]);
            if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                brightDif = rawmap[tlocm2] - rawmap[tloc];
                fndIdx    = tlocm2;   // point at the outer side
                ii        = i;
                break;
            }

            // BE Side search
            if(sideBEGo) {
                tloc   = baseloc - shift1 * i;
                tlocm2 = tloc + measOffset;
                tBEloc = tloc - measOffset * 3;

                        Log.d(TAG, " +++ simpleEdgeFinder() AP3 search B step " + i + " comparing: " + rawmap[tlocm2] + " & " + rawmap[tloc]);
                if (!isBE && rbuf[tBEloc] > 1) {
                    sideBEGo = false;
                    Log.d(TAG, " +++ simpleEdgeFinder() AP3 search B side stopping...");
                    continue;
                } else if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                    brightDif = rawmap[tlocm2] - rawmap[tloc];
                    fndIdx  = tlocm2;
                    sidePos = false;
                    ii = i;
                    break;
                }
            }
        }

        if (fndIdx > -1) { // continue on to find the real down transition point in the neighborhood
            Log.d(TAG, " +++ simpleEdgeFinder() Q3 Org Anchor Point: " + (fndIdx % w) + ", " + (fndIdx / w) + " ; strength = " + brightDif);

            shift1 = (sidePos)? w-1 : -(w-1);
            for (int j = ii+1; j < ii+6; j++) {
                tloc   = baseloc + shift1*j;
                tlocm2 = tloc + measOffset;

                if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                    fndIdx     = tlocm2;
                    brightDif  = rawmap[tlocm2] - rawmap[tloc];
                } else {
                    break;  // down slope
                }
            }

            rnewQ3pos[0] = fndIdx % w + 1;
            rnewQ3pos[1] = fndIdx / w - 1;

            Log.d(TAG, " +++ simpleEdgeFinder() Q3 Anchor Point found: " + rnewQ3pos[0] + ", " + rnewQ3pos[1] + " ; strength = " + brightDif);

            traceMap[fndIdx]         = ringMark;
            traceMap[fndIdx - w - 1] = ringMarkn;
/* for debug
            traceMap[fndIdx - 1]         = ringMark;
            traceMap[fndIdx + 1]         = ringMark;
            traceMap[fndIdx - w]         = ringMark;
            traceMap[fndIdx + w]         = ringMark;
            if(traceMap[fndIdx - w - 1 - 1] < 1) traceMap[fndIdx - w - 1 - 1] = ringMarkn;
            if(traceMap[fndIdx - w - 1 + 1] < 1) traceMap[fndIdx - w - 1 + 1] = ringMarkn;
            if(traceMap[fndIdx - w - 1 - w] < 1) traceMap[fndIdx - w - 1 - w] = ringMarkn;
            if(traceMap[fndIdx - w - 1 + w] < 1) traceMap[fndIdx - w - 1 + w] = ringMarkn;
*/
        } else {
            Log.d(TAG, " XXX simpleEdgeFinder() Q3 Anchor Point search failed!");
        }

        // - Q4 -
        fndIdx      = -1;
        baseloc     = rnewQ4pos[1] * w + rnewQ4pos[0];
        measOffset  = -3*w - 3;                     // intensity measurement offset
        shift1      = -(w + 1);
        sidePos     = true;                        //  location is away from center
        sideBEGo    = true;
        brightDif   = threshPeak;
        ii          = -1;

        for (int i = 0; i < searchLen; i++) { // looking for the first down edge
            tloc   = baseloc + shift1*i;
            tlocm2 = tloc + measOffset;

            if(tlocm2 > 0) {
//                Log.d(TAG, " +++ simpleEdgeFinder() AP4 search step " + i + " comparing: " + rawmap[tlocm2] + " & " + rawmap[tloc]);
                if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                    brightDif = rawmap[tlocm2] - rawmap[tloc];
                    fndIdx = tlocm2;   // point at the outer side
                    ii = i;
                    break;
                }
            }

            // BE Side search
            if(sideBEGo) {
                tloc   = baseloc - shift1 * i;
                tlocm2 = tloc + measOffset;
                tBEloc = tloc - measOffset * 3;

//                Log.d(TAG, " +++ simpleEdgeFinder() AP4 search B step "+i+" comparing: "+rawmap[tlocm2]+" & "+rawmap[tloc]);
                if (!isBE && rbuf[tBEloc] > 1) {
                    sideBEGo = false;
//                    Log.d(TAG, " +++ simpleEdgeFinder() AP4 search B side stopping...");
                    continue;
                } else if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                    brightDif = rawmap[tlocm2] - rawmap[tloc];
                    fndIdx  = tlocm2;
                    sidePos = false;
                    ii      = i;
                    break;
                }
            }
        }

        if (fndIdx > -1) { // continue on to find the real down transition point in the neighborhood
            Log.d(TAG, " +++ simpleEdgeFinder() Q4 Org Anchor Point: " + (fndIdx % w + 1) + ", " + (fndIdx / w + 1) + " ; strength = " + brightDif);

            shift1 = (sidePos)? -(w+1) : (w+1);
            for (int j = ii+1; j < ii+6; j++) {
                tloc   = baseloc + shift1*j;
                tlocm2 = tloc + measOffset;

                if ((rawmap[tlocm2] - rawmap[tloc]) / brightDif > 1.0) {
                    fndIdx     = tlocm2;
                    brightDif  = rawmap[tlocm2] - rawmap[tloc];
                } else { break; } // down slope
            }

            rnewQ4pos[0] = fndIdx % w + 1;
            rnewQ4pos[1] = fndIdx / w + 1;

            Log.d(TAG, " +++ simpleEdgeFinder() Q4 Anchor Point found: " + rnewQ4pos[0] + ", " + rnewQ4pos[1] + " ; strength = " + brightDif);

            traceMap[fndIdx]         = ringMark;
            traceMap[fndIdx - w - 1] = ringMarkn;
/* for debug
            traceMap[fndIdx - 1]         = ringMark;
            traceMap[fndIdx + 1]         = ringMark;
            traceMap[fndIdx - w]         = ringMark;
            traceMap[fndIdx + w]         = ringMark;
            if(traceMap[fndIdx - w - 1 - 1] < 1) traceMap[fndIdx - w - 1 - 1] = ringMarkn;
            if(traceMap[fndIdx - w - 1 + 1] < 1) traceMap[fndIdx - w - 1 + 1] = ringMarkn;
            if(traceMap[fndIdx - w - 1 - w] < 1) traceMap[fndIdx - w - 1 - w] = ringMarkn;
            if(traceMap[fndIdx - w - 1 + w] < 1) traceMap[fndIdx - w - 1 + w] = ringMarkn;
*/
        } else {
            Log.d(TAG, " XXX simpleEdgeFinder() Q4 Anchor Point search failed!");
        }

        Log.d(TAG, " >>>>> simpleEdgeFinder() APs: "+rnewQ1pos[0] + ", " + rnewQ1pos[1]+"/"+rnewQ2pos[0] + ", " + rnewQ2pos[1]+"/"
                +rnewQ3pos[0] + ", " + rnewQ3pos[1]+"/"+rnewQ4pos[0] + ", " + rnewQ4pos[1]);

        // Trace around the white circle (ring) to complete the Ring
        // We move clockwise from one anchor point to meet the next. Full ring is recovered after all 4 A.P. aew done

        threshPeak *= 0.7;
        // ** AP 1 in Q1; direction down **
        int      maxsteps = (rnewQ2pos[1] - rnewQ1pos[1]) * 6 / 5; // 1.2 X vertical travel
        int      shift, ploc;
        int      rnx      = rnewQ1pos[0];
        int      rny      = rnewQ1pos[1];
        boolean  q1stop   = false;
        boolean  isFirst  = true, onStill;

        Log.d(TAG, "+++simpleEdgeFinder() Q1 tracing started @: " + rnx + ", " + (rny + 1) + "; maxsteps = " + maxsteps);
        for (int i = rnewQ1pos[1] + 1; i <= rnewQ2pos[1] && maxsteps > 0 && !q1stop; ++i, --maxsteps) { // check downward 5-neighbor for peak
            onStill = false;
            for (int k = -5; k <= 5; k++) {
                int cpos   = i * w + rnx + k;
                int cposm2 = cpos + 3; // closer to center
//                Log.d(TAG, "  simpleEdgeFinder() comparing: "+rawmap[cposm2]+" - "+rawmap[cpos]+" over "+threshPeak);
                if (((rawmap[cposm2] - rawmap[cpos]) / threshPeak) > 1.0) { // found it
                    ploc           = cpos;
                    onStill        = true;
                    shift          = k;
                    double  maxDif = rawmap[cposm2] - rawmap[cpos];
                    int     mpos, npm2;
                    for(int j=k+1; j<k+3; j++) { // continue 3 more steps to get the max peak
                        mpos = i * w + rnx + j; ////// mpos    = i * w + rnewQ1pos[0] + j;
                        npm2 = mpos + 3;
                        if ((rawmap[npm2] - rawmap[mpos]) / maxDif > 1.0) {
                            maxDif = rawmap[npm2] - rawmap[mpos];
                            ploc   = mpos;
                            shift  = j;
                        } else { break; }
                    }

                    traceMap[ploc + 1]               = ringMark; // consistent with AP localization
                    rnx                             += (shift-1);
                    if (markNbr) traceMap[ploc + 2]  = ringMarkn;

                    if(!isFirst) { // don't apply to the first trace point!
                        if (shift > 1) { // leave no gaps!
//                        Log.d(TAG, "  simpleEdgeFinder() filling gap for shift = "+shift);

                            for (int j = 1; j < shift; j++) {
                                traceMap[ploc + 1 - j] = ringMark;
                                if (markNbr && traceMap[ploc + 2 - j] < 1)
                                    traceMap[ploc + 2 - j] = ringMarkn;
                            }
                        } else if (shift < -1) {
//                        Log.d(TAG, "  simpleEdgeFinder() filling gap for shift = "+shift);

                            for (int j = shift + 1; j < 0; j++) {
                                traceMap[ploc - j] = ringMark;
                                if (markNbr && traceMap[ploc + 1 - j] < 1)
                                    traceMap[ploc + 1 - j] = ringMarkn;
                            }
                        }
                    }
//                    Log.d(TAG, "simpleEdgeFinder() Q1 closing extended to x/y = " + rnx + " / " + i);
                    break;
                }
            }
            isFirst = false;

            if (!onStill) {
                q1stop       = true;
                Log.d(TAG, "simpleEdgeFinder() Q1 closing stopped at x/y = " + rnx + " / " + i);
            }
        }

        // AP 2 in Q2; direction left
        maxsteps   = (rnewQ2pos[0] - rnewQ3pos[0]) * 6 / 5; // 1.2 X vertical travel
        measOffset = 3*w;
        rnx        = rnewQ2pos[0];
        rny        = rnewQ2pos[1];
        isFirst    = true;
        boolean  q2stop = false;

        Log.d(TAG, "+++ simpleEdgeFinder() Q2 tracing started @: " + (rnx-1) + ", " + rny + "; maxsteps = " + maxsteps);
        for (int i = rnewQ2pos[0] - 1; i >= rnewQ3pos[0] && maxsteps > 0 && !q2stop; --i, --maxsteps) {
            onStill = false;
            for (int k = -5; k <= 5; k++) {
                int cpos   = (rny + k) * w + i;
                int cposm2 = cpos + measOffset; // lower-neighbor; away from center

//                Log.d(TAG, "+++ simpleEdgeFinder() step "+i+" comparing "+rawmap[cposm2]+" vs "+rawmap[cpos]);

                if ((rawmap[cposm2] - rawmap[cpos]) / threshPeak > 1.0) { // found it
                    ploc = cpos;
                    onStill = true;
                    shift = k;
                    double maxDif = rawmap[cposm2] - rawmap[cpos];
                    int mpos, npm2;
                    for (int j = k + 1; j < k + 3; j++) { // continue 3 more steps to get the max peak
                        mpos = (rny + j) * w + i;
                        npm2 = mpos + measOffset;
                        if (((rawmap[npm2] - rawmap[mpos]) / maxDif) > 1.0) {
                            maxDif = rawmap[npm2] - rawmap[mpos];
                            ploc = mpos;
                            shift = j;
                        } else {
                            break;
                        }
                    }
                    rny += (shift + 1);
                    traceMap[ploc + w] = ringMark;
                    if (markNbr) traceMap[ploc + w * 2] = ringMarkn;

                    if (!isFirst) {
                        if (shift > 1) { // leave no gaps!
                            for (int j = 1; j < shift; j++) {
                                traceMap[(rny + j) * w + i] = ringMark;
                                if (markNbr && traceMap[(rny + j + 1) * w + i] < 1)
                                    traceMap[(rny + j + 1) * w + i] = ringMarkn;
                            }
                        } else if (shift < -1) {
                            for (int j = shift + 2; j < 0; j++) {
                                traceMap[(rny + j) * w + i] = ringMark;
                                if (markNbr && traceMap[(rny + j + 1) * w + i] < 1)
                                    traceMap[(rny + j + 1) * w + i] = ringMarkn;
                            }
                        }
                    }
                    //                    Log.d(TAG, "simpleEdgeFinder() Q2 closing extended to x/y = " + i + " / " + rny);
                    break;
                }
            }
            isFirst = false;

            if (!onStill) {
                q2stop       = true;
//                        ringQ2pos[0] = i + 1;
                Log.d(TAG, "simpleEdgeFinder() Q2 closing stopped at x/y = " + i + " / " + rny);
            }
        }

        // AP 3 in Q3; direction up
        maxsteps       = (rnewQ3pos[1] - rnewQ4pos[1]) * 6 / 5; // 1.2 X vertical travel
        measOffset     = -3;
        rnx            = rnewQ3pos[0];
        rny            = rnewQ3pos[1];
        isFirst        = true;
        boolean q3stop = false;

        Log.d(TAG, "+++ simpleEdgeFinder() Q3 tracing started @: " + rnx + ", " + (rny - 1) + "; maxsteps = " + maxsteps);

        for (int i = rnewQ3pos[1] - 1; i >= rnewQ4pos[1] && maxsteps > 0 && !q3stop; --i, --maxsteps) {
            onStill = false;
            for (int k = -5; k <= 5; k++) {
                int cpos   = i * w + rnx + k;
                int cposm2 = cpos + measOffset;

//                        Log.d(TAG, "  makeZCSMapMore() comparing: "+rawmap[cposm2]+" - "+rawmap[cpos]+" over "+threshPeak);
                if ((rawmap[cposm2] - rawmap[cpos]) / threshPeak > 1.0) { // found it
                    ploc           = cposm2;
                    onStill        = true;
                    shift          = k;
                    double  maxDif = rawmap[cposm2] - rawmap[cpos];
                    int     mpos, npm2;
                    for(int j=k+1; j<k+3; j++) { // continue 3 more steps to get the max peak
                        mpos    = i * w + rnx + j;
                        npm2    = mpos + measOffset;
                        if (((rawmap[npm2] - rawmap[mpos]) / maxDif) > 1.0) {
                            maxDif = rawmap[npm2] - rawmap[mpos];
                            ploc   = npm2;
                            shift  = j;
                        } else { break; }
                    }
                    rnx                            += shift;
                    traceMap[ploc + 2]              = ringMark;  // traceMap[ploc + 1]  = ringMark;
                    if(markNbr) traceMap[ploc + 1]  = ringMarkn; // traceMap[ploc]      = ringMarkn;

                    if(!isFirst) {
                        if(shift > 1) { // leave no gaps!
                            for (int j = 1; j < shift; j++) {
                                traceMap[ploc + w - j]  = ringMark;  // traceMap[ploc + w + 2 + j]  = ringMark;
                                if (markNbr && traceMap[ploc + w - 1 - j] < 1) traceMap[ploc + w - 1 - j] = ringMarkn;
                            }
                        } else if(shift < -1) {
                            for (int j = shift + 1; j < 0; j++) {
                                traceMap[ploc + w - j]  = ringMark;  // traceMap[ploc + w + 2 + j]  = ringMark;
                                if (markNbr && traceMap[ploc + w - 1 - j] < 1) traceMap[ploc + w - 1 - j] = ringMarkn;
                            }
                        }
                    }
//                    Log.d(TAG, "simpleEdgeFinder() Q3 closing extended to x/y = " + rnx + " / " + i +"; shift = "+shift);
                    break;
                }
            }
            isFirst = false;
            if (!onStill) {
                q3stop       = true;
//                        ringQ3pos[1] = i;
                Log.d(TAG, "simpleEdgeFinder() Q3 closing stopped at x/y = " + rnx + " / " + rny);
            }
        }

        // Q4 Arch; direction right
        maxsteps       = (rnewQ1pos[0] - rnewQ4pos[0]) * 6 / 5;
        measOffset     = -3 * w;
        rnx            = rnewQ4pos[0];
        rny            = rnewQ4pos[1];
        isFirst        = true;
        boolean q4stop = false;

        Log.d(TAG, "+++ simpleEdgeFinder() Q4 tracing started @: " + (rnx + 1) + ", " + rny + "; maxsteps = " + maxsteps);

        for (int i = rnewQ4pos[0] + 1; i <= rnewQ1pos[0] && maxsteps > 0 && !q4stop; ++i, --maxsteps) {
            // moving along X & check rightward neighbors
            onStill = false;
            for (int k = -5; k <= 5; k++) {
                int cpos   = i + w * (rny + k);
                int cposm2 = cpos + measOffset;

//                        Log.d(TAG, "  simpleEdgeFinder() comparing: "+rawmap[cposm2]+" - "+rawmap[cpos]+" over "+threshPeak);
                if ((rawmap[cposm2] - rawmap[cpos]) / threshPeak > 1.0) { // found it
                    ploc           = cpos;
                    onStill        = true;
                    shift          = k;
                    double  maxDif = rawmap[cposm2] - rawmap[cpos];
                    int     mpos, npm2;
                    for(int j=k+1; j<k+3; j++) { // continue 3 more steps to get the max peak
                        mpos    = i + w * (rny + j);
                        npm2    = mpos + measOffset;
                        if (((rawmap[npm2] - rawmap[mpos]) / maxDif) > 1.0) {
                            maxDif = rawmap[npm2] - rawmap[mpos];
                            ploc   = mpos;
                            shift  = j;
                        } else { break; }
                    }
                    rny                               += shift;
                    traceMap[ploc - w]                 = ringMark;
                    if (markNbr) traceMap[ploc - w*2]  = ringMarkn;

                    if(!isFirst) {
                        if (shift > 1) { // leave no gaps!
                            for (int j = 1; j < shift; j++) {
                                traceMap[ploc - w * j] = ringMark; // traceMap[ploc + w + w*j]  = ringMark;
                                if (markNbr && traceMap[ploc - w * j + j] < 1)
                                    traceMap[ploc - w * j + j] = ringMarkn;
                            }
                        } else if (shift < -1) {
                            for (int j = shift + 1; j < 0; j++) {
                                traceMap[ploc - w * j] = ringMark; // traceMap[ploc + w + w*j]  = ringMark;
                                if (markNbr && traceMap[ploc + -w * j + j] < 1)
                                    traceMap[ploc - w * j + j] = ringMarkn;
                            }
                        }
                    }
//                    Log.d(TAG, "simpleEdgeFinder() Q4 closing extended to x/y = " + i + " / " + rny);

                    break;
                }
            }
            isFirst = false;
            if (!onStill) {
                q4stop = true;

                Log.d(TAG, "simpleEdgeFinder() Q4 closing stopped at x/y = " + i + " / " + rny);
            }
        }

        int xmin = 999, ymin = 999;
        int xmax = 0,   ymax = 0;
        final int w2   = 2*w;
        for(int i=0; i<size; i++) {
            if(traceMap[i] == ringMark) {
                if(i%w > xmax)       xmax = i%w;
                else if (i%w < xmin) xmin = i%w;
                if(i/w > ymax)       ymax = i/w;
                else if (i/w < ymin) ymin = i/w;

                // mark prohibit pixels in rbuf
                rbuf[i]     = ringMark;
                rbuf[i + 1] = ringMark;
                rbuf[i - 1] = ringMark;
                rbuf[i + w] = ringMark;
                rbuf[i + w + 1]  = ringMark;
                rbuf[i + w - 1]  = ringMark;
                rbuf[i - w]      = ringMark;
                rbuf[i - w + 1]  = ringMark;
                rbuf[i - w - 1]  = ringMark;
                rbuf[i + 2]      = ringMark;
                rbuf[i - 2]      = ringMark;
                rbuf[i + w2]     = ringMark;
                rbuf[i + w2 + 2] = ringMark;
                rbuf[i + w2 - 2] = ringMark;
                rbuf[i - w2]     = ringMark;
                rbuf[i - w2 + 2] = ringMark;
                rbuf[i - w2 - 2] = ringMark;
            }
        }

        mBlob.x    = (xmax + xmin) / 2;
        mBlob.y    = (ymax + ymin) / 2;
        mBlob.xDim = xmax - xmin;
        mBlob.yDim = ymax - ymin;

        return mBlob;
    } // simpleEdgeFinder()

    public void  cleanupSPlate(int targetModel, byte[] ringMap, int w, int h) {
        /// Remove isolated ringMap pixels and etc.

        Log.d(TAG, "cleanupSPlate() entered...");

        final int size      = w*h;
        final int wm        = w - 1;
        final int hm        = h - 1;
        final int safeStart = w + 1;
        final int limit = size - w - 1;

        byte[]  nbr1 = new byte[size];
        byte[]  nbr2 = new byte[size];
        byte[]  nbr3 = new byte[size];
        byte[]  nbr4 = new byte[size];
        byte[]  nbr5 = new byte[size];
        byte[]  nbr6 = new byte[size];
        byte[]  nbr7 = new byte[size];
        byte[]  nbr8 = new byte[size];

        for (int i = safeStart; i < limit; i++) {
            nbr1[i] = ringMap[i - w];
            nbr2[i] = ringMap[i - w + 1];
            nbr3[i] = ringMap[i + 1];
            nbr4[i] = ringMap[i + w + 1];
            nbr5[i] = ringMap[i + w];
            nbr6[i] = ringMap[i + w - 1];
            nbr7[i] = ringMap[i - 1];
            nbr8[i] = ringMap[i - w - 1];
        }

        if(targetModel == 1) {
            for (int j=1; j<=5; j++) { // expect 11, 22, 33, 44, 55 and -11
                int      mark = j*11;
                byte[]  nbrNo = new byte[size];

                for(int k=0; k<size; k++) {
                    if(nbr1[k] == mark) nbrNo[k]++;
                    if(nbr2[k] == mark) nbrNo[k]++;
                    if(nbr3[k] == mark) nbrNo[k]++;
                    if(nbr4[k] == mark) nbrNo[k]++;
                    if(nbr5[k] == mark) nbrNo[k]++;
                    if(nbr6[k] == mark) nbrNo[k]++;
                    if(nbr7[k] == mark) nbrNo[k]++;
                    if(nbr8[k] == mark) nbrNo[k]++;
                }

                for(int k=0; k<size; k++) {
                    if(ringMap[k] == mark && nbrNo[k] < 1) {
                        ringMap[k] = 0;
                        Log.d(TAG, "--- cleanupSPlate() removing iso-pixel of mark "+mark);
                    }
                }
            }
        }
    }

}