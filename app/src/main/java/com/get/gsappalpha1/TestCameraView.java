package com.get.gsappalpha1;

/**
 *  ===== This work is copyrighted! Do not copy or reuse without Guidance permission! ======
 *
 *    For detail, contact www.guidance-edu.com
 *
 *
 *     Created by Rob on 2/23/2017.
 *
 *  Copy Rights 2017, Guidance Education Technologies, All Rights Reserved.
 *
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.get.gsappalpha1.ImgProc.Blob;
import com.get.gsappalpha1.ImgProc.GrayProc;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;


public class TestCameraView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder  mHolder;
    private        Camera  mCamera;
    public   TestActivity  mActivity;

    //-------------------------------------------------------------------------------------
    private static final int                C_WAIT = -2;  // calibration wait for start,
    private static final int               C_CALIB = -1;  // calibration in progress,
    private static final int            C_PARAMCAL = 0;   // just starting IP,
    private static final int              C_DETECT = 1;   // in detection,
    private static final int            C_POSTPROC = 2;   // post-detection processing
    private static final int              C_REVIEW = 3;   // in review
    //-------------------------------------------------------------------------------------

    //Setup
    private Bitmap  bgImage;                  // the background image for review, rotates by 90 degrees
    public  int     shotLimit;                // maximum number of shots per session
    private int     IPState = C_WAIT;         // State for IPCamera, -2: calibration wait until completion, -1: calibration in progress,
                                              //                      0: just starting, 1: shooting, 3: in review
    private int[]   ROI     = new int[4];

    private int     timesCalibrated;          // we want to calibrate up to 3 times
    private boolean isFirstCal;               // Marker of the first calibration run? If it is, we want to grab the frame for bgImage


    //camera settings
    public   Camera.Parameters cameraParameters;
    private  Camera.Size       mCameraSize;
    private  Camera.CameraInfo mCameraInfo;
    private  int               IPwidth;       // s.b. 480
    private  int               IPheight;      // s.b. 640

    private List<Camera.Size>  CamPreviewSizes;

    //detection
    private static final int G_IPStartFrame        = 20;    // Starting Frame for real detection (after auto threshold determination)
    private static final int G_IPStartFrame2       = G_IPStartFrame + 2;
    private static final int G_IPDCZFramesN        = 16;                   // no. of DCZ source frames [KNOW HOW TO CHANGE THIS NUMBER - bit shift involved!]
    private static final int G_IPDCZcountMax       = 1000;                 // max no. of Don't Care Zones
    private static final int G_IPDCZcountMax_m5    = G_IPDCZcountMax - 5;  // max no. of Don't Care Zones
    private static final int G_IPHRecordMax        = 3000;  // max no. of hit pixel records in a single frame; matching G_IPFrameDataCountMax in IPFrameData.java
    private static final int G_IPHCSizeMax         = 3000;  // max no. of hit pixels in a single frame
    private static final int G_IPHitFramesMax      = 500;   // max no. of frames containing hits
    private static final int G_IPFrameCountMax     = 9500;  // max no. of frames in a session (31 Fm/Sec x 60 Sec x 5 Min)
    private static final int G_IPFrameCountStop    = 9300;
    private static final int G_JPGQuality          = 70;
    private static final int G_MAXPREVIEWDIMENSION = 640;   // Global! - check external files when updating!     // target scoring template

//    private boolean[]        IPDCZMap    = new boolean[640 * 480];
    private boolean[]     IPTemplateLow;                              //  = new boolean[640 * 480];    // if IPTemplate[] point is below IPBKLow
    private int[]            IPTemplate  = new int[640 * 480];        // [previewHeight * previewWidth];
    private int              IPDCZcount  = 0;
    private int[]            IPDCZx      = new int[G_IPDCZcountMax+10];
    private int[]            IPDCZy      = new int[G_IPDCZcountMax+10];
    private int[]            IPDCZv      = new int[G_IPDCZcountMax+10];
    private int[]            IPHState    = new int[G_IPFrameCountMax]; // hit State of a Frame
    private int[]            IPHPopul    = new int[G_IPFrameCountMax]; // hit population of a Frame
    public  int              IPHFIndx    = 0;
    public  boolean          IPNewSeq    = false; // reset Hit Sequence Flag
    public  boolean          IPDetectSet = false;
    public  boolean        IPneedStartIP = true;
    public  boolean          ISOinAction = false;
    public  boolean   IPTemplateLowReady = false;
    public  boolean        IPThreshReady = false;
    public  int              IPHSeqID    = -1;
    public  int              IPFmRate    = 0;
    public  long             IPStarTime  = 0L;
    public  long             IPEndTime   = 0L;
    private long             timestamp;

    public  boolean          IPNeedPamCalInit   = true;
    private String           ISOValuesParameter = null;
    private String           ISOParameter       = null;
    private String           ISOValues          = null;

    public  ArrayList<Integer>  HitStarts;
    public  ArrayList<laserHit> HitList;

    // Calibration stuff
    private int            DZCThresh;
    private int		       IPBKLow;
    public  int            IPRefAvg;
    public  int            IPCtrAvg;
    private int            IPThresh;
    private int		       IPThreshLow;
    public  int            threadHB;
    private int            head;
    private int            mIPFrame;
    public TextView        statusBar;

    private final String   TAG                 = "TestCameraView";
    private       boolean  IPAutoFocusBusy     = false;
    private       boolean  IPSlowWarned        = false;
    private       boolean  IPShootingIsStopped = false;
    private       boolean  NoPVSizeControl     = false;
//    private       boolean  G_IsBeta   = true;

    //other stuff
    private int            shots;                       // number of shots detected

    //for review
    float x1, x2;                                       // touch coordinates
    float y1, y2;                                       // touch coordinates

    public int currentreviewShot;                       // the shot that is being shown in review mode, -1 is show all

    public SharedPreferences mpreferences;
    public String            model;


    private static final int[]  BWmap = {
            0xff000000, 0xff010101, 0xff020202, 0xff030303, 0xff040404, 0xff050505, 0xff060606, 0xff070707, 0xff080808, 0xff090909, 0xff0a0a0a, 0xff0b0b0b, 0xff0c0c0c, 0xff0d0d0d, 0xff0e0e0e, 0xff0f0f0f,
            0xff101010, 0xff111111, 0xff121212, 0xff131313, 0xff141414, 0xff151515, 0xff161616, 0xff171717, 0xff181818, 0xff191919, 0xff1a1a1a, 0xff1b1b1b, 0xff1c1c1c, 0xff1d1d1d, 0xff1e1e1e, 0xff1f1f1f,
            0xff202020, 0xff212121, 0xff222222, 0xff232323, 0xff242424, 0xff252525, 0xff262626, 0xff272727, 0xff282828, 0xff292929, 0xff2a2a2a, 0xff2b2b2b, 0xff2c2c2c, 0xff2d2d2d, 0xff2e2e2e, 0xff2f2f2f,
            0xff303030, 0xff313131, 0xff323232, 0xff333333, 0xff343434, 0xff353535, 0xff363636, 0xff373737, 0xff383838, 0xff393939, 0xff3a3a3a, 0xff3b3b3b, 0xff3c3c3c, 0xff3d3d3d, 0xff3e3e3e, 0xff3f3f3f,
            0xff404040, 0xff414141, 0xff424242, 0xff434343, 0xff444444, 0xff454545, 0xff464646, 0xff474747, 0xff484848, 0xff494949, 0xff4a4a4a, 0xff4b4b4b, 0xff4c4c4c, 0xff4d4d4d, 0xff4e4e4e, 0xff4f4f4f,
            0xff505050, 0xff515151, 0xff525252, 0xff535353, 0xff545454, 0xff555555, 0xff565656, 0xff575757, 0xff585858, 0xff595959, 0xff5a5a5a, 0xff5b5b5b, 0xff5c5c5c, 0xff5d5d5d, 0xff5e5e5e, 0xff5f5f5f,
            0xff606060, 0xff616161, 0xff626262, 0xff636363, 0xff646464, 0xff656565, 0xff666666, 0xff676767, 0xff686868, 0xff696969, 0xff6a6a6a, 0xff6b6b6b, 0xff6c6c6c, 0xff6d6d6d, 0xff6e6e6e, 0xff6f6f6f,
            0xff707070, 0xff717171, 0xff727272, 0xff737373, 0xff747474, 0xff757575, 0xff767676, 0xff777777, 0xff787878, 0xff797979, 0xff7a7a7a, 0xff7b7b7b, 0xff7c7c7c, 0xff7d7d7d, 0xff7e7e7e, 0xff7f7f7f,
            0xff808080, 0xff818181, 0xff828282, 0xff838383, 0xff848484, 0xff858585, 0xff868686, 0xff878787, 0xff888888, 0xff898989, 0xff8a8a8a, 0xff8b8b8b, 0xff8c8c8c, 0xff8d8d8d, 0xff8e8e8e, 0xff8f8f8f,
            0xff909090, 0xff919191, 0xff929292, 0xff939393, 0xff949494, 0xff959595, 0xff969696, 0xff979797, 0xff989898, 0xff999999, 0xff9a9a9a, 0xff9b9b9b, 0xff9c9c9c, 0xff9d9d9d, 0xff9e9e9e, 0xff9f9f9f,
            0xffa0a0a0, 0xffa1a1a1, 0xffa2a2a2, 0xffa3a3a3, 0xffa4a4a4, 0xffa5a5a5, 0xffa6a6a6, 0xffa7a7a7, 0xffa8a8a8, 0xffa9a9a9, 0xffaaaaaa, 0xffababab, 0xffacacac, 0xffadadad, 0xffaeaeae, 0xffafafaf,
            0xffb0b0b0, 0xffb1b1b1, 0xffb2b2b2, 0xffb3b3b3, 0xffb4b4b4, 0xffb5b5b5, 0xffb6b6b6, 0xffb7b7b7, 0xffb8b8b8, 0xffb9b9b9, 0xffbababa, 0xffbbbbbb, 0xffbcbcbc, 0xffbdbdbd, 0xffbebebe, 0xffbfbfbf,
            0xffc0c0c0, 0xffc1c1c1, 0xffc2c2c2, 0xffc3c3c3, 0xffc4c4c4, 0xffc5c5c5, 0xffc6c6c6, 0xffc7c7c7, 0xffc8c8c8, 0xffc9c9c9, 0xffcacaca, 0xffcbcbcb, 0xffcccccc, 0xffcdcdcd, 0xffcecece, 0xffcfcfcf,
            0xffd0d0d0, 0xffd1d1d1, 0xffd2d2d2, 0xffd3d3d3, 0xffd4d4d4, 0xffd5d5d5, 0xffd6d6d6, 0xffd7d7d7, 0xffd8d8d8, 0xffd9d9d9, 0xffdadada, 0xffdbdbdb, 0xffdcdcdc, 0xffdddddd, 0xffdedede, 0xffdfdfdf,
            0xffe0e0e0, 0xffe1e1e1, 0xffe2e2e2, 0xffe3e3e3, 0xffe4e4e4, 0xffe5e5e5, 0xffe6e6e6, 0xffe7e7e7, 0xffe8e8e8, 0xffe9e9e9, 0xffeaeaea, 0xffebebeb, 0xffececec, 0xffededed, 0xffeeeeee, 0xffefefef,
            0xfff0f0f0, 0xfff1f1f1, 0xfff2f2f2, 0xfff3f3f3, 0xfff4f4f4, 0xfff5f5f5, 0xfff6f6f6, 0xfff7f7f7, 0xfff8f8f8, 0xfff9f9f9, 0xfffafafa, 0xfffbfbfb, 0xfffcfcfc, 0xfffdfdfd, 0xfffefefe, 0xffffffff,
    };

    private static final int[] RDmap = {
            0xff000000,0xff010000,0xff020000,0xff030000,0xff040000,0xff050000,0xff060000,0xff070000,0xff080000,0xff090000,0xff0a0000,0xff0b0000,0xff0c0000,0xff0d0000,0xff0e0000,0xff0f0000,
            0xff100000,0xff110000,0xff120000,0xff130000,0xff140000,0xff150000,0xff160000,0xff170000,0xff180000,0xff190000,0xff1a0000,0xff1b0000,0xff1c0000,0xff1d0000,0xff1e0000,0xff1f0000,
            0xff200000,0xff210000,0xff220000,0xff230000,0xff240000,0xff250000,0xff260000,0xff270000,0xff280000,0xff290000,0xff2a0000,0xff2b0000,0xff2c0000,0xff2d0000,0xff2e0000,0xff2f0000,
            0xff300000,0xff310000,0xff320000,0xff330000,0xff340000,0xff350000,0xff360000,0xff370000,0xff380000,0xff390000,0xff3a0000,0xff3b0000,0xff3c0000,0xff3d0000,0xff3e0000,0xff3f0000,
            0xff400000,0xff410000,0xff420000,0xff430000,0xff440000,0xff450000,0xff460000,0xff470000,0xff480000,0xff490000,0xff4a0000,0xff4b0000,0xff4c0000,0xff4d0000,0xff4e0000,0xff4f0000,
            0xff500000,0xff510000,0xff520000,0xff530000,0xff540000,0xff550000,0xff560000,0xff570000,0xff580000,0xff590000,0xff5a0000,0xff5b0000,0xff5c0000,0xff5d0000,0xff5e0000,0xff5f0000,
            0xff600000,0xff610000,0xff620000,0xff630000,0xff640000,0xff650000,0xff660000,0xff670000,0xff680000,0xff690000,0xff6a0000,0xff6b0000,0xff6c0000,0xff6d0000,0xff6e0000,0xff6f0000,
            0xff700000,0xff710000,0xff720000,0xff730000,0xff740000,0xff750000,0xff760000,0xff770000,0xff780000,0xff790000,0xff7a0000,0xff7b0000,0xff7c0000,0xff7d0000,0xff7e0000,0xff7f0000,
            0xff800000,0xff810000,0xff820000,0xff830000,0xff840000,0xff850000,0xff860000,0xff870000,0xff880000,0xff890000,0xff8a0000,0xff8b0000,0xff8c0000,0xff8d0000,0xff8e0000,0xff8f0000,
            0xff900000,0xff910000,0xff920000,0xff930000,0xff940000,0xff950000,0xff960000,0xff970000,0xff980000,0xff990000,0xff9a0000,0xff9b0000,0xff9c0000,0xff9d0000,0xff9e0000,0xff9f0000,
            0xffa00000,0xffa10000,0xffa20000,0xffa30000,0xffa40000,0xffa50000,0xffa60000,0xffa70000,0xffa80000,0xffa90000,0xffaa0000,0xffab0000,0xffac0000,0xffad0000,0xffae0000,0xffaf0000,
            0xffb00000,0xffb10000,0xffb20000,0xffb30000,0xffb40000,0xffb50000,0xffb60000,0xffb70000,0xffb80000,0xffb90000,0xffba0000,0xffbb0000,0xffbc0000,0xffbd0000,0xffbe0000,0xffbf0000,
            0xffc00000,0xffc10000,0xffc20000,0xffc30000,0xffc40000,0xffc50000,0xffc60000,0xffc70000,0xffc80000,0xffc90000,0xffca0000,0xffcb0000,0xffcc0000,0xffcd0000,0xffce0000,0xffcf0000,
            0xffd00000,0xffd10000,0xffd20000,0xffd30000,0xffd40000,0xffd50000,0xffd60000,0xffd70000,0xffd80000,0xffd90000,0xffda0000,0xffdb0000,0xffdc0000,0xffdd0000,0xffde0000,0xffdf0000,
            0xffe00000,0xffe10000,0xffe20000,0xffe30000,0xffe40000,0xffe50000,0xffe60000,0xffe70000,0xffe80000,0xffe90000,0xffea0000,0xffeb0000,0xffec0000,0xffed0000,0xffee0000,0xffef0000,
            0xfff00000,0xfff10000,0xfff20000,0xfff30000,0xfff40000,0xfff50000,0xfff60000,0xfff70000,0xfff80000,0xfff90000,0xfffa0000,0xfffb0000,0xfffc0000,0xfffd0000,0xfffe0000,0xffff0000
    };

    private static final int[] GNmap = {
            0xff000000,0xff000100,0xff000200,0xff000300,0xff000400,0xff000500,0xff000600,0xff000700,0xff000800,0xff000900,0xff000a00,0xff000b00,0xff000c00,0xff000d00,0xff000e00,0xff000f00,
            0xff001000,0xff001100,0xff001200,0xff001300,0xff001400,0xff001500,0xff001600,0xff001700,0xff001800,0xff001900,0xff001a00,0xff001b00,0xff001c00,0xff001d00,0xff001e00,0xff001f00,
            0xff002000,0xff002100,0xff002200,0xff002300,0xff002400,0xff002500,0xff002600,0xff002700,0xff002800,0xff002900,0xff002a00,0xff002b00,0xff002c00,0xff002d00,0xff002e00,0xff002f00,
            0xff003000,0xff003100,0xff003200,0xff003300,0xff003400,0xff003500,0xff003600,0xff003700,0xff003800,0xff003900,0xff003a00,0xff003b00,0xff003c00,0xff003d00,0xff003e00,0xff003f00,
            0xff004000,0xff004100,0xff004200,0xff004300,0xff004400,0xff004500,0xff004600,0xff004700,0xff004800,0xff004900,0xff004a00,0xff004b00,0xff004c00,0xff004d00,0xff004e00,0xff004f00,
            0xff005000,0xff005100,0xff005200,0xff005300,0xff005400,0xff005500,0xff005600,0xff005700,0xff005800,0xff005900,0xff005a00,0xff005b00,0xff005c00,0xff005d00,0xff005e00,0xff005f00,
            0xff006000,0xff006100,0xff006200,0xff006300,0xff006400,0xff006500,0xff006600,0xff006700,0xff006800,0xff006900,0xff006a00,0xff006b00,0xff006c00,0xff006d00,0xff006e00,0xff006f00,
            0xff007000,0xff007100,0xff007200,0xff007300,0xff007400,0xff007500,0xff007600,0xff007700,0xff007800,0xff007900,0xff007a00,0xff007b00,0xff007c00,0xff007d00,0xff007e00,0xff007f00,
            0xff008000,0xff008100,0xff008200,0xff008300,0xff008400,0xff008500,0xff008600,0xff008700,0xff008800,0xff008900,0xff008a00,0xff008b00,0xff008c00,0xff008d00,0xff008e00,0xff008f00,
            0xff009000,0xff009100,0xff009200,0xff009300,0xff009400,0xff009500,0xff009600,0xff009700,0xff009800,0xff009900,0xff009a00,0xff009b00,0xff009c00,0xff009d00,0xff009e00,0xff009f00,
            0xff00a000,0xff00a100,0xff00a200,0xff00a300,0xff00a400,0xff00a500,0xff00a600,0xff00a700,0xff00a800,0xff00a900,0xff00aa00,0xff00ab00,0xff00ac00,0xff00ad00,0xff00ae00,0xff00af00,
            0xff00b000,0xff00b100,0xff00b200,0xff00b300,0xff00b400,0xff00b500,0xff00b600,0xff00b700,0xff00b800,0xff00b900,0xff00ba00,0xff00bb00,0xff00bc00,0xff00bd00,0xff00be00,0xff00bf00,
            0xff00c000,0xff00c100,0xff00c200,0xff00c300,0xff00c400,0xff00c500,0xff00c600,0xff00c700,0xff00c800,0xff00c900,0xff00ca00,0xff00cb00,0xff00cc00,0xff00cd00,0xff00ce00,0xff00cf00,
            0xff00d000,0xff00d100,0xff00d200,0xff00d300,0xff00d400,0xff00d500,0xff00d600,0xff00d700,0xff00d800,0xff00d900,0xff00da00,0xff00db00,0xff00dc00,0xff00dd00,0xff00de00,0xff00df00,
            0xff00e000,0xff00e100,0xff00e200,0xff00e300,0xff00e400,0xff00e500,0xff00e600,0xff00e700,0xff00e800,0xff00e900,0xff00ea00,0xff00eb00,0xff00ec00,0xff00ed00,0xff00ee00,0xff00ef00,
            0xff00f000,0xff00f100,0xff00f200,0xff00f300,0xff00f400,0xff00f500,0xff00f600,0xff00f700,0xff00f800,0xff00f900,0xff00fa00,0xff00fb00,0xff00fc00,0xff00fd00,0xff00fe00,0xff00ff00
    };

    private static final int[] BLmap = {
            0xff000000,0xff000001,0xff000002,0xff000003,0xff000004,0xff000005,0xff000006,0xff000007,0xff000008,0xff000009,0xff00000a,0xff00000b,0xff00000c,0xff00000d,0xff00000e,0xff00000f,
            0xff000010,0xff000011,0xff000012,0xff000013,0xff000014,0xff000015,0xff000016,0xff000017,0xff000018,0xff000019,0xff00001a,0xff00001b,0xff00001c,0xff00001d,0xff00001e,0xff00001f,
            0xff000020,0xff000021,0xff000022,0xff000023,0xff000024,0xff000025,0xff000026,0xff000027,0xff000028,0xff000029,0xff00002a,0xff00002b,0xff00002c,0xff00002d,0xff00002e,0xff00002f,
            0xff000030,0xff000031,0xff000032,0xff000033,0xff000034,0xff000035,0xff000036,0xff000037,0xff000038,0xff000039,0xff00003a,0xff00003b,0xff00003c,0xff00003d,0xff00003e,0xff00003f,
            0xff000040,0xff000041,0xff000042,0xff000043,0xff000044,0xff000045,0xff000046,0xff000047,0xff000048,0xff000049,0xff00004a,0xff00004b,0xff00004c,0xff00004d,0xff00004e,0xff00004f,
            0xff000050,0xff000051,0xff000052,0xff000053,0xff000054,0xff000055,0xff000056,0xff000057,0xff000058,0xff000059,0xff00005a,0xff00005b,0xff00005c,0xff00005d,0xff00005e,0xff00005f,
            0xff000060,0xff000061,0xff000062,0xff000063,0xff000064,0xff000065,0xff000066,0xff000067,0xff000068,0xff000069,0xff00006a,0xff00006b,0xff00006c,0xff00006d,0xff00006e,0xff00006f,
            0xff000070,0xff000071,0xff000072,0xff000073,0xff000074,0xff000075,0xff000076,0xff000077,0xff000078,0xff000079,0xff00007a,0xff00007b,0xff00007c,0xff00007d,0xff00007e,0xff00007f,
            0xff000080,0xff000081,0xff000082,0xff000083,0xff000084,0xff000085,0xff000086,0xff000087,0xff000088,0xff000089,0xff00008a,0xff00008b,0xff00008c,0xff00008d,0xff00008e,0xff00008f,
            0xff000090,0xff000091,0xff000092,0xff000093,0xff000094,0xff000095,0xff000096,0xff000097,0xff000098,0xff000099,0xff00009a,0xff00009b,0xff00009c,0xff00009d,0xff00009e,0xff00009f,
            0xff0000a0,0xff0000a1,0xff0000a2,0xff0000a3,0xff0000a4,0xff0000a5,0xff0000a6,0xff0000a7,0xff0000a8,0xff0000a9,0xff0000aa,0xff0000ab,0xff0000ac,0xff0000ad,0xff0000ae,0xff0000af,
            0xff0000b0,0xff0000b1,0xff0000b2,0xff0000b3,0xff0000b4,0xff0000b5,0xff0000b6,0xff0000b7,0xff0000b8,0xff0000b9,0xff0000ba,0xff0000bb,0xff0000bc,0xff0000bd,0xff0000be,0xff0000bf,
            0xff0000c0,0xff0000c1,0xff0000c2,0xff0000c3,0xff0000c4,0xff0000c5,0xff0000c6,0xff0000c7,0xff0000c8,0xff0000c9,0xff0000ca,0xff0000cb,0xff0000cc,0xff0000cd,0xff0000ce,0xff0000cf,
            0xff0000d0,0xff0000d1,0xff0000d2,0xff0000d3,0xff0000d4,0xff0000d5,0xff0000d6,0xff0000d7,0xff0000d8,0xff0000d9,0xff0000da,0xff0000db,0xff0000dc,0xff0000dd,0xff0000de,0xff0000df,
            0xff0000e0,0xff0000e1,0xff0000e2,0xff0000e3,0xff0000e4,0xff0000e5,0xff0000e6,0xff0000e7,0xff0000e8,0xff0000e9,0xff0000ea,0xff0000eb,0xff0000ec,0xff0000ed,0xff0000ee,0xff0000ef,
            0xff0000f0,0xff0000f1,0xff0000f2,0xff0000f3,0xff0000f4,0xff0000f5,0xff0000f6,0xff0000f7,0xff0000f8,0xff0000f9,0xff0000fa,0xff0000fb,0xff0000fc,0xff0000fd,0xff0000fe,0xff0000ff
    };


    static {
        System.loadLibrary("glt21xxlib");
    }

    public TestCameraView(Context context, Camera camera){
        super(context);
        //make sure we are on the back camera

//      Log.d("--- TestCameraView() ", "Starting...");

        mCamera = camera;
        mCamera.setDisplayOrientation(90);

        CamPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();

        //get the holder and set this class as the callback, so we can get camera data here
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        if (mCamera == null) {
            Log.d(TAG, "surfaceCreated() mCamera is null!");
            return;
        }

        try {
//            Log.d(TAG, "--- surfaceCreated() entered...");
            cameraParameters = mCamera.getParameters();

            //when the surface is created, we can set the camera to draw images in this surfaceholder
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

            cameraParameters = mCamera.getParameters();

//            Log.d(TAG, "--- surfaceCreated() Camera Preview w/h = "+cameraParameters.getPreviewSize().width+"/"+cameraParameters.getPreviewSize().height);

            if( !cameraParameters.isAutoExposureLockSupported() || NoPVSizeControl) {
                AlertDialog mAlertDialog = new AlertDialog.Builder(mActivity.getApplicationContext()).create();

                if(mAlertDialog != null) {
                    mAlertDialog.setTitle("Performance Warning"); // ("Session Alert");
                    String msg = "Your device doesn't support Camera Exposure Control!";
                    if(NoPVSizeControl) {
                        msg = "Your device doesn't support required Camera Control!";
                    }
                    mAlertDialog.setMessage( msg );
                    mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "\n       OK ",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    dialog.dismiss();
                                }
                            });
                    mAlertDialog.show();
                    return;
                }
            }

            cameraParameters.setExposureCompensation(0);
            cameraParameters.setAutoExposureLock(false);
            cameraParameters.setFocusMode(FOCUS_MODE_AUTO);
            cameraParameters.setAutoWhiteBalanceLock(false);
            cameraParameters.setFlashMode(FLASH_MODE_OFF);

            mCamera.setParameters(cameraParameters);

        } catch (IOException e) {
            Log.e("TestCameraPreview", "Camera error on surfaceCreated " + e.getMessage());
            return;
        }

        mActivity.mAudioManager.setSpeakerphoneOn(true);
        mActivity.mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mActivity.SpeakerVol, 0);

        IPAutoFocusBusy = false;
        ISOValues       = getISOValues();
        String[] supportedISOs = ISOValues.split(",");

        if(ISOValuesParameter != null) {

            ISOParameter = ISOValuesParameter.replace("-values", "");

            for (int i = 0; i < supportedISOs.length; i++) {
                if (supportedISOs[i].equals("400")) {
                    cameraParameters.set(ISOParameter, "400");
                    Log.d(TAG, "  -------surfaceCreated() setting ISO to 400!");

                    break;
                }
            }

            mCamera.setParameters(cameraParameters);
        } else {
            Log.d(TAG, "  ------------ surfaceCreated() Can't Set ISO; not supported.");
        }

        mpreferences = mActivity.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        model        = mpreferences.getString("MyDeviceModel", "");
    }


    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        //before changing the application orientation, you need to stop the preview, rotate and then start it again

        if(mHolder.getSurface() == null) return;

        try {
            mCamera.stopPreview(); // safety measure; won't hurt anything!
        } catch (Exception e){
            //this will happen when you are trying the camera if it's not running
        }

        NoPVSizeControl = false;
        IPAutoFocusBusy = false;

        try { // recreate the camera preview

            cameraParameters = mCamera.getParameters();
            CamPreviewSizes  = cameraParameters.getSupportedPreviewSizes();

            if(CamPreviewSizes == null) {
                // will not work; post message and quit!
                Log.d(TAG, "surfaceChanged() - Camera PreviewSize can't be controlled on device!");
                NoPVSizeControl = true;
            } else {
/*
                Log.d(TAG, "surfaceChanged(): Available Cam Preview Size:");
                for (Camera.Size size : CamPreviewSizes) {
                    Log.d(TAG, "  --- " + size.width + " / " + size.height);
                }
*/
                mCameraSize = getOptimalPreviewSize(CamPreviewSizes, 640, 480); // assume landscape mode!
                Log.d(TAG, "surfaceChanged() --- Setting Landscape Cam Preview Size w/h: " + mCameraSize.width + " / " + mCameraSize.height);

                if (mCameraSize.width != 640 || mCameraSize.height != 480) {
                    NoPVSizeControl = true;
                } else {
                    cameraParameters.setPreviewSize(mCameraSize.width, mCameraSize.height); // (640,480);
                    mCamera.setParameters(cameraParameters);
                }
            }

            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

//        Log.d(TAG, "== surfaceDestroyed() entered.");

        mCamera.stopPreview();
        mHolder.removeCallback(this);
        mCamera.release();

        IPTemplate = null;
        IPDCZx     = null;
        IPDCZy     = null;
        IPDCZv     = null;
        IPHState   = null;
        IPHPopul   = null;
    }

   // @Override
   public  void Start(){
       cameraParameters = mCamera.getParameters();             //added this line

       boolean   testIt = false;  // turn on for testing

       if(cameraParameters == null || testIt) {
           AlertDialog.Builder alertDialog =
                   new AlertDialog.Builder(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);

           alertDialog.setIcon(android.R.drawable.ic_dialog_alert);

           alertDialog.setTitle(" --------- S T O P ---------");

           alertDialog.setMessage(R.string.CameraStartFailureWarning);

           alertDialog.setNegativeButton("Q U I T", new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                   mCamera.stopPreview();

                   // mActivity.finishAndRemoveTask(); // exit
               }
           });
           alertDialog.show();
           return;

       }

       int expMin         = cameraParameters.getMinExposureCompensation();
       int expMax         = cameraParameters.getMaxExposureCompensation();
       int expIndx        = cameraParameters.getExposureCompensation();
       int eVal           = 0;
       isFirstCal         = true;
       isFirstCal         = true;
       IPThreshReady      = false;
       IPTemplateLowReady = false;
       IPneedStartIP      = true;

       cameraParameters.getPreviewFormat();
       eVal = expIndx+1;
//	Log.d(TAG, " ----------- adjustCameraExposureMedium() setting Exposure Compensation to: "+eVal+ "(FS) = "+expRange);

       cameraParameters.setExposureCompensation(eVal);
       cameraParameters.setAutoExposureLock(true);

       Log.d(TAG, "!!!! Start() device model check: "+ model);
       if(model.contains("SM-G950")            // S8
               || model.contains("SM-G935")    // S7 Edge
               || model.contains("SM-G930")    // S7
               || model.contains("SM-G900")    // S6
               || model.contains("SM-N920")
               || model.contains("SM-G870")) { // Special Case!
           Log.d(TAG, "!!!! Start() Setting AutoExposureLock to False !!!!!");

           cameraParameters.setAutoExposureLock(false);
       }

       cameraParameters.setAutoWhiteBalanceLock(true);
       cameraParameters.setPreviewFpsRange(30000, 30000);
       // cameraParameters.setFocusMode(cameraParameters.FOCUS_MODE_AUTO);

       IPwidth  = cameraParameters.getPreviewSize().width;
       IPheight = cameraParameters.getPreviewSize().height;

//        Log.d(TAG, "Start() ipwidth / IPheight = " + IPwidth+" / "+IPheight);

       ROI[0] = 10; // 0;
       ROI[1] = IPheight - 10; // IPheight;
       ROI[2] = 10; // 0;
       ROI[3] = IPwidth - 10; // IPwidth

       mCamera  .setParameters(cameraParameters);
       statusBar.setText(R.string.Calibrating);
       statusBar.setAlpha(0.73f);

//        int BPP = (ImageFormat.getBitsPerPixel(cameraParameters.getPreviewFormat())) / 8;
       int           PVArySize = 3110400; /// IPwidth * IPheight * BPP; WATCH THIS LINE!!!!!!!!
       byte[]  mCallbackBuffer = new byte[PVArySize];
       mCamera.addCallbackBuffer(mCallbackBuffer);


       //this will always start the calibration, so we set the state to -1
       IPState                = C_WAIT;
       timesCalibrated        = 0;
       mIPFrame               = 0;

       List<String> supported_focus_values = cameraParameters.getSupportedFocusModes();

       for (String s : supported_focus_values){
           Log.d(" TestCamView-SFV: ", s);
       }

       int new_focus_index = supported_focus_values.indexOf(FOCUS_MODE_AUTO);

       if(new_focus_index < 0) {
           Log.d(TAG, "!! AutoFocus NOT Supported!");

           AlertDialog mAlertDialog = new AlertDialog.Builder(getContext()).create();

           if (mAlertDialog != null) {
               mAlertDialog.setTitle("Camera Incompatibility"); // ("Session Alert");
               String msg = "Your Device doesn't offer camera control required by this App. Please use a different device.";
               mAlertDialog.setMessage(msg);
               mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "\n       OK ",
                       new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int which) {

                               dialog.dismiss();

                               // quit camera
                               mCamera.stopPreview();
                           }
                       });
               mAlertDialog.show();

           }
           return;
       } else {
           Log.d(TAG, "autofocus works on this device!");
       }


       // run the auto-focus first to use as a delay
       mCamera.autoFocus(new AutoFocusCallback() {
           @Override
           public void onAutoFocus(boolean success, Camera camera) {
//                System.out.println("Auto focus Done");
               IPState = C_CALIB;
           }
       });

       final int          rx1 = ROI[2], rx2 = ROI[3];
       final int          ry1 = ROI[0], ry2 = ROI[1];
       final int        ryDim = ry2 - ry1 + 1;
       final int[]     rxHead = new int[ryDim];

       for (int i = 0; i < ryDim; i++) { rxHead[i] = (ry1+i) * IPwidth + rx1; }

       mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {

           final TextView mstatusBar = statusBar;

           @Override
           public void onPreviewFrame(final byte[] data, Camera camera) {
               timestamp  = System.currentTimeMillis();

               final int     tIPFrame = mIPFrame;
               final short  zeroShort = (short)0;
               final long  mtimestamp = timestamp;

//                Log.d(" ++++ onPrevFrame ", "Fm | Gap = " + tIPFrame + " | " + tGap);

               mIPFrame++;

               new Thread() {
                   public void run() {
                       //------------------------------------------------ Module grabs the frame and makes it into a BMP
                       final YuvImage               yuv       = new YuvImage(data, cameraParameters.getPreviewFormat(), IPwidth, IPheight, null);
                       final ByteArrayOutputStream  out       = new ByteArrayOutputStream();
                       final int                    mIPwidth  = IPwidth;
                       final int                    mIPheight = IPheight;
                       final int                    mIPState  = IPState;
                       final int                    mIPsize   = mIPwidth * mIPheight;
                       final int                    G_IPStartFrame1  = G_IPStartFrame + 1;
                       final int                    G_IPStartFrame2  = G_IPStartFrame + 2;
                       final int                    G_IPStartFrame10 = G_IPStartFrame + 10;
                       final Rect                   mRect = new Rect(0, 0, mIPwidth, mIPheight);

                       yuv.compressToJpeg(mRect, G_JPGQuality, out); // Output Jpeg quality 70

                       final byte[] bytes = out.toByteArray();
                       final int[]   pixs = new int[IPwidth * IPheight];
                       BitmapFactory.Options bitmap_options = new BitmapFactory.Options();
                       bitmap_options.inPreferredConfig     = Bitmap.Config.ARGB_8888;

                       final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bitmap_options);
                       bitmap.getPixels(pixs, 0, IPwidth, 0, 0, mIPwidth, mIPheight);

                       int[]     hstR = new int[256];
                       int       r, g, b;
                       int       bw, p;
                       int       indx;
                       int       mindx;

                       if (mIPState == C_CALIB) { // ready for Calib

                           for (int my = ry1, mi = 0; my < ry2; my++, mi++) {
                               mindx = rxHead[mi];  // mindx = my * IPwidth + rx1;
                               //	Log.d(TAG, "++++++++++++++++  setup indx = "+mindx);
                               for (int x = rx1; x < rx2; x++, mindx++) {
                                   p  = pixs[mindx];
                                   r  = (p >> 16) & 0x4c;
                                   g  = (p >> 8) & 0x96;
                                   b  = p & 0x1d;
                                   bw = r + g + b;

                                   hstR[bw]++;
                               }
                           }

                           //grab the bgimage before calibration, so it is bright

                           if (isFirstCal == true) {
//                                Log.d(TAG, " STart() - grabbing... ");
                               bgImage    = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                               isFirstCal = false;
                           }

                           // calibrate here
                           calibrate(hstR);
                       } else if (mIPState == C_DETECT) { // shooting mode

                           if (tIPFrame == G_IPFrameCountStop - 1) {
                               mActivity.runOnUiThread(new Runnable() {
                                   public void run()
                                   {
                                       stopShooting(1);
                                   }
                               });
                           }

                           else if (tIPFrame == 1) {
                               // do the prep clean-up
                               Log.d(TAG, "Start() cleaning up IPTemplate..... <"+tIPFrame+">");

                               for(int j=0; j<mIPsize; ++j) {
//                                   IPDCZMap[j]   = false;
                                   IPTemplate[j] = 0;
                               }
                           }

                           else if (tIPFrame >= (G_IPStartFrame - G_IPDCZFramesN) && tIPFrame < G_IPStartFrame) {
                               // set up the DCZ and IPTemplate

                               if (tIPFrame == (G_IPStartFrame - G_IPDCZFramesN)) {

                                   Log.d(TAG, "Start() -- initializing IPTemplate..... <"+tIPFrame+">");

                                   for (int my = ry1, mi = 0; my < ry2; my++, mi++) {
                                       mindx = rxHead[mi];  // mindx = my * IPwidth + rx1;

                                       for (int x = rx1; x < rx2; x++, mindx++) {
                                           p  = pixs[mindx];
                                           r  = (p >> 16) & 0x4c;
                                           g  = (p >> 8) & 0x96;
                                           b  = p & 0x1d;
                                           bw = r + g + b;
                                           hstR[bw]++;
                                           IPTemplate[mindx] = bw;  // 1st impression
                                           if (bw >= 220 && IPDCZcount < G_IPDCZcountMax) {
                                               // record position in IPDCZx & IPDCZy
                                               IPDCZx[IPDCZcount]   = x;
                                               IPDCZy[IPDCZcount]   = my;
                                               IPDCZv[IPDCZcount]   = bw;
                                               IPDCZcount++;
                                           }
                                       }
                                   }
                                   Log.d(TAG, "   ---- DCZ Size = "+IPDCZcount);
                               } else {

//                                    Log.d(TAG, "Start() --- accumulating IPTemplate..... <"+tIPFrame+">");

                                   for (int my = ry1, mi = 0; my < ry2; my++, mi++) {
                                       indx = rxHead[mi];  // indx = my * IPwidth + rx1;
                                       for (int x = rx1; x < rx2; x++, indx++) {
                                           p  = pixs[indx];
                                           r  = (p >> 16) & 0x4c;
                                           g  = (p >> 8) & 0x96;
                                           b  = p & 0x1d;
                                           bw = r + g + b;
                                           hstR[bw]++;
                                           IPTemplate[indx] += bw; // accumulated impression
                                           if (bw >= IPThresh && IPDCZcount < G_IPDCZcountMax) {
                                               IPDCZy[IPDCZcount] = my;
                                               IPDCZv[IPDCZcount] = bw;
                                               IPDCZcount++;
                                           }
                                       }
                                   }

                                   Log.d(TAG, "   ---- DCZ Size = "+IPDCZcount);
                               }

                               if (IPDCZcount >= G_IPDCZcountMax) { // case too bright
                                   Log.d(TAG, "   ---------- LightLevel Determination: Lighting too bright!");

                                   mActivity.runOnUiThread(new Runnable() {

                                       @Override
                                       public void run() {
                                           mstatusBar.setText("Scene may be too bright");
                                           mstatusBar.setAlpha(0.73f);
                                           mActivity .changeStatusBar("Scene may be too bright");
                                       }
                                   });
                               }

                               int i = 255;
                               int tPop;

                               threadHB = 255;
                               while (i > 0) {
                                   tPop = hstR[i];
                                   //NSLog(@"tpop %i", tPop);
                                   if (tPop > 4) {
                                       threadHB = i;
                                       break;
                                   }
                                   i--;
                               }

                               if (threadHB < 250) {
                                   if (threadHB > head) {
                                       head = threadHB;
                                   }

                                   if (threadHB > IPThresh) {
                                       IPThresh = threadHB;
                                   }
                               }

//                                Log.d(TAG, "threadhb = " + threadHB);

                               //let user know if the scene might be too bright
                               if(threadHB > 240)
                               {
                                   mActivity.runOnUiThread(new Runnable() {

                                       @Override
                                       public void run() {
                                           mstatusBar.setText("Scene may be too bright");
                                           mstatusBar.setAlpha(0.73f);
                                           mActivity .changeStatusBar("Scene may be too bright");
                                       }
                                   });
                               }
                           }

                           // collect interested pixels for hit ID
                           else if (tIPFrame >= G_IPStartFrame) {
                               short[]          xpo = new short[G_IPHCSizeMax]; // int[]  xpo = new int[G_IPHCSizeMax];
                               short[]          ypo = new short[G_IPHCSizeMax]; // int[]  ypo = new int[G_IPHCSizeMax];
                               short[]          val = new short[G_IPHCSizeMax]; // int[]  val = new int[G_IPHCSizeMax];
                               int           posLen = 0;                        // real data length
                               boolean    endBySize = false;
                               boolean          hit;

                               if(tIPFrame > G_IPStartFrame10) {

                                   for (int my = ry1, mi = 0; my < ry2; my++, mi++) {
                                       indx = rxHead[mi];  // indx = my * IPwidth + rx1;
                                       for (int x = rx1; x < rx2; x++, indx++) {
                                           p  = pixs[indx];
                                           r  = (p >> 16) & 0x4c;
                                           g  = (p >> 8) & 0x96;
                                           b  = p & 0x1d;
                                           bw = r + g + b;

                                           if (bw >= IPThresh) {
                                               xpo[posLen] = (short) x;
                                               ypo[posLen] = (short) my;
                                               val[posLen++] = (short) bw;
                                               if (posLen >= G_IPHCSizeMax) {
                                                   my = IPheight;
                                                   endBySize = true;
                                                   break;
                                               }
                                           }

                                           if (endBySize) break;
                                       }
                                       if (endBySize) break;
                                   }
                               }

                               if (tIPFrame == G_IPStartFrame1) { // Threshold determination, No Detection!

                                   /// record IPStarTime
                                   IPStarTime = System.currentTimeMillis();

                                   mActivity.runOnUiThread(new Runnable() {

                                       @Override
                                       public void run() {
                                           mstatusBar.setText("Analyzing");
                                           mstatusBar.setAlpha(0.73f);
                                           mActivity .changeStatusBar("Analyzing");
                                       }
                                   });

                                   cameraParameters = mCamera.getParameters();

                                   cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);

                                   // cleanup DCZ
                                   if (IPDCZcount > 1) {
                                       int fIPDCZcount = IPDCZcount;
                                       for (int ii = IPDCZcount - 1; ii > 0; ii--) {

                                           int       xa = IPDCZx[ii];
                                           int       ya = IPDCZy[ii];
                                           boolean done = false;
                                           for (int jj = ii - 1; jj >= 0; jj--) {
                                               if (xa == IPDCZx[jj] && ya == IPDCZy[jj]) {
                                                   // remove the current ii from list
                                                   for (int kk = ii + 1; kk < fIPDCZcount; kk++) {
                                                       IPDCZx[kk - 1] = IPDCZx[kk];
                                                       IPDCZy[kk - 1] = IPDCZy[kk];
                                                   }
                                                   fIPDCZcount--;
                                                   IPDCZx[fIPDCZcount] = -1;
                                                   IPDCZy[fIPDCZcount] = -1;
                                                   done = true;
                                               }
                                               if (done) break;
                                           }
                                       }
                                       IPDCZcount = fIPDCZcount;

                                       Log.d(TAG, "   ---- DCZ Final Size = "+IPDCZcount);
                                   } else {
                                       Log.d(TAG, "   ---- DCZ is empty! ");
                                   }

                                   int cc1 = GetThreshold(IPThresh, IPThreshLow, IPBKLow);
                                   int cc2 = GetThresholdLow(IPThresh, IPThreshLow, IPBKLow);
                                   int cc3 = GetIPBK(IPThresh, IPThreshLow, IPBKLow);

                                   IPThresh    = cc1;
                                   IPThreshLow = cc2;
                                   IPBKLow     = cc3;

                                   Log.d(TAG, "Start() >>>> IPThresh / IPThreshLow / IPBKLow " + IPThresh+" / "+IPThreshLow+" / "+IPBKLow);
                               }
                               else if (tIPFrame == G_IPStartFrame2) { // finalize IPTemplate
                                   Log.d(TAG, "Start()  ---- finalizing IPTemplate..... <"+tIPFrame+">");

                                   for (int j = 0; j < mIPsize; j++) { IPTemplate[j] /= G_IPDCZFramesN; }
                               }

                               ////// practice starts here
                               else {
                                   if(tIPFrame == G_IPStartFrame10)
                                   {
                                       // Play sound
                                       mActivity.sdpool.play(mActivity.bzSoundID, 0.99f, 0.99f, 1, 0, 0.99f);

                                       mActivity.runOnUiThread(new Runnable() {

                                           @Override
                                           public void run() {
                                               mstatusBar.setText("Begin shooting");
                                               mstatusBar.setAlpha(0.73f);
                                               mActivity. changeStatusBar("Begin shooting");
                                           }
                                       });

                                   }

                                   //look for bad spots here
                                   if((tIPFrame < G_IPStartFrame10) && posLen >=2)
                                   {
                                       //mark the spot and  STOP
                                       int     q = 0;
                                       int  xAvg = 0;
                                       int  yAvg = 0;
                                       int count = 0;

                                       while(q < posLen) {
                                           if((xpo[q]>=0)&&(ypo[q]>=0))
                                           {
                                               xAvg = xAvg + xpo[q];
                                               yAvg = yAvg + ypo[q];
                                               count++;
                                           }
                                           q++;
                                       }

                                       xAvg = xAvg/count;
                                       yAvg = yAvg/count;

                                       final int xFinal = xAvg;
                                       final int yFinal = yAvg;

                                       mActivity.runOnUiThread(new Runnable() {

                                           @Override
                                           public void run() {
                                               mstatusBar.setText("This may cause issues");
                                               mstatusBar.setAlpha(0.73f);
                                               mActivity .changeStatusBar("This may cause issues");
                                               showBadSpot(xFinal, yFinal);
                                               stopShooting(2);
                                           }
                                       });

                                   }

                                   final int IPDCZcount4 = IPDCZcount + 4;

                                   if (posLen > 4 && posLen < IPDCZcount4) {
                                       // Post Processing -
                                       // 1. clenup against DCZ pixels

                                       IPHPopul[tIPFrame] = posLen;
                                       int        fposLen = posLen;
                                       final int    mDist = 3;
                                       boolean    IsSmall = false;

                                       for (int j = posLen - 1; j >= 0; j--) {
                                           int xa = xpo[j];
                                           int ya = ypo[j];
                                           for (int k = 0; k < IPDCZcount; k++) {
                                               if ((Math.abs(xa - IPDCZx[k]) + Math.abs(ya - IPDCZy[k])) <= mDist) {
                                                   // ignore this one
                                                   for (int ii = j; ii < posLen - 1; ii++) {
                                                       xpo[ii] = xpo[ii + 1];
                                                       ypo[ii] = ypo[ii + 1];
                                                       val[ii] = val[ii + 1];
                                                   }
                                                   fposLen--;
                                                   if(fposLen < 4) IsSmall = true;

                                                   if(fposLen > 1 && !IsSmall) {
                                                       xpo[fposLen - 1] = -1;
                                                       ypo[fposLen - 1] = -1;
                                                       val[fposLen - 1] = -1;
                                                   }

                                                   break;
                                               }
                                           }
                                           if(IsSmall) break;
                                       }

                                       posLen = fposLen;
                                   }

                                   if (posLen > 4) {
                                       boolean isHit = true;
                                       IPHState[tIPFrame] = (IPHState[tIPFrame - 1] >= 0) ? IPHState[tIPFrame - 1] + 1 : 0;
                                       // Valid Candidate Seq Index
                                       IPHPopul[tIPFrame] = posLen;

                                       if (isHit) {    //// Keep the record
                                           if (posLen > G_IPHRecordMax) {
                                               posLen = G_IPHRecordMax; // Force it to reduce!
                                               IPHPopul[tIPFrame] = posLen;
                                           }

                                           //add to list of hits
                                           laserHit newhit = new laserHit();

                                           newhit.setup(mtimestamp);
                                           newhit.posLen = posLen;
                                           newhit.state  = 1;
                                           newhit.FmNum  = tIPFrame;
                                           newhit.hitID  = shots+1;

                                           int j = 0;

                                           while (j < posLen) {
                                               //add only if >0, so we don't have the -1 problem
                                               if ((xpo[j] >= 0) && (ypo[j] >= 0)) {
                                                   newhit.add(xpo[j], ypo[j], zeroShort, val[j]);
                                                    /*
                                                    if(G_IsBeta) {
                                                        String slog =  "    -- Fm< "+tIPFrame +" >  newHit Coord[ "+j+ "] :" + xpo[j]+", "+ypo[j];
                                                        appendLog(slog);
                                                    }
                                                    */

                                               }
                                               j++;
                                           }

                                           //add to array here
                                           HitList.add(newhit);

                                           if (IPHFIndx < G_IPHitFramesMax - 1) IPHFIndx++;
                                       }
                                   } else {
                                       if (IPHState[tIPFrame - 1] == 0) {
                                           if (IPHPopul[tIPFrame - 1] >= 6) { // this is a single frame case with big size, take it!
                                               hit = true;
                                               Log.d(TAG, "=== Hit Frame Detected: Single: "+(tIPFrame - 1)+" Shot# "+shots);
                                               shots++;

                                               HitStarts.add(tIPFrame - 1);
                                               showShotforStartFrame(tIPFrame - 1);

                                               mActivity.sdpool.play(mActivity.ssSoundID, 0.99f, 0.99f, 1, 0, 0.99f);
                                           } else {
                                               hit = false;
                                           }
                                           IPNewSeq = false; // reset Hit Sequence Flag
                                           IPHSeqID = -1;
                                       } else if (IPHState[tIPFrame - 1] > 0) { // this is the end of a sequence, post proc is needed!
                                           hit = false;
                                           int seqSize = IPHState[tIPFrame - 1] + 1;
                                           // We expect the 2X of the BBoxes overlap and that their centers are close by for the entire sequence
                                           // a. Calculate the average BBox size
                                           // b. Calculate the Super BBox size
                                           // c. Compare the 2 sizes
                                           for (int j = tIPFrame - seqSize; j < tIPFrame; j++) {
                                               hit = false;
                                               if (IPHPopul[j] >= 4) { // this frame in the sequence has a large hot pixel population, take it!
                                                   hit = true;

                                                   Log.d(TAG, "=== Hit Frame Detected: Large: "+j+" Shot# "+shots);

                                                   HitStarts.add(j);
                                                   showShotforStartFrame(j);
                                               }
                                               if (hit) break;
                                           }

                                           if (hit) { // record time and hit location
                                               //                                        Log.d("hit", "hit");
                                               shots++;
                                               Log.d(TAG, "=== Hit Frame Detected: Multi: "+(tIPFrame - seqSize)+" Shot# "+shots);
                                               mActivity.sdpool.play(mActivity.ssSoundID, 0.99f, 0.99f, 1, 0, 0.99f);
                                           } else {

                                           }
                                           IPNewSeq = false; // reset Hit Sequence Flag
                                           IPHSeqID = -1;
                                       }
                                   }
                               }
                           }

//                            Log.d("shots", "shots =" +shots);
                           if (shots >= 10) {

                               mActivity.runOnUiThread(new Runnable() {
                                   public void run()
                                   {
                                       stopShooting(1);
                                   }
                               });
                           }
                       } // (shots >= 10)

                       bitmap.recycle();
                   }
               }.start(); // Thread()

               mCamera.addCallbackBuffer(data);

           } // onPreviewFrame()
       });

   } // start()



    //---------------------------------------------------- Drawing functions --------------------------------------------------------
    private void showBadSpot(int x, int y)

    {
        //now draw onto that location
        Bitmap bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bgImage, new Matrix(), null);

        Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.warnmarker),80,80,false);

        canvas.drawBitmap(hitMarker, x-20, y-20, null);

        //need to rotate this 90 degrees
        Matrix matrix = new Matrix();
        matrix.preRotate(90);

        final Bitmap finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);

        mActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mActivity.drawImage(finalImg);
////                finalImg.recycle();
            }
        });

        bmOverlay.recycle();
        hitMarker.recycle();

    }


    //---------------------------------------------------- State Change functions --------------------------------------------------------

    private void startIP( )
    {
        if(IPState == C_DETECT) {
            Log.d(TAG, "\n\n StartIP() aborted - (by State) already in Detection State...");
            return;
        } else if(mIPFrame <= 5) { // for somewhat unexpected conditions when IPState wasn't updated quick enough
            Log.d(TAG, "\n\n StartIP() aborted - (by Frm #) already in Detection State...");
            return;
        } else {
            Log.d(TAG, "\n\n StartIP() entered...");
        }

        IPState = C_DETECT;


        {
            Matrix matrix = new Matrix();
            matrix.preRotate(90);

            final Bitmap finalbgImage = Bitmap.createBitmap(bgImage, 0, 0, bgImage.getWidth(), bgImage.getHeight(), matrix, true);

            mActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mActivity.drawImage(finalbgImage);
                }
            });
        }

        /*
        {

            Thread thread = new Thread() {
                @Override
                public void run() {
                    final Matrix matrix = new Matrix();
                    matrix.preRotate(90);

                    final Bitmap finalbgImage = Bitmap.createBitmap(bgImage, 0, 0, bgImage.getWidth(), bgImage.getHeight(), matrix, true);
                    mActivity.drawImage(finalbgImage);

                }
            };
            thread.start();
        }
        */

        HitList             = new ArrayList<laserHit>();
        HitStarts           = new ArrayList<Integer>();
        mIPFrame            = 0;
        IPThresh            = 0;
        IPSlowWarned        = false;
        IPShootingIsStopped = false;
        ISOinAction         = false;

        for(int s=0; s<G_IPFrameCountMax; s++) {

            for (int k = 0; k < G_IPDCZcountMax; k++) {
                IPDCZx[k] = -1;
                IPDCZy[k] = -1;
            }

            IPHState[s] = -1;
            IPHPopul[s] = 0;
        }

        //chop off 10 pixels off the edges to minimize edge noise
        ROI[0]     = 10;
        ROI[1]     = IPheight-10;
        ROI[2]     = 10;
        ROI[3]     = IPwidth-10;
        threadHB   = 175;
        IPHFIndx   = 0;
        IPNewSeq   = false; // reset Hit Sequence Flag
        IPHSeqID   = -1;
        IPDCZcount = 0;
        shots      = 0;

        mActivity.stateChange(1);     // mActivity.stateChange(1);
        IPState    = C_DETECT;        // 1;

        return;
    }


    //state 1 is test passed, 2 is detected bad spot
    public void  stopShooting(int state)
    {
        Log.d(TAG, "--- stopShooting( "+state+" ) entered; shots=" +HitList.size());

        if(IPShootingIsStopped) return;

        IPShootingIsStopped = true;
        IPState             = C_POSTPROC;
        mActivity.stateChange(C_POSTPROC);

//        Log.d(TAG, "--- stopShooting() shots=" +HitList.size());
//        Log.d(TAG, "--- stopShooting() hitstarts=" +HitStarts.size());

        IPEndTime          = System.currentTimeMillis();
        long IPSessionTime = IPEndTime - IPStarTime;
        IPFmRate           = (IPSessionTime < 2000)? 0 : (int)((mIPFrame-G_IPStartFrame)/(IPSessionTime/1000L));

        if(state == C_DETECT) {
            currentreviewShot = -1;             // always start on show all
            showAllShots();
        }

        IPState = C_POSTPROC;
        mCamera.stopPreview();


        if(IPFmRate < 20 && !IPSlowWarned) {
            AlertDialog mAlertDialog = new AlertDialog.Builder(getContext()).create();

            if(mAlertDialog != null) {
                mAlertDialog.setTitle("Performance Warning"); // ("Session Alert");
                String msg = "Your device is running very slow. ("+(IPFmRate*3.33f)+"%) Please consider switching to a faster device.";
                mAlertDialog.setMessage( msg );
                mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "\n       OK ",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                IPSlowWarned = true;
                                dialog.dismiss();
                            }
                        });
                mAlertDialog.show();
                return;
            }
        } else if (IPFmRate < 25 && !IPSlowWarned) {
            AlertDialog mAlertDialog = new AlertDialog.Builder(getContext()).create();

            if(mAlertDialog != null) {
                mAlertDialog.setTitle("Performance Warning"); // ("Session Alert");
                String msg = "Your device is running slow. ("+IPFmRate*3.33f+"%) Please turn off all nonessential Apps!";
                mAlertDialog.setMessage( msg );
                mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "\n       OK ",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                IPSlowWarned = true;
                                dialog.dismiss();
                            }
                        });
                mAlertDialog.show();
                return;
            }
        }
    }

    public void reset()
    {
        IPState = C_WAIT;
        mActivity.stateChange(0);
        mActivity.drawImage(null);

        //reset arrays
        HitList  .clear();
        HitStarts.clear();

//  Log.d("reset", "reset");

        // restart the camera

        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.startPreview();

        cameraParameters = mCamera.getParameters();

        cameraParameters.setExposureCompensation(0);
        cameraParameters.setAutoExposureLock(false);
        cameraParameters.setFocusMode(FOCUS_MODE_AUTO);
        cameraParameters.setAutoWhiteBalanceLock(false);

        int expMin = cameraParameters.getMinExposureCompensation();
        int expMax = cameraParameters.getMaxExposureCompensation();

        if (expMax - expMin < 10) { // Reserve ISO space for device that don't offer enough exposure adjustment
            String supportedISOs = cameraParameters.get("iso-values");
            Boolean useSpeedVal  = false;
            if (supportedISOs == null) {
                supportedISOs = cameraParameters.get("iso-speed-values");
                useSpeedVal = true;
            }

            if (supportedISOs != null) {
                String[] supportedISOsAry = supportedISOs.split(","); //supported values, comma separated String
//                String cISO = cameraParameters.get("iso");
                int i = 0;

                for (; i < supportedISOsAry.length; i++) {
                    if (supportedISOsAry[i].equals("400")) { // 200
                        if(useSpeedVal) {
                            cameraParameters.set("iso-speed", "400"); // 200
//                            Log.d("=== CameraView", "  ------------ reset() set ISO SPEED to 400!");
                        } else {
                            cameraParameters.set("iso", "400"); // 200
//                            Log.d("=== CameraView", "  ------------ reset() set ISO to 400!");
                        }
                        break;
                    }
                }

            } else {
                Log.d(TAG, "  ------------ reset() Can't Set ISO; not supported.");
            }
        }

        mCamera  .setParameters(cameraParameters);
        statusBar.setText(R.string.zoomMessage);
        statusBar.setAlpha(0.73f);
    }

    //---------------------------------------------------- calibration functions --------------------------------------------------------

    private void calibrate(int[] hist)
    {
        if(timesCalibrated > 4 || IPState != C_CALIB) return;

        int  i = 255;
        head   = 0;
        while (i > 0)
        {
            if(hist[i] > 4) {
                head = i;
                break;
            }
            i--;
        }

        Log.d(TAG, "calibrate() head = " + head);

        double adjust;

        //we want below 160
        if (head > 220) {
            adjust = -2.0;
        } else if (head > 200) {
            adjust = -1.75;
        } else if (head > 150) {
            adjust = -.75;
        } else if (head < 50) {
            adjust = 1.15;
        } else if (head < 90) {
            adjust = .5;
        } else {
            adjust = 0;
        }

        Log.d(TAG, "  calibrate(): adjust value = " + adjust+" | state = "+IPState);
        //adjustValue is EV
        adjustExposure(adjust);

    }

    private void adjustExposure(double adjustValue)
    {
        if(IPAutoFocusBusy || IPState == C_PARAMCAL) {
            Log.d(TAG, "=== adjustExposure() aborted due to same active State ===");
            return;
        }

        //adjustValue is EV
        cameraParameters = mCamera.getParameters();
        int      expMin  = cameraParameters.getMinExposureCompensation();
        int      expMax  = cameraParameters.getMaxExposureCompensation();

        // make sure it's supported!
        if(expMin == 0 && expMax == 0) {
            Log.d(TAG, "=== adjustExposure(): Feature Not Supported on this device; reset Frm No to -1!");

            timesCalibrated = 10;
            IPState         = C_PARAMCAL;
            mIPFrame        = -1;

            return;
        }


        int     expIndx  = cameraParameters.getExposureCompensation();
        float   expStep  = cameraParameters.getExposureCompensationStep();
        double       EV  = expIndx * expStep;
        double      wEV  = EV + adjustValue;
        int      wIndex  = (int)(wEV/expStep);

        Log.d(TAG, "=== adjustExposure(): wanted exposure | current | range = " + wIndex+" | "+expIndx+" | "+expMin +" - "+expMax);
        if(wIndex < 1 && wIndex > -1) {

            return;
        }

        // make sure we're in range
        if((wIndex >= expMin)&&(wIndex <= expMax))
        {
            Log.d(TAG, "=== adjustExposure: setting to original given value: "+(int)wIndex);
            cameraParameters.setExposureCompensation((int)wIndex);
            mCamera.setParameters(cameraParameters);
        } else {
            if (wIndex > 0 && expIndx < expMax) {
                wIndex = expMax;
                cameraParameters.setExposureCompensation((int)wIndex);
                mCamera.setParameters(cameraParameters);

                Log.d(TAG, "   === adjustExposure() set exposure to " + (int)wIndex);
            } else if (wIndex < 0 && expIndx > expMin) {
                wIndex = expMin;
                cameraParameters.setExposureCompensation((int)wIndex);
                mCamera.setParameters(cameraParameters);

                Log.d(TAG, "   === adjustExposure() set exposure to " + (int)wIndex);
            } else if (!ISOinAction ) { // apply ISO change
                ISOinAction = true;
                ISOValues   = getISOValues();
                String[] supportedISOs = ISOValues.split(",");

                if(ISOValuesParameter != null) {

                    ISOParameter = ISOValuesParameter.replace("-values", "");

                    for (int i = 0; i < supportedISOs.length; i++) {
                        if (supportedISOs[i].equals("100")) {
                            cameraParameters.set(ISOParameter, "100"); // 200
                            Log.d(TAG, "  -------adjustExposure() setting ISO to 100!");

                            break;
                        }
                    }

                    mCamera.setParameters(cameraParameters);
                } else {
                    Log.d(TAG, "  ------------ adjustExposure() Can't Set ISO; not supported.\n    ** Early termination of calibrate()**");
                    // early exit
                    timesCalibrated = 100;
                    ISOinAction     = false;
                    IPState         = C_PARAMCAL;  //  startIP();

                    startIP();

                    mIPFrame        = -1;
                    return;
                }

                timesCalibrated = 100;
                Log.d(TAG, "  -------adjustExposure() setting timesCalibrated to 100!");
            }
        }

        if( !IPAutoFocusBusy ) {
            //run the auto focus first to use as a delay
//            Log.d(TAG, "  -------adjustExposure() autoFocus activated...");

            mCamera.autoFocus(new AutoFocusCallback() {

                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    timesCalibrated++;
                    IPAutoFocusBusy = false;

                    if (timesCalibrated >= 3) {
                        //if we've calibrated 3 times, we can start the IP, state change is handled in the function

                        IPState  = C_PARAMCAL;  // C_DETECT;
                        startIP();
                        mIPFrame = -1;
                    } else {
//                        Log.d(TAG, "\n\n adjustExposure() AutoFocus Done; keep IPState to C_CALIB; timesCalibrated =" + timesCalibrated);

                        IPState = C_CALIB;
                    }
                }
            });
            IPAutoFocusBusy = true;
        }
    }


    // returns a list with supported ISO values
    private String  getISOValues() {

        ISOValuesParameter = getISOValuesParameter();

        ISOValues          = cameraParameters.get(ISOValuesParameter);

        if(ISOValues == null) {
            Log.d(TAG, " XXX getISOValues() - current device doesn't support ISO change XXX");
        } else {
            Log.d(TAG, " XXX getISOValues() acquired values.");
        }

        return ISOValues!=null ? ISOValues : "ISO not supported";
    }


    // this will return the name of the ISO parameter containing supported ISO values
    private String  getISOValuesParameter() {

        String   flatten         = cameraParameters.flatten();
        String[] paramsSeparated = flatten.split(";");

        // For debug
        Log.d(TAG, "Resetting to saved camera params: \n\n");
        for(String cur : paramsSeparated) {
            Log.d(TAG, " -- " + cur);
        }
        Log.d(TAG, " ------------------------------------------ \n\n");


        for(String cur : paramsSeparated) {
            if(cur.contains("iso") && cur.contains("values")) {
                return cur.substring(0,cur.indexOf('='));
            }
        }

        return null;
    }


    public Camera.Size getOptimalPreviewSize(List<Camera.Size> PVsizes, int w, int h) {
        if (PVsizes == null) return null;

        final double ASPECT_TOLERANCE = 0.1;
        final double minDiff      = 15.0;
        double       targetRatio  = (double) (640.0 / 480.0);
        int          targetHeight = 480;
        int          width, height;
        Camera.Size  optimalSize  = null;

        for (Camera.Size size : PVsizes) {
            width  = size.width;
            height = size.height;

            if (width > G_MAXPREVIEWDIMENSION || height > G_MAXPREVIEWDIMENSION) continue;

            double ratio = (double)width / (double) height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;

            if (Math.abs(height - targetHeight) < minDiff) {
                optimalSize = size;
                break;
            }
        }

        if (optimalSize == null) {
            for (Camera.Size size : PVsizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    break;
                }
            }
        }
        return optimalSize;
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int  action = event.getAction();
        int keyCode = event.getKeyCode();

        if(mActivity.mAudioManager == null) {
            Log.d(TAG, "  dispatchKeyEvent() - mAudioManager is NULL!");
            return super.dispatchKeyEvent(event);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    mActivity.SpeakerVol++;
                    if(mActivity.SpeakerVol <= mActivity.mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
                        mActivity.mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mActivity.SpeakerVol, AudioManager.FLAG_SHOW_UI);

                        Log.d(TAG, "  == SoundPool volume forced to: " +mActivity.mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) +
                                " | VolumeFixed = " + mActivity.mAudioManager.isVolumeFixed());

                        mActivity.editor.putInt("SpeakerVolume", mActivity.SpeakerVol);
                        mActivity.editor.commit();
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    mActivity.SpeakerVol--;
                    if(mActivity.SpeakerVol >= 0) {
                        mActivity.mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mActivity.SpeakerVol, AudioManager.FLAG_SHOW_UI);

                        Log.d(TAG, "  == SoundPool volume forced to: " + mActivity.mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) +
                                " | VolumeFixed = " + mActivity.mAudioManager.isVolumeFixed());

                        mActivity.editor.putInt("SpeakerVolume", mActivity.SpeakerVol);
                        mActivity.editor.commit();
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    //---------------------------------------------------- shooting in progress functions --------------------------------------------------------

    private  void showShotforStartFrame(int frame)
    {
//        Log.d(TAG, "showShotforStartFrame() entered; Frame = " +frame);

        laserHit wantedHit;
        Bitmap   bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
        Canvas      canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bgImage, new Matrix(), null);

        for(int i = 0; i<HitList.size(); i++)
        {
            wantedHit = HitList.get(i);

            if(wantedHit.FmNum == frame)
            {
                Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker),40,40,false);

                Log.d(TAG, "showShotforStartFrame() calling findAvgXYPlus(); Frame = " +frame);

                int[]  sPos = wantedHit.findAvgXYPlus(5000, true);
                if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0]-20, sPos[1]-20, null);

                Matrix matrix = new Matrix();
                matrix.preRotate(90);

                final Bitmap finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.drawImage(finalImg);
                    }
                });

                break;
            }
        }
    }


    //----------------------------------------------------- review functions ---------------------------------------------------------------


    private void showAllShots()
    {
        // Display all hits; assumes entrance PState = C_POSTPROC;

        statusBar.setText(+shots + " shots detected (" +IPFmRate+" FPS)");
        statusBar.setAlpha(0.73f);

        Log.d(TAG, " showAllShots( ) entered...");

        Bitmap bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
        Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker), 40, 40, false);
        Canvas canvas    = new Canvas(bmOverlay);
        canvas.drawBitmap(bgImage, new Matrix(), null);

        int      wantedFrame;
        laserHit wantedHit;

        if(HitStarts != null) {
            for (int i = 0; i < HitStarts.size(); i++) {
                wantedFrame = HitStarts.get(i);
        //            Log.d(TAG, "showAllShots() hitStart" +wantedFrame);

                for (int j = 0; j < HitList.size(); j++) {
                    wantedHit = HitList.get(j);

        //                Log.d(TAG, "showAllShots() wantedHit" +wantedHit.FmNum);

                    if (wantedHit.FmNum == wantedFrame) {
                        int[]  sPos = wantedHit.findAvgXYPlus(5000, true);
                        if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0]-20, sPos[1]-20, null);

                        break;
                    }
                }
            }
        }

        Matrix matrix = new Matrix();
        matrix.preRotate(90);

        final  Bitmap finalImg = Bitmap.createBitmap(bmOverlay , 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.drawImage(finalImg);
            }
        });
        hitMarker.recycle();

    }

    public void appendLog(String text)
    {
        File logFile = new File("sdcard/LTPlog_Test.file");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public  void setActivity(TestActivity TestActivity) {
        mActivity = TestActivity;
    }
    public native int    GetThreshold(int i1, int i2, int i3);
    public native int    GetThresholdLow(int i1, int i2, int i3);
    public native int    GetIPBK(int i1, int i2, int i3);
}
