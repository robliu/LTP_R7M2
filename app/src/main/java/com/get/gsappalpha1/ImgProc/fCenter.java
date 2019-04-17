package com.get.gsappalpha1.ImgProc;

/**
 * Created by Rob on 7/15/2017.
 */


public class fCenter {
    private int     centerX;
    private int     centerY;
    private double  diameter;
    private int     type;
    private int[]   prof;
    private int     concentric;  // concentric group ID


    public void  fCenter()  {
        centerX     = -1;
        centerY     = -1;
        diameter    = 0.0;
        type        = 0;
        prof        = null;
        concentric  = -1;
    }

    public void  fCenter(int cx, int cy, double dia, int mtype)  {
        centerX     = cx;
        centerY     = cy;
        diameter    = dia;
        type        = mtype;
        prof        = null;
        concentric  = -1;
    }

    public void  fCenter(int cx, int cy, double dia, int mtype, int[] mprof)  {
        centerX     = cx;
        centerY     = cy;
        diameter    = dia;
        type        = mtype;
        prof        = mprof;
        concentric  = -1;
    }

    public void  fCenter(int cx, int cy, double dia, int mtype, int[] mprof, int concentID)  {
        centerX     = cx;
        centerY     = cy;
        diameter    = dia;
        type        = mtype;
        prof        = mprof;
        concentric  = concentID;
    }

    public void setXY (int ix, int iy) {
        centerX     = ix;
        centerY     = iy;
    }

    public void setDia (double idia) {
        diameter    = idia;
    }

    public void setType (int iType) {
        centerX     = iType;
    }

    public void setConcentricID (int cID) {
        concentric  = cID;
    }
}
