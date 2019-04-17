package com.get.gsappalpha1.ImgProc;

/**
 * Created by Rob on 7/17/2017.
 */

public class tTrace {
    int[][]    tAry;     // trace position (x, y)
    int        gap;


    public  void tTrace() {
        tAry = null;
        gap = -9999;
    }

    public  void tTrace(int[][]  pos) {
        tAry = null;
        gap = -9999;

        if(pos.length < 1) return;

        tAry = new int[pos.length][2];

        for(int i =0; i< pos.length; i++) {
            tAry[i][0] = pos[i][0];
            tAry[i][1] = pos[i][1];
        }
    }

    public  void tTrace(int[][]  pos, int mgp) {
        tAry = null;
        gap  = mgp;

        if(pos.length < 1) return;

        tAry = new int[pos.length][2];

        for(int i =0; i< pos.length; i++) {
            tAry[i][0] = pos[i][0];
            tAry[i][1] = pos[i][1];
        }
    }

    public int length() {
        return tAry.length;
    }

    public  void push(int[] pos) {
        int len  = tAry.length;
        int len1 = tAry.length+1;
        int i;

        int[][]  nAry = new int[len1][2];
        for(i=0; i<len; i++) {
            nAry[i][0] = tAry[i][0];
            nAry[i][1] = tAry[i][1];
        }

        tAry = null;
        tAry = new int[len1][2];
        for(i=0; i<len; i++) {
            tAry[i][0] = nAry[i][0];
            tAry[i][1] = nAry[i][1];
        }
        tAry[i][0] = pos[0];
        tAry[i][1] = pos[1];

    }

    public  void pop( ) {
        int len1 = tAry.length-1;
        int i;

        int[][]  nAry = new int[len1][2];
        for(i=0; i<len1; i++) {
            nAry[i][0] = tAry[i][0];
            nAry[i][1] = tAry[i][1];
        }

        tAry = null;
        tAry = new int[len1][2];
        for(i=0; i<len1; i++) {
            tAry[i][0] = nAry[i][0];
            tAry[i][1] = nAry[i][1];
        }

    }

}
