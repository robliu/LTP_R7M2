package com.get.gsappalpha1.ImgProc;

/**
 *  Target Ring Class -
 *      this is the standard Ring Class for circular targets, which is made of Rings. Unlike the
 *      printed Ring on the target, which may be a White Ring in between 2 Black Bands of circles,
 *      each such physical Ring will actually produce 2 Ring objects; one corresponds to
 *      a Dark-to-Bright center-wise transition and the other to a Bright-to-Dark center-wise
 *      transition. These two Ring objects should be concentric and with limited diameter
 *      difference, due to their physical association property.
 *
 * Created by Rob on 7/15/2017.
 */

public class TRing {
    public int     trcID;
    public int     centerX;
    public int     centerY;
    public int     type;         // Light-to-dark or vise-versa <-1, 0, 1>
    public double  diameter;
    public double  aspectRat;
    public int     concentric;
    public fCenter Center = null;
    public int[]   prof;
    public boolean isSuper;

    private static   final int    MAXRINGS  = 18;
    private static   final String      TAG  = "TRing";


    public void   TRing() {
        trcID       = -1;
        centerX     = -1;
        centerY     = -1;
        type        = -1;
        diameter    = -1.0;
        concentric  = -1;
        Center      = null;
        prof        = null;
        aspectRat   = -1.0;
        isSuper     = false;
    }

    public void   TRing(int indx) {
        trcID       = indx;
        centerX     = -1;
        centerY     = -1;
        type        = -1;
        diameter    = -1.0;
        concentric  = -1;
        Center      = null;
        prof        = null;
        aspectRat   = -1.0;
        isSuper     = false;
    }

    public void   TRing(int indx, int ix, int iy) {
        trcID       = indx;
        centerX     = ix;
        centerY     = iy;
        type        = -1;
        diameter    = -1.0;
        concentric  = -1;
        Center      = null;
        prof        = null;
        aspectRat   = -1.0;
        isSuper     = false;
    }

    public void   TRing(int indx, int ix, int iy, double iDia) {
        trcID       = indx;
        centerX     = ix;
        centerY     = iy;
        type        = -1;
        diameter    = iDia;
        concentric  = -1;
        Center      = null;
        prof        = null;
        aspectRat   = -1.0;
        isSuper     = false;
    }

    public void   TRing(TCircle mcircle) {
        if(mcircle == null) {
            trcID       = -1;
            centerX     = -1;
            centerY     = -1;
            type        = -1;
            diameter    = -1.0;
            concentric  = -1;
            Center      = null;
            prof        = null;
            aspectRat   = -1.0;
            isSuper     = false;
            return;
        }

        trcID       = mcircle.trcID;
        centerX     = mcircle.centerX;
        centerY     = mcircle.centerX;
        type        = mcircle.type;
        diameter    = -1;
        concentric  = mcircle.concentric;
        Center      = null;
        prof        = mcircle.prof;
        aspectRat   = mcircle.aspectRat;
        isSuper     = false;
    }

    public void   setXY(int ix, int iy) {
        centerX     = ix;
        centerY     = iy;
    }

    public void   setXY(int indx, int ix, int iy) {
        trcID       = indx;
        centerX     = ix;
        centerY     = iy;
    }

    public void   setAll(int indx, int ix, int iy, int itype, double iDia, int conIndx, int[] iprof, double aspRat, boolean Super) {
        trcID       = indx;
        centerX     = ix;
        centerY     = iy;
        type        = itype;
        diameter    = iDia;
        concentric  = conIndx;
        prof        = iprof;
        aspectRat   = aspRat;
        isSuper     = Super;
    }

    public TRing clone() {
        TRing  cRing = new TRing();

        cRing.setAll(trcID, centerX, centerY, type, diameter, concentric, prof, aspectRat, isSuper);

        return cRing;
    }

    public void RingSwap(TRing ring1, TRing ring2) {
//        TRing  cRing = new TRing;

        TRing cRing = ring1.clone();

        ring1 = ring2.clone();
        ring2 = cRing.clone();
    }


    public void sortByDia(TRing[] rings, boolean Increase)
    // Sort the Rings by their diameters
    // Increase = true:
    //   sorted by decreasing diameter size
    {
        if(rings.length <= 1) return;

        if(Increase) {
            for (int i = 0; i < rings.length - 1; i++) {
                TRing wring = rings[i].clone();

                for (int j = i + 1; j < rings.length; j++) {
                    TRing cring = rings[j].clone();

                    if (wring.diameter > cring.diameter) {
                        wring = cring.clone();
                        RingSwap(rings[i], rings[j]);
                    }
                }
            }
        } else {
            for (int i = 0; i < rings.length - 1; i++) {
                TRing wring = rings[i].clone();

                for (int j = i + 1; j < rings.length; j++) {
                    TRing cring = rings[j].clone();

                    if (wring.diameter > cring.diameter) {
                        wring = cring.clone();
                        RingSwap(rings[i], rings[j]);
                    }
                }
            }
        }

    }


    public TRing[][] groupRings(TRing[]tRings, int limit) {
        //  Creates and returns a number of grouped concentric-ring sets from the original
        //  raw TRing set tRings.
        //
        //   Note:
        //   1. the raw TRings are assumed arranged in the diameter' order; bigger rings first.
        //   2. The clean-up is based on the 'concentric' information of the rings.
        //
        int      i, j, limitm = limit - 1;
        int      x0, y0, x1, y1, r0, r1, dis, ConcenCnt = 0;
        double   ddist;
        boolean  conChecked = false;

        sortByDia(tRings, false);

        // calculate the concentric value for all rings
        for (i = 0; i < limitm; i++) {
            if (conChecked) { ConcenCnt++; conChecked = false; }

//            ring0 = tRings[i];
            if (tRings[i].concentric > 0) { continue; } // already checked

            x0 = tRings[i].centerX;
            y0 = tRings[i].centerY;
            r0 = (int)(0.5 * tRings[i].diameter);
            for (j = i+1; j < limit; j++) { // smaller center must be in the bigger one's radius
//                ring1 = tRings[j];
                if (tRings[j].concentric > 0) { continue; } // already checked

                r1 = (int)(0.5 * tRings[j].diameter);
                if (r1 >= r0 - 0.5) { continue; }

                x1 = tRings[j].centerX;
                y1 = tRings[j].centerY;

                ddist = Math.pow((x0 - x1), 2.0) + Math.pow((double)(y0 - y1), 2.0);

                dis = (int)Math.sqrt(ddist);
                if (dis < (r0 - r1 - 0.5)) { // concentric formula
                    tRings[i].concentric = ConcenCnt + 1;
                    tRings[j].concentric = ConcenCnt + 1;
                    conChecked = true;
                }
            }
        }

        TRing[][]  ringSets = new TRing[ConcenCnt][MAXRINGS];

        for (i = 1; i <= ConcenCnt; i++) {
            int units = 0;
//            ringSets[i - 1][0] = tRings[i].clone();

            for (j = 0; j < limitm; j++) {
//                ring0 = tRings[j];
                if (tRings[j].concentric == i) {
                    // Apply inner circle aspect ratio verification - must be rounded!
                    if(tRings[j].aspectRat > 1.54 || tRings[j].aspectRat < 0.65) continue;

                    ringSets[i - 1][0] = tRings[i].clone();
                    /*
                    TRing ringGroupUnit = new TRing();
                    ringSets[i-1].push(ringGroupUnit);

                    ringSets[i-1][units].ID     = i;
                    ringSets[i-1][units].trcID  = tRings[j].trcID;
                    ringSets[i-1][units].x      = tRings[j].centerX;
                    ringSets[i-1][units].y      = tRings[j].centerY;
                    ringSets[i-1][units].aspRat = tRings[j].aspectRat;
                    ringSets[i-1][units].dia    = tRings[j].diameter;
                    ringSets[i-1][units].type   = tRings[j].type;
                    */
                    units++;
                    if (units == 1) { ringSets[i - 1][0].isSuper = true; }
                }
            }
        }
        return ringSets;
    }
}
