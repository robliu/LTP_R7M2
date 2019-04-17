package com.get.gsappalpha1.ImgProc;

/**
 * Created by Rob on 7/15/2017.
 */

public class keypoint {

    public int     x;
    public int     y;
    public int     score;
    public int     level;
    public double  angle;


    public void  keypoint( ) {
        x     = -1;
        y     = -1;
        score = -1;
        level = -1;
        angle = -100.0;
    }


    public void  keypoint(int ix, int iy, int iscore, int ilevel, double iangle) {
        x     = ix;
        y     = iy;
        score = iscore;
        level = ilevel;
        angle = iangle;
    }

    public void  setAll(int ix, int iy, int iscore, int ilevel, double iangle) {
        x     = ix;
        y     = iy;
        score = iscore;
        level = ilevel;
        angle = iangle;
    }

    public void  setXY(int ix, int iy) {
        x     = ix;
        y     = iy;
    }

}
