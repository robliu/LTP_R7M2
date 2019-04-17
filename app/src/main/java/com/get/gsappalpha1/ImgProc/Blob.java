package com.get.gsappalpha1.ImgProc;

import android.util.Log;

import java.util.ArrayList;

/**
 * ===========================================================================
 *
 * Created by Rob, Guidance Education technologies, on 7/27/2017.
 *
 *  www.guidance-edu.com
 *
 * =========== All Rights Reserved, 2017 ===========================================
 */

public class Blob {
    public     int  Index;
    public     int  size;
    public     int  xDim;  // BBox based
    public     int  yDim;  // BBox based
    public     int  perem;
    public     int  x;
    public     int  y;
    public  double  density;
    public  double  aspRat;
    public     int  quality;
    public    byte  byteval;


    private static   final int     BlobRunLimit = 30000;
    private static   final int    G_MAXTRACELEN = 6000;
    private static   final int         MAXPATHS = 600;
    private static   final String           TAG = "Blob<>";

    public Blob( ) {
        Index = -1;
         size = -1;
         xDim = -1;
         yDim = -1;
        perem = -1;
            x = -1;
            y = -1;
        density = -1.0;
         aspRat = -1.0;
        quality = -1;
        byteval = -1;
    }


    public Blob(int idx) {
        Index = idx;
        size = -1;
        xDim = -1;
        yDim = -1;
        perem = -1;
        x = -1;
        y = -1;
        density = -1.0;
        aspRat = -1.0;
        quality = -1;
        byteval = -1;
    }

    public Blob(int idx, int msize, int mx, int my) {
        Index = idx;
        size = msize;
        xDim = -1;
        yDim = -1;
        perem = -1;
        x = mx;
        y = my;
        density = -1.0;
        aspRat = -1.0;
        quality = -1;
        byteval = -1;
    }

    public Blob(int idx, int msize, int mxdim, int mydim, int mx, int my) {
        Index = idx;
        size = (msize > 0)? msize : 0;
        xDim = (mxdim > 0)? mxdim : 0;
        yDim = (mydim > 0)? mydim : 0;
        perem = -1;
        x = mx;
        y = my;
        density = -1.0;
        aspRat = xDim/(yDim + 0.0001);
        quality = -1;
        byteval = -1;
    }

    public Blob(int idx, int msize, int mxdim, int mydim, int mperem, int mx, int my, double mdensity) {
        Index = idx;
        size = (msize > 0)? msize : 0;
        xDim = (mxdim > 0)? mxdim : 0;
        yDim = (mydim > 0)? mydim : 0;
        perem = mperem;
        x = mx;
        y = my;
        density = mdensity;
        aspRat = xDim/(yDim + 0.0001);
        quality = -1;
        byteval = -1;
    }

    public void copy(Blob src) {
        Index = src.Index;
        size  = src.size;
        xDim  = src.xDim;
        yDim  = src.yDim;
        perem = src.perem;
        x = src.x;
        y = src.y;
        density = src.density;
        aspRat  = src.aspRat;
        quality = src.quality;
        byteval = src.byteval;
    }

    public void logging() {
        Log.d(TAG, "Blob: < xDim / yDim / x / y / size / perem / density / aspRatio / byteval > = "
                + xDim +" / " + yDim+" / "+x+" / "+y+" / "+size+" / "+perem+" / "+density+" / "+aspRat+" / "+byteval);
    }


    public void setQuality(int q) {
        quality = q;
    }

    public void Evaluate(int[] map, int w, int h, int label, int indx) {
        // characterize the present blob marked in map[] by 'label' and assign the Blob index to 'indx'
        Log.d(TAG, "Evaluate() entered...   w / h / label / indx = " + w + " / " + h + " / " + label + " / " + indx);

        if (map.length != w * h) {
            Log.d(TAG, "Evaluate() aborted due to inconsistent data size!");
            return;
        }

        int    len = w * h;
        int   lenm = len - w - 1;
        int   xmin = w, xmax = 0;
        int   ymin = h, ymax = 0;
        int     wm = w - 1, hm = h - 1;
        int border = 0;

        size  = 0;
        Index = indx;
        perem = 0;
        for (int i = w+1; i < lenm; i++) {
            if (map[i] == label) {
                size++;
                int ix = i % w;
                int iy = i / w;

                if (ix > xmax) xmax = ix;
                else if (ix < xmin) xmin = ix;
                if (iy > ymax) ymax = iy;
                else if (iy < ymin) ymin = iy;

                // count 9-neighbors to identify boundary points
                int[] nbr8 = new int[8];
                if( (ix < 1) || (ix == wm) ) { continue; }

                nbr8[0] = i - w - 1;
                nbr8[1] = i - w;
                nbr8[2] = i - w + 1;
                nbr8[3] = i - 1;
                nbr8[4] = i + 1;
                nbr8[5] = i + w - 1;
                nbr8[6] = i + w;
                nbr8[7] = i + w + 1;

                for(int j=0; j<8; j++) {
                    if (map[nbr8[j]] != label) {
                        border++;
                        break;
                    }
                }
            }
        }

        if (size > 0) {
            xDim  = xmax - xmin + 1;
            yDim  = ymax - ymin + 1;
            x     = (xmax + xmin) / 2;
            y     = (ymax + ymin) / 2;
            perem = border;

            density = size / (xDim * yDim + 0.0001);

            if(yDim > 0) aspRat = xDim/(yDim+0.0001);
            else         aspRat = 0.0;

            Log.d(TAG, "Evaluate() result: xDim/yDim/x/y/size/perem/density/aspRatio = "
                    +xDim+"/"+yDim+"/"+x+"/"+y+"/"+size+"/"+perem+"/"+density+"/"+aspRat);
        }
    }

    public ArrayList<Blob>  EvaluateAll(int[] map, int w, int h, int minSize, int[] indxList, int[] labelList) {
        // characterize and return all blobs marked in map[] with their labels specified in indxList[] and
        // size greater than minSize

        Log.d(TAG, "EvaluateAll() entered...   w / h / minSize = " + w + " / " + h + " / " + minSize);

        if (map.length != w * h || indxList.length < 1 || labelList.length < 1) return null;

        if(minSize < 1) minSize = 1;

        int    len = w * h;
        int   xmin = w, xmax = 0;
        int   ymin = h, ymax = 0;
        int     wm = w - 1, hm = h - 1;
        int border = 0;
        int  mlabel, msize, mIndex, mperem;
        int   blen = indxList.length;

        ArrayList<Blob> BlobAry = null;

        for (int j=0; j<blen; j++) {
            msize = 0;
            mIndex = indxList[j];
            mlabel = labelList[j];
            for (int i = 0; i < len; i++) {
                if (map[i] == mlabel) {
                    msize++;
                }
            }

            if (msize < minSize) continue;

            for (int i = 0; i < len; i++) {
                if (map[i] == mlabel) {

                    int ix = i % w;
                    int iy = i / w;

                    if (ix > xmax) xmax = ix;
                    else if (ix < xmin) xmin = ix;
                    if (iy > ymax) ymax = iy;
                    else if (iy < ymin) ymin = iy;

                    // count 9-neighbors to identify boundary points
                    int[] nbr8 = new int[8];
                    if ((ix < 1) || (iy < 1) || (ix == wm) || (iy == hm)) {
                        continue;
                    }

                    nbr8[0] = i - w - 1;
                    nbr8[1] = i - w;
                    nbr8[2] = i - w + 1;
                    nbr8[3] = i - 1;
                    nbr8[4] = i + 1;
                    nbr8[5] = i + w - 1;
                    nbr8[6] = i + w;
                    nbr8[7] = i + w + 1;

                    for (int k = 0; k < 8; k++) {
                        if (map[nbr8[k]] != mlabel) {
                            border++;
                            break;
                        }
                    }
                }
            }

            if (msize > minSize) {
                Blob blb = new Blob(mIndex, msize, xmax - xmin + 1, ymax - ymin + 1, border, (xmax + xmin) / 2, (ymax + ymin) / 2, density);
                // Blob(int idx, int msize, int mxdim, int mydim, int mperem, int mx, int my, double mdensity)

                blb.logging();

                BlobAry.add(blb);
            }
        }
        return BlobAry;
    }
}
