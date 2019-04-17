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

 /*  - change history

   11/02/2018  Rob   Changed G_IPStartFrame from 20 to 18 and G_IPDCZFramesN from 13 to 10 to
                     allow for detection of quicker first shot.

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
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.get.gsappalpha1.ImgProc.GrayProc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;


public class TimerCameraView extends SurfaceView implements SurfaceHolder.Callback {
    private   SurfaceHolder mHolder;
    private   Camera        mCamera;
    public    TimerActivity mActivity;

    private   int[]             ROI = new int[4];
    private   Bitmap            bgImage;              // the background image for review; rotates by 90 degrees
    private   Bitmap            DBGimage;             // dumb background image for testing/general use; rotates by 90 degrees
    private   List<Camera.Size> CamPreviewSizes;

    // shot-timer session parameters
    public boolean GetReadyMuteOn = false;   // to mute the "Get Ready" sound
    public boolean RandomModeOn   = true;    // to use the Random Timer (instead of Fixed)

    public int SessionsLimit      = 2;       // # of sessions to be performed
    public int ShotsLimit         = 3;       // max number shots each session can have. Session ends when this number is reached or time runs out
    public int SessionDelay       = 5;       // time between sessions. Starts counting when session ends and stops when the next session begins.
    public int SessionLength      = 15;      // 10;  max time in each session; default 10 (Sec)
    public int CountDown          = 5;       // time before the first session starts. Give the user time to get into position etc.
    public int DeviceTimerRating;
    private int currentSession;
    private int sessionIndx;              // currentSession - 1;

    private int shots;                    // number of shots detected in all sessions
    public  int sHits;                    // current session hits count

    //States
    private static final int S_WAIT      = -2;
    private static final int S_CALIB     = -1;
    private static final int S_PARAMCAL  = 0;
    private static final int S_COUNTDOWN = 1;
    private static final int S_RANDOM    = 2;
    private static final int S_DETECT    = 3;
    private static final int S_POSTPROC  = 4;
    private static final int S_REVIEW    = 5;
    private static final int S_DONE      = 6;
    private static final int S_PAUSE     = 7;

    private int IPState = S_WAIT;               //    state for IPCamera, -2: calibration wait until completion,
    //                        -1: calibration in progress,
    //                         0: pre-detection; detections param calculation,
    //                         1: countdown in progress,
    //                         2: random pause (after READY call) in progress,
    //                         3. shooting started; detection in progress,
    //                         4: break period (no detection) in between sessions,
    //                         5: review,
    //                         6. done.

    private int     timesCalibrated;            // actions taken so far in cam calibration
    private boolean isFirstCal;                 // is this the first calibration run? If it is, we want to grab the frame for bgImage
    public  boolean IPIgnoreTouch;

    //camera settings
    private Camera.Parameters cameraParameters;
    private  Camera.Size      mCameraSize;
    private int IPwidth;     // s.b. 480
    private int IPheight;    // s.b. 640

    private static final int G_MAXPREVIEWDIMENSION = 640; // Global! - check external files when updating!
    private              int         previewHeight = 640;
    private              int         previewWidth  = 480;

    //detection
    private static final int        G_IPStartFrame = 18;      //  20;     // Starting Frame for real detection (after auto threshold determination)
    private static final int        G_IPDCZFramesN = 10;      //  13;     //// 15;     // no. of DCZ source frames
    private static final int       G_IPDCZcountMax = 1000;   // max # of Don't Care Zones
    private static final int        G_IPHRecordMax = 3000;   // max # of hit pixel records in a frame; (ref. G_IPFrameDataCountMax in IPFrameData.java)
    private static final int         G_IPHCSizeMax = 3000;   // max # of hit pixels in a single frame
    private static final int      G_IPHitFramesMax = 500;    // max # of frames containing hits
    private static final int     G_IPFrameCountMax = 12000;  // 9350;   // max # of frames in a session (31 Fm/Sec x 60 Sec x 6 (5) Min)
    private static final int          G_JPGQuality = 70;
    private static final int         G_MAXSESSIONS = 8;


//    private boolean[]    DCZMap = new boolean[640 * 480];
    private int[]    IPTemplate = new int[640 * 480];        // [previewHeight * previewWidth];
    private int      IPDCZcount = 0;
    private int[]        IPDCZx = new int[G_IPDCZcountMax + 5];
    private int[]        IPDCZy = new int[G_IPDCZcountMax + 5];
    private int[]        IPDCZv = new int[G_IPDCZcountMax + 5];
    private int[]      IPHState = new int[G_IPFrameCountMax + 10]; // hit State of a Frame
    private int[]      IPHPopul = new int[G_IPFrameCountMax + 10]; // hit population of a Frame
    public int         IPHFIndx = 0;
    public boolean     IPNewSeq = false;    // reset Hit Sequence Flag
    public boolean       IPDone = false;
    public boolean     IPActive = false;
    public boolean    IPCountOn = false;
    public boolean    IPinCalib = false;
    public boolean IPinStopping = false;
    public boolean IPinSessionStoppng = false;
    public boolean ReadyTimerInAction = false;
    public boolean IPTimerDetectReady = false;
    public int               IPHSeqID = -1;
    public int               IPFmRate = 0;
    public long            IPStarTime = 0L;
    public int[]      IPSessionFmRate = new int [G_MAXSESSIONS];
    public long[]  IPSessionStartTime = new long[G_MAXSESSIONS];  // 0L;
    public long[]    IPSessionEndTime = new long[G_MAXSESSIONS];  // 0L;
    public int[]  IPSessionStartFrame = new int [G_MAXSESSIONS];   // 0;
    public int[]    IPSessionEndFrame = new int [G_MAXSESSIONS];   // 0;
    public int[]        IPSessionHits = new int [G_MAXSESSIONS];   // 0;
    public  boolean      needDBGimage;
    private boolean   NoPVSizeControl = false;
    private boolean  IPAutoFocusBusy  = false;
//    public Bitmap           IPbitmap2;

    private ArrayList<Integer>  HitStarts;   // original Frame Index of the 1st Frame of recorded hits
    private ArrayList<Integer>  HitSMaster;  // Master Frame Index List of the 1st Frame of hits; including Timestamp & Session Hit Count-Headers
    private ArrayList<laserHit> HitList;     // detailed Frame information of recorded hit Frames (more than just the 1st Frame)

    private int      IPBKLow;
    private int      IPThresh;
    private int      IPThreshLow;
    private int      threadHB;
    private int      head;
    private int      mIPFrame;
    private int      CalibFrame;

    private String   ISOValuesParameter = null;
    private String   ISOParameter       = null;
    private String   ISOValues          = null;

//    private String   deviceName = "";
    public  int      currentreviewShotID;        //the shot index that is being shown in review mode, -1 is show all
    public  TextView statusBar;
    public  TextView splitTimeBar;

    private static final String TAG = "===TimerCameraView";

    //for review
    private float    tc_x1, tc_x2;                               //touch coordinates
    private float    tc_y1, tc_y2;                               //touch coordinates

    public SharedPreferences mpreferences;
    public String            model;


    static {
        System.loadLibrary("glt21xxlib");
    }

    public TimerCameraView(Context context, Camera camera) {
        super(context);

        Log.d(TAG, "Constructor entered...");

        mCamera = camera;
        mCamera.setDisplayOrientation(90);

        CamPreviewSizes  = mCamera.getParameters().getSupportedPreviewSizes();

        //get the holder and set this class as the callback, so we can get camera data here
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        Log.d(TAG, "surfaceCreated() entered...");

        if (mCamera == null) {
            Log.d(TAG, "surfaceCreated() mCamera is null!");
            return;
        }

        try {
            //this might give some problems when calibrating, be careful
            cameraParameters = mCamera.getParameters();

            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

            cameraParameters = mCamera.getParameters();
            mActivity        = (TimerActivity) getContext();

//            Log.d(TAG, "Camera Parameters: "+cameraParameters);

            if (!cameraParameters.isAutoExposureLockSupported() || NoPVSizeControl) {
                AlertDialog mAlertDialog = new AlertDialog.Builder(mActivity.getApplicationContext()).create();

                if (mAlertDialog != null) {
                    mAlertDialog.setTitle("Performance Warning"); // ("Session Alert");
                    String msg = "Device misses Exposure Control.";
                    if(NoPVSizeControl) {
                        msg = "Your device doesn't support required Camera Control!";
                    }
                    mAlertDialog.setMessage(msg);
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
            Log.e(TAG, "Exception occurred in surfaceCreated(): " + e.getMessage());
        }

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
            Log.d(TAG, "  ------------ adjustExposure() Can't Set ISO; not supported.");
        }

        mpreferences = mActivity.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);

        if(mpreferences != null) {
            model          = mpreferences.getString("MyDeviceModel", "");

            SessionsLimit  = mpreferences.getInt("TimerSessionsLimit", SessionsLimit);
            ShotsLimit     = mpreferences.getInt("TimerShotsLimit", ShotsLimit);
            SessionDelay   = mpreferences.getInt("TimerSessionDelay", SessionDelay);
            SessionLength  = mpreferences.getInt("TimerSessionLength", SessionLength);
            CountDown      = mpreferences.getInt("TimerCountDown", CountDown);
            GetReadyMuteOn = mpreferences.getBoolean("TimerReadyMuteOn", GetReadyMuteOn);
            RandomModeOn   = mpreferences.getBoolean("TimerRandomModeOn", RandomModeOn);

            Log.d(TAG, "/// surfaceCreated() settings: #sess, shots, time, leng, ctdwn, GetReadyMuteOn, RandomModeOn = " +
                    SessionsLimit + ", " + ShotsLimit + ", " + SessionDelay + ", " + SessionLength + ", " + CountDown +
                    ", " + GetReadyMuteOn + ", " + RandomModeOn);

            Log.d(TAG, "/// surfaceCreated() settings: MyDeviceModel = "+ model);
        } else {
            Log.d(TAG, "!!! surfaceCreated() retrieving settings failed  !!!");
        }

    }


    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        //before changing the application orientation, you need to stop the preview, rotate and then start it again
        if (mHolder.getSurface() == null) return;

        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            //this will happen when you are trying the camera if it's not running
        }

        NoPVSizeControl = false;
        IPAutoFocusBusy = false;

        //now, recreate the camera preview
        try {
            cameraParameters = mCamera.getParameters();
            CamPreviewSizes  = cameraParameters.getSupportedPreviewSizes();

            if(CamPreviewSizes == null) {
                // will not work; post message and quit!
                Log.d(TAG, "surfaceChanged() - Camera PreviewSize can't be controlled on device!");
                NoPVSizeControl = true;
            } else {
                mCameraSize = getOptimalPreviewSize(CamPreviewSizes, 640, 480);

                if (mCameraSize.width != 640 || mCameraSize.height != 480) {
                    NoPVSizeControl = true;
                } else {
                    cameraParameters.setPreviewSize(previewHeight, previewWidth); // (640,480);
                    mCamera.setParameters(cameraParameters);
                }
            }

            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "surfaceChanged cameraParameters exceptions: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // our app has only one screen, so we'll destroy the camera in the surface
        // if you are using with more screens, please move this code your activity

        Log.d(TAG, "  ----surfaceDestroyed() entered...");

        Utils.clearTextLineCache();
        if (mCamera == null) {
            Log.d(TAG, "  ----surfaceDestroyed() - mCamera is null!");
            return;
        }

        mCamera.stopPreview();
        mHolder.removeCallback(this);
        mCamera.release();
        mCamera = null;

        IPTemplate = null;
        IPDCZx     = null;
        IPDCZy     = null;
        IPDCZv     = null;
        IPHState   = null;
        IPHPopul   = null;
    }


    // @Override
    public void Start() {

        cameraParameters = mCamera.getParameters();

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

        int expIndx = cameraParameters.getExposureCompensation();
        int eVal;
        IPDone       = false;
        IPinStopping = false;
        IPActive     = false;
        IPDCZcount   = 0;
        isFirstCal   = true;

        BitmapFactory.Options bitmap_option = new BitmapFactory.Options();
        bitmap_option.inPreferredConfig = Bitmap.Config.ARGB_8888;


        SharedPreferences   preferences = mActivity.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);

        SessionsLimit   = preferences.getInt("TimerSessionsLimit", SessionsLimit);
        ShotsLimit      = preferences.getInt("TimerShotsLimit", ShotsLimit);
        SessionDelay    = preferences.getInt("TimerSessionDelay", SessionDelay);
        SessionLength   = preferences.getInt("TimerSessionLength", SessionLength);
        CountDown       = preferences.getInt("TimerCountDown", CountDown);
        GetReadyMuteOn  = preferences.getBoolean("TimerReadyMuteOn", GetReadyMuteOn);

        Log.d(TAG, "/// Start() settings: #sess, shots, time, leng, ctdwn, GetReadyMuteOn = " + SessionsLimit + ", " + ShotsLimit + ", " +
                SessionDelay + ", " + SessionLength + ", " + CountDown + ", " + GetReadyMuteOn);

        eVal = expIndx + 1;
//	Log.d(TAG, " ------Start() setting initial Exposure Compensation to: "+eVal);

        cameraParameters.getPreviewFormat();
        cameraParameters.setExposureCompensation(eVal);
        cameraParameters.setAutoExposureLock(true);

        if(model.contains("SM-G950")            // S8
                || model.contains("SM-G935")    // S7 Edge
                || model.contains("SM-G930")    // S7
                || model.contains("SM-G900")    // S6
                || model.contains("SM-N920")
                || model.contains("SM-G870")) { // Special Case!
            cameraParameters.setAutoExposureLock(false);
        }

        cameraParameters.setAutoWhiteBalanceLock(true);
        cameraParameters.setPreviewFpsRange(30000, 30000);

        IPwidth  = cameraParameters.getPreviewSize().width;
        IPheight = cameraParameters.getPreviewSize().height;

        ROI[0] = 10; // 0;
        ROI[1] = IPheight - 10; // IPheight;
        ROI[2] = 10; // 0;
        ROI[3] = IPwidth - 10; // IPwidth

        for (int s = 0; s < G_MAXSESSIONS; s++) {
            IPSessionFmRate[s]     = -1;
            IPSessionStartTime[s]  = 0L;
            IPSessionEndTime[s]    = 0L;
            IPSessionEndFrame[s]   = -1;
            IPSessionStartFrame[s] = -1;
            IPSessionHits[s]       = -1;
        }

        HitSMaster = new ArrayList<>();

        for (int s = 0; s < SessionsLimit; s++) {
            HitSMaster.add(0);
            for (int t = 0; t < ShotsLimit; t++) {
                final int dt = t - ShotsLimit;
                HitSMaster.add(dt);
            }
        }

        Log.d(TAG, " ------Start() setting initial HitsMaster to: " + HitSMaster);

        mCamera.setParameters(cameraParameters);

        statusBar.setText(R.string.Calibrating);

        int PVArySize = 3110400;
        byte[] mCallbackBuffer = new byte[PVArySize];
        mCamera.addCallbackBuffer(mCallbackBuffer);

        // States and camera init
        IPState            = S_WAIT;
        timesCalibrated    = 0;
        needDBGimage       = true;
        IPTimerDetectReady = false;
        ReadyTimerInAction = false;
        IPinCalib          = false;
        currentSession     = 1;
        mIPFrame           = 0;


        if(mCamera == null) {
            Log.d(TAG, " ------Start() aborted due to NULL mCamera!!!");
            return;
        }

        Log.d(TAG, " ------Start() calling initial AutoFocus...");

        mCamera.autoFocus(new AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                IPState = S_CALIB;

                /*
                Log.d(TAG, "   ------Start() initial AutoFocus done...");

                Log.d(TAG, "   ------Start() states: needDBGimage, IPinCalib, IPTimerDetectReady, ReadyTimerInAction = "+
                        needDBGimage +", "+ IPinCalib +", "+ IPTimerDetectReady +", "+ ReadyTimerInAction);
                */
            }
        });

        final int          rx1 = ROI[2], rx2 = ROI[3];
        final int          ry1 = ROI[0], ry2 = ROI[1];
        final int        ryDim = ry2 - ry1 + 1;
        final int[]     rxHead = new int[ryDim];

        Log.d(TAG, "      ------Start() ryDim = "+ryDim+"; IPwidth, IPheight = "+IPwidth +", "+ IPheight);

        for (int i = 0; i < ryDim; i++) { rxHead[i] = (ry1+i) * IPwidth + rx1; }


        mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() {

            final TextView   mstatusBar = statusBar;
            final int   G_IPStartFrameP = G_IPStartFrame + 1;
            final short       zeroShort = (short)0;

            @Override
            public void onPreviewFrame(final byte[] data, Camera camera) {

                final int    tIPFrame = mIPFrame;
                final int    mIPState = IPState;
                final int sessionIndx = currentSession - 1;
                final long mtimestamp = System.currentTimeMillis(); // timestamp;

                mIPFrame++;

                new Thread() {
                    public void run() {
                        final YuvImage                    yuv = new YuvImage(data, cameraParameters.getPreviewFormat(), IPwidth, IPheight, null);
                        final ByteArrayOutputStream       out = new ByteArrayOutputStream();
                        final int                    mIPwidth = IPwidth;
                        final int                   mIPheight = IPheight;
                        final int                     mIPsize = mIPwidth * mIPheight;
                        final int             G_IPStartFrame2 = G_IPStartFrame + 2;
                        final int                 mSessionLen = SessionLength * 1000;
                        final Rect                      mRect = new Rect(0, 0, mIPwidth, mIPheight);

                        yuv.compressToJpeg(mRect, G_JPGQuality, out); // Output Jpeg quality 70

                        final byte[]                    bytes = out.toByteArray();
                        final int[]                      pixs = new int[IPwidth * IPheight];
                        BitmapFactory.Options  bitmap_options = new BitmapFactory.Options();
                        bitmap_options.inPreferredConfig      = Bitmap.Config.ARGB_8888;

                        final Bitmap                   bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bitmap_options);
						
                        bitmap.getPixels(pixs, 0, IPwidth, 0, 0, mIPwidth, mIPheight);

                        // grabs the BMP and gets the histogram
                        int[] hstR = new int[256];
                        int   r, g, b, p;
                        int   indx, mindx;
                        int   bw;

                        if (needDBGimage && tIPFrame == 20) { // just once; pre-condition for stabilization
                            needDBGimage = false;
                        }

//                        Log.d(TAG, "      Fm | IPState = " + tIPFrame + " | " + mIPState);

                        if (mIPState == S_CALIB && !IPinCalib && !needDBGimage) { // in calibration
//      Log.d(TAG, "TimerCamView start() (mIPState == CALIB) @ Fm " + tIPFrame);

                            for (int my = ry1, mi = 0; my < ry2; my++, mi++) {
                                mindx = rxHead[mi];  // mindx = my * IPwidth + rx1;

                                for (int x = rx1; x < rx2; x++, mindx++) {
                                    p  = pixs[mindx];
                                    r  = (p >> 16) & 0x4c;
                                    g  = (p >> 8) & 0x96;
                                    b  = p & 0x1d;
                                    bw = r + g + b;
                                    hstR[bw]++;
                                }
                            }

                            if (isFirstCal) {
//                                Log.d(TAG, "start() - bgImage captured!");

                                isFirstCal = false;
                                bgImage    = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                CalibFrame = tIPFrame;

                                calibrate(hstR, tIPFrame, -2);
                            } else {
                                if (tIPFrame - CalibFrame == 1) { // skip contiguous frames!
                                    CalibFrame = tIPFrame;
                                } else if (tIPFrame - CalibFrame == -1) { // skip contiguous frames!
                                    // do nothing!
                                } else {
                                    calibrate(hstR, tIPFrame, CalibFrame);
                                }
                            }

                        } else if (mIPState == S_COUNTDOWN) {
//                            Log.d(TAG, "start() - CountDown Frame...");
                        } else if (mIPState == S_RANDOM) {
//                            Log.d(TAG, "start() - Random Frame...");
                            sHits = 0; // session reset
                        } else if (mIPState == S_POSTPROC) {
//                            Log.d(TAG, "start() - PostProc Frame...");
                        } else if (mIPState == S_DETECT && !IPinStopping && !IPDone) {

                            //this one is for shooting mode
//                           Log.d(TAG, "++++++++++++ DetectMode Fm | G_IPFrameCountMax | Sesn Indx = " + tIPFrame + " | " + G_IPFrameCountMax + " | " + sessionIndx);
                            final long      nwTime = mtimestamp;
                            final long      stTime = IPSessionStartTime[sessionIndx];
                            final int      stFrame = IPSessionStartFrame[sessionIndx];
                            final int   FmProgress = tIPFrame - stFrame;   //  session frame elapse
                            final long  TmProgress = mtimestamp - stTime;    //  session time elapse

                            //if we run out of frames, stop it here
                            if (tIPFrame >= G_IPFrameCountMax || TmProgress > mSessionLen) { //stop here

                                final int oldState = IPState;

                                IPState = S_POSTPROC;
                                mActivity.ActivityStateChange(oldState, S_POSTPROC);

                                mActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        IPSessionEndTime[sessionIndx]  = nwTime;
                                        IPSessionEndFrame[sessionIndx] = tIPFrame;

                                        stopSessionShooting(tIPFrame, nwTime, sessionIndx);
                                    }
                                });
                            }

                            if (tIPFrame == IPSessionStartFrame[0]+1) {
                                // do the prep clean-up
//                                Log.d(TAG, "   Start() cleaning up IPTemplate & DCZMap ..... <"+tIPFrame+">");

                                for(int j=0; j<mIPsize; ++j) {
//                                    DCZMap[j]     = false;
                                    IPTemplate[j] = 0;
                                }
                            }

                            //first set up the DCZ
                            final int  TemplateInitFrame = G_IPStartFrame - G_IPDCZFramesN; // 18 - 10

                            if (sessionIndx == 0 && FmProgress >= TemplateInitFrame && FmProgress < G_IPStartFrame) {

//                                Log.d(TAG, "Start( "+FmProgress+" ) -- accumulating IPTemplate..... <"+tIPFrame+">");

                                for (int my = ry1, mi = 0; my < ry2; my++, mi++) {
                                    mindx = rxHead[mi];  // mindx = my * IPwidth + rx1;

                                    for (int x = rx1; x < rx2; x++, mindx++) {
                                        p  = pixs[mindx];
                                        r  = (p >> 16) & 0x4c;
                                        g  = (p >> 8) & 0x96;
                                        b  = p & 0x1d;
                                        bw = r + g + b;
                                        hstR[bw]++;
                                        IPTemplate[mindx] += bw;  // 1st impression
                                        if (bw >= 220 && IPDCZcount < G_IPDCZcountMax) {
                                            // record position in IPDCZx & IPDCZy
                                            IPDCZx[IPDCZcount]   = x;
                                            IPDCZy[IPDCZcount]   = my;
                                            IPDCZv[IPDCZcount]   = bw;
                                            IPDCZcount++;
                                        }
                                    }
                                }

                                if (IPDCZcount >= G_IPDCZcountMax) {
                                    //case too bright
                                    //    Log.d(TAG, "   ---------- LightLevel Determination: Lighting too bright!");
                                }

                                int    i = 255;
                                int tPop;
                                threadHB = 255;

                                while (i > 0) {
                                    tPop = hstR[i];
                                    if (tPop > 3) {
                                        threadHB = i;
                                        break;
                                    }
                                    i--;
                                }

                                if (threadHB < 250) {
                                    if (threadHB > head)         head = threadHB;
                                    if (threadHB > IPThresh) IPThresh = threadHB;
                                }
//                                Log.d(TAG, " tIPFrame = "+tIPFrame+": threadhb | IPThresh = " + threadHB+" | "+IPThresh);

                            }
                            else if (FmProgress >= G_IPStartFrame) {

//                                Log.d(TAG, "   Start( "+FmProgress+" ) --- process post-G_IPStartFrame..... <"+tIPFrame+">");

                                short[]       xpo = new short[G_IPHCSizeMax];
                                short[]       ypo = new short[G_IPHCSizeMax];
                                short[]       val = new short[G_IPHCSizeMax];
                                int        posLen = 0; // real data length
                                int        head10 = head + 10;
                                boolean endBySize = false;

                                if( sessionIndx == 0 && FmProgress == G_IPStartFrame && !IPTimerDetectReady) {

//                                    Log.d(TAG, "   Start( "+FmProgress+" ) --- setting detect param..... <"+tIPFrame+">");

                                    for (int my = ry1, mi = 0; my < ry2; my++, mi++) {
                                        mindx = rxHead[mi];  // mindx = my * IPwidth + rx1;

                                        for (int x = rx1; x < rx2; x++, mindx++) {
                                            p  = pixs[mindx];
                                            r  = (p >> 16) & 0x4c;
                                            g  = (p >> 8) & 0x96;
                                            b  = p & 0x1d;
                                            bw = r + g + b;
                                            hstR[bw]++;
                                        }
                                    }

                                    int    i = 255;
                                    int tPop;
                                    threadHB = 255;

                                    while (i > 0) {
                                        tPop = hstR[i];
                                        if (tPop > 3) {
                                            threadHB = i;
                                            break;
                                        }
                                        i--;
                                    }

                                    if (threadHB < 250) {
                                        if (threadHB > head)         head = threadHB;
                                        if (threadHB > IPThresh) IPThresh = threadHB;
                                    }

                                    int cc1 = GetThreshold(IPThresh, IPThreshLow, IPBKLow);
                                    int cc2 = GetThresholdLow(IPThresh, IPThreshLow, IPBKLow);
                                    int cc3 = GetIPBK(IPThresh, IPThreshLow, IPBKLow);

                                    IPThresh    = cc1;
                                    IPThreshLow = cc2;
                                    IPBKLow     = cc3;

//                                    Log.d(TAG, "  Start() tIPFrame = "+tIPFrame+": threadhb | IPThresh = " + threadHB+" | "+IPThresh);

                                    for (int j = 0; j < mIPsize; j++) { IPTemplate[j] /= G_IPDCZFramesN; }

                                    IPTimerDetectReady = true;

                                    /*
                                    Log.d(TAG, "  Start()  tIPFrame = "+tIPFrame+" >> IPTimerDetectReady! IPThresh | IPThreshLow | IPBKLow = "+
                                    " | "+IPThresh + " | "+IPThreshLow + " | "+IPBKLow);
                                    */
                                }

                                else if( IPTimerDetectReady)
                                {

                                    for (int my = ry1, mi = 0; my < ry2; my++, mi++) {
                                        indx = rxHead[mi];  // indx = my * IPwidth + rx1;
                                        for (int x = rx1; x < rx2; x++, indx++) {
                                            p  = pixs[indx];
                                            r  = (p >> 16) & 0x4c;
                                            g  = (p >> 8) & 0x96;
                                            b  = p & 0x1d;
                                            bw = r + g + b;

                                            if (bw >= IPThresh) {
                                                // Log.d("caught", "bw | head = " + bw +" | "+head);
                                                hstR[bw]++;
                                                xpo[posLen]   = (short) x;
                                                ypo[posLen]   = (short) my;
                                                val[posLen++] = (short) bw;
                                                if (posLen >= G_IPHCSizeMax) {
                                                    my        = IPheight;
                                                    endBySize = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (endBySize) break;
                                    }

                                    boolean hit;

                                    if (tIPFrame == G_IPStartFrameP) { // Threshold determination, No Detection!
                                        /// record IPStarTime
                                        IPStarTime = System.currentTimeMillis();

//                                        Log.d(TAG, "\n\n  Start() tIPFrame = "+tIPFrame+":  set StatusBar to Begin Shooting..\n\n");

                                        mActivity.runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {
                                                mstatusBar.setText(R.string.BeginShooting);
                                                mActivity.changeStatusBar("Begin shooting");
                                            }
                                        });

                                        cameraParameters = mCamera.getParameters();

                                        cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);

                                        // cleanup DCZ
                                        if (IPDCZcount > 1) {
                                            int fIPDCZcount = IPDCZcount;
                                            for (int ii = IPDCZcount - 1; ii > 0; ii--) {

                                                int xa = IPDCZx[ii];
                                                int ya = IPDCZy[ii];
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
                                        }

                                    }

                                    //// detection frames start here
                                    else if (FmProgress > G_IPStartFrameP && !IPinStopping && IPTimerDetectReady) {

//                                        Log.d(TAG, "   Start( " + tIPFrame + ")  -- do detect .. posLen = " + posLen);

                                        final int tIPFramem = tIPFrame - 1;
                                        final int IPDCZcount4 = IPDCZcount + 4;

                                        if (posLen >= 4 && posLen < IPDCZcount4) { // Post Detection Processing -

                                            IPHPopul[tIPFrame] = posLen;
                                            int fposLen = posLen;
                                            final int mDist = 3;
                                            final int posLenm = posLen - 1;
                                            boolean IsSmall = false;

                                            for (int j = posLenm; j >= 0; j--) {
                                                int xa = xpo[j];
                                                int ya = ypo[j];
                                                for (int k = 0; k < IPDCZcount; k++) {
                                                    if ((Math.abs(xa - IPDCZx[k]) + Math.abs(ya - IPDCZy[k])) <= mDist) {
                                                        // ignore this one
                                                        for (int ii = j; ii < posLenm; ii++) {
                                                            xpo[ii] = xpo[ii + 1];
                                                            ypo[ii] = ypo[ii + 1];
                                                            val[ii] = val[ii + 1];
                                                        }
                                                        fposLen--;
                                                        if (fposLen < 4) IsSmall = true;

                                                        if (fposLen > 1 && !IsSmall) {
                                                            xpo[fposLen - 1] = (short) -1;
                                                            ypo[fposLen - 1] = (short) -1;
                                                            val[fposLen - 1] = (short) -1;
                                                        }

                                                        break;
                                                    }
                                                }
                                                if (IsSmall) break;
                                            }

                                            posLen = fposLen;
                                        }

                                        if (posLen >= 4 && !IPinStopping && !IPDone) {
                                            boolean isHit = true;
                                            IPHState[tIPFrame] = (IPHState[tIPFramem] >= 0) ? IPHState[tIPFramem] + 1 : 0;
                                            // Valid Candidate Seq Index
                                            IPHPopul[tIPFrame] = posLen;

                                            if (isHit) {    //// Keep the record
                                                if (posLen > G_IPHRecordMax) {
                                                    posLen = G_IPHRecordMax; // Force it to reduce!
                                                    IPHPopul[tIPFrame] = posLen;
                                                }

                                                //add to list of hits
                                                laserHit newhit = new laserHit();

                                                newhit.setup(TmProgress); // newhit.setup();
                                                newhit.posLen = posLen;
                                                newhit.state = 1;
                                                newhit.FmNum = tIPFrame;
                                                newhit.session = sessionIndx; // default is -1
                                                newhit.hitID = sHits + 1;

                                                int j = 0;

                                                while (j < posLen) {
                                                    //add only if > 0
                                                    if ((xpo[j] > 0) && (ypo[j] > 0)) {
                                                        newhit.add(xpo[j], ypo[j], zeroShort, val[j]);
                                                    }
                                                    j++;
                                                }

                                                HitList.add(newhit);

                                                if (IPHFIndx < G_IPHitFramesMax - 1) IPHFIndx++;
                                            }
                                        } else if (!IPinStopping && !IPDone) {
                                            if (IPHState[tIPFramem] == 0) {
                                                if (IPHPopul[tIPFramem] >= 5) { // this is a single frame case with big size, take it!
                                                    hit = true;
                                                    shots++;
                                                    sHits++;
                                                    IPSessionHits[sessionIndx]++;

                                                    HitStarts.add(tIPFramem);

                                                    final int masterSeIndx = sessionIndx * (ShotsLimit + 1);
                                                    final int masterIndx = masterSeIndx + sHits;
                                                    HitSMaster.set(masterIndx, tIPFramem);
                                                    HitSMaster.set(masterSeIndx, HitSMaster.get(masterSeIndx) + 1);

//                                                Log.d(TAG, "== Hit Frame Detected 2: masterIndx: " + masterIndx + " | masterSeIndx: " + masterSeIndx + " | sHits: " + sHits);

                                                    showShotforStartFrame(tIPFramem);

                                                    mActivity.sdpool.play(mActivity.ssSoundID, 0.99f, 0.99f, 1, 0, 0.99f);

//                                                Log.d(TAG, "== Hit Frame Detected: Single: " + tIPFramem + " | Shot# " + shots);
                                                } else {
                                                    hit = false;
                                                }
                                                IPNewSeq = false; // reset Hit Sequence Flag
                                                IPHSeqID = -1;
                                            } else if (IPHState[tIPFramem] > 0) { // this is the end of a sequence, post proc is needed!
                                                hit = false;
                                                int seqSize = IPHState[tIPFramem] + 1;
                                                int j = tIPFrame - seqSize;

                                                for (; j < tIPFrame; j++) {
                                                    if (IPHPopul[j] >= 4) { // this frame in the sequence has a large hot pixel population, take it!
                                                        hit = true;
                                                        HitStarts.add(j);
                                                        shots++;
                                                        sHits++;

                                                        mActivity.sdpool.play(mActivity.ssSoundID, 0.99f, 0.99f, 1, 0, 0.99f);

                                                        final int masterSeIndx = sessionIndx * (ShotsLimit + 1);
                                                        final int masterIndx = masterSeIndx + sHits;
                                                        HitSMaster.set(masterIndx, j);
                                                        HitSMaster.set(masterSeIndx, HitSMaster.get(masterSeIndx) + 1);

//                                                    Log.d(TAG, "== Hit Frame Detected 3: masterIndx: " + masterIndx + " | masterSeIndx: " + masterSeIndx + " | sHits: " + sHits);

                                                        showShotforStartFrame(j);

                                                        IPSessionHits[sessionIndx]++;

//                                                    Log.d("== Hit Frames Found: ", "multi: " + j + " seqLen : " + seqSize + " | Shot# " + shots);
                                                        break;
                                                    }
                                                }

                                                if (!hit) {
                                                    // weird case - need special attention!
//                                                Log.d("== Hit Frames missing: ", "multi: " + j + " seqLen : " + seqSize + " | Shot# " + shots);

                                                    mActivity.sdpool.play(mActivity.ssSoundID, 0.99f, 0.99f, 1, 0, 0.99f);

                                                    j = tIPFrame - seqSize;
                                                    shots++;
                                                    sHits++;
                                                    IPSessionHits[sessionIndx]++;
                                                    HitStarts.add(j);

                                                    final int masterSeIndx = sessionIndx * (ShotsLimit + 1);
                                                    final int masterIndx = masterSeIndx + sHits;
                                                    HitSMaster.set(masterIndx, j);
                                                    HitSMaster.set(masterSeIndx, HitSMaster.get(masterSeIndx) + 1);

//                                                Log.d(TAG, "== Hit Frame Detected 4: masterIndx: " + masterIndx + " | masterSeIndx: " + masterSeIndx + " | sHits: " + sHits);

                                                    showShotforStartFrame(j);
                                                }
                                                IPNewSeq = false; // reset Hit Sequence Flag
                                                IPHSeqID = -1;
                                            }
                                        }
                                    }
                                }
                            }


                            // Stop session if
                            //  (a) max number of shots per session is reached or
                            //  (b) session time is ended.

                            if ((sHits == ShotsLimit || TmProgress > mSessionLen || IPinStopping) && !IPDone) {
                                IPSessionEndTime [sessionIndx] = mtimestamp;
                                IPSessionEndFrame[sessionIndx] = tIPFrame;

/*
            Log.d(TAG, "  >> start: shot-/time- limit reached @ time | start time | Session Idx = "+IPSessionEndTime[sessionIndx]+" | "
                                           + IPSessionStartTime[sessionIndx] +" | "+sessionIndx);
                                Log.d(TAG, "    >>> start: ipstate = " + mIPState + " to be changed to "+S_POSTPROC);
*/
                                mActivity.sdpool.play(mActivity.bzSoundID, 0.99f, 0.99f, 1, 0, 0.99f); // play sound for end of session

                                if (((sessionIndx >= SessionsLimit - 1) || (currentSession > SessionsLimit) || IPinStopping) && !IPDone) { //====//
                                    IPState = S_POSTPROC;
                                    IPDone  = true;
                                    currentSession = SessionsLimit + 1;

                                    final long   SessionTime = IPSessionEndTime[sessionIndx] - IPSessionStartTime[sessionIndx];
                                    final long SessionFrames = IPSessionEndFrame[sessionIndx] - IPSessionStartFrame[sessionIndx];

                                    IPSessionFmRate[sessionIndx] = (SessionTime < 1000) ? 0 : (int) (SessionFrames * 1000 / SessionTime);

//                                    Log.d(TAG, " start( ) ended naturally - last session Time | Frms | FmRate = " + SessionTime + " | " + SessionFrames + " | " + IPSessionFmRate[sessionIndx]);

//                                    HitSMaster.add(0, (int)(IPSessionStartTime[0]/1000));
                                    stopShootingAll();
                                } else if (IPState == S_DETECT && !IPinStopping && !IPinSessionStoppng) { //====//

//                                    Log.d(TAG, " start( ) ending current session ID = " + sessionIndx);

                                    IPState = S_WAIT; //====// IPState = S_PAUSE;
                                    final long   SessionTime = IPSessionEndTime[sessionIndx] - IPSessionStartTime[sessionIndx];
                                    final long SessionFrames = IPSessionEndFrame[sessionIndx] - IPSessionStartFrame[sessionIndx];

                                    IPSessionFmRate[sessionIndx] = (SessionTime < 1000) ? 0 : (int) (SessionFrames * 1000 / SessionTime);
                                    stopSession();
                                }
                            }
                        }

                        bitmap.recycle();
                    }
                }.start(); // Thread()

                mCamera.addCallbackBuffer(data);

            } // onPreviewFrame()
        });

    } // start()


    //---------------------------------------------------- State Change functions --------------------------------------------------------

    private void startIP() {
        Log.d(TAG, "==== startIP() entered.... timesCalibrated | IPState | IPActive = " + timesCalibrated + " | " + IPState + " | " + IPActive);

        int oldState = IPState;

        if (oldState == S_POSTPROC || IPActive) {
            Log.d(TAG, "  ==== startIP() abort due to redundancy!");
            return;
        }

        IPActive = true;
        IPState  = S_POSTPROC;
        mActivity.ActivityStateChange(oldState, S_POSTPROC);

        Matrix matrix = new Matrix();
        matrix.preRotate(90);

        final Bitmap finalbgImage = Bitmap.createBitmap(bgImage, 0, 0, bgImage.getWidth(), bgImage.getHeight(), matrix, true);

        mActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mActivity.drawImage(finalbgImage);
            }
        });

        HitList    = new ArrayList<>();
        HitStarts  = new ArrayList<>();
        mIPFrame   = 0;
        IPThresh   = 0;
        CalibFrame = -1;

        for (int s = 0; s < G_IPFrameCountMax; s++) {
            for (int k = 0; k < G_IPDCZcountMax; k++) {
                IPDCZx[k] = -1;
                IPDCZy[k] = -1;
            }
            IPHState[s] = -1;
            IPHPopul[s] = 0;
        }

        //chop off 10 pixels off the edges to minimize edge noise
        ROI[0] = 10;
        ROI[1] = IPheight - 10;
        ROI[2] = 10;
        ROI[3] = IPwidth - 10;

        threadHB   = 175;
        IPHFIndx   = 0;
        IPNewSeq   = false; // reset Hit Sequence Flag
        IPHSeqID   = -1;
        IPDCZcount = 0;
        shots      = 0;

        Log.d("   ==== startIP()", "calling shotTimerController() -- oState | newState = " + oldState + " | " + IPState);

        currentSession = 1;
        sessionIndx    = 0;

        STimerNewSessionControl();
    }


    public void stopSessionShooting(int endFrame, long EndTime, int sessionID) {
        Log.d(TAG, "stopSessionShooting() entered...sessionID | currentSession = " + sessionID + " | " + currentSession);

        final int oldState = IPState;
        int     SessionCnt = 0;

        long   SessionTime = EndTime  - IPSessionStartTime[sessionID];
        long SessionFrames = endFrame - IPSessionStartFrame[sessionID];

        IPSessionFmRate[sessionID] = (SessionTime < 2000) ? 0 : (int) (SessionFrames / (SessionTime / 1000L));

        Log.d(TAG, "  stopSessionShooting(): Fm Rate | oState -> newState = " + IPSessionFmRate[sessionID] + " | " + oldState + " -> " + IPState);

        if (currentSession > SessionsLimit) {
            mCamera.stopPreview();

            IPFmRate = IPSessionFmRate[0];
            if (IPFmRate > 0) {
                SessionCnt = 1;

                for (int i = 1; i < sessionID; i++) {
                    if (IPSessionFmRate[i] > 0) {
                        IPFmRate += IPSessionFmRate[i];
                        SessionCnt++;
                    }
                }
            }

            if (SessionCnt > 0) {
                double fRate = (IPFmRate + 0.00001f) / SessionCnt;
                IPFmRate = (int) (fRate + 0.5);
            } else {
                IPFmRate = 0;
            }

            Log.d(TAG, "  stopSessionShooting(): Avg Rm Rate = " + IPFmRate);

            currentreviewShotID = 0; // currentreviewShotID = -1;    //always start on show all
            IPState = S_REVIEW;
            mActivity.ActivityStateChange(oldState, S_REVIEW);

            int info[] = showShotsByMasterList(0, HitSMaster); // showAllShots();
            currentreviewShotID = 0;
        } else {
            // continue on with the next session
            IPState = S_POSTPROC;
            mActivity.ActivityStateChange(oldState, S_POSTPROC);

            currentSession++;
            sessionIndx++;
            STimerNewSessionControl();
        }
    }


    public void setSessionNum(int sessionNum) {
        Log.d(TAG, "setSessionNum() entered... Target sessionNum | current sessionIndx = " + sessionNum + " | " + sessionIndx);

        currentSession = sessionNum + 1;
        sessionIndx    = sessionNum;

    }


    public void stopShootingAll()
    // Stop all shooting activity and enter the Review Mode immediately.
    //
    // This function is the Kill Switch for the TimerActivity. For regular single-session stop handler use the
    // stopSession() function.
    {
        Log.d(TAG, "stopShootingAll() entered...");

        if (IPinStopping) return;

        int      oldState = IPState;
        int     sessCount = 0;
        IPState           = S_POSTPROC;

        IPinStopping = true;  // Kill Switch ON
        HitSMaster.add(0, (int) (IPSessionStartTime[0] / 60000)); // in Minute resolution

        Log.d(TAG, " --stopShootingAll() ...completing HitSMaster -> "+HitSMaster);

        mActivity.ActivityStateChange(oldState, S_POSTPROC);

        Log.d(TAG, " --stopShootingAll() ...currentSession Index | oState | newState = " + (currentSession - 1) + " | " + oldState + " | " + S_POSTPROC);

        mCamera.stopPreview();

        if (IPSessionFmRate[0] > 0) {
            IPFmRate  = IPSessionFmRate[0];
            sessCount = 1;

            for (int i = 1; i < SessionsLimit; i++) {
                if (IPSessionFmRate[i] > 0) {
                    IPFmRate += IPSessionFmRate[i];
                    sessCount++;
                }
            }

            double fRate = (IPFmRate + 0.00001f) / sessCount;
            IPFmRate     = (int) (fRate + 0.5);
        } else {
            IPFmRate = 0;
        }
        Log.d(TAG, "  stopShootingAll(): Avg Fm Rate = " + IPFmRate);

        currentreviewShotID = 0;  //  currentreviewShotID = -1;     //always start on show all

        if (currentSession >= SessionsLimit) {

            Log.d(TAG, "calling showAllShots()...SessionsLimit = " + SessionsLimit);

            int info[] = showShotsByMasterList(0, HitSMaster); // showAllShots();
            currentreviewShotID = 0;
        } else { // premature stopping
            Log.d(TAG, "stopShootingAll() bad processing state!");


        }
    }

    public void reset() {
        int oldState = IPState;
        IPState = S_WAIT;
        mActivity.ActivityStateChange(oldState, S_WAIT);

        Log.d(TAG, "reset() entered...currentSession Index | oState | newState = " + (currentSession - 1) + " | " + oldState + " | " + IPState);

        mActivity.drawImage(null);

        //reset arrays
        HitList.clear();
        HitStarts.clear();
        HitSMaster.clear();

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

        mCamera.setParameters(cameraParameters);

        statusBar.setText(R.string.zoomMessage);
    }

    //---------------------------------------------------- calibration functions --------------------------------------------------------

    private void calibrate(int[] hist, int Indx, int lastIndx) {
        if (timesCalibrated > 4 || IPActive || IPinCalib) { // skipping contiguous frames
            return;
        }

        IPinCalib  = true;
//        CalibFrame = Indx;
        Log.d(TAG, " calibrate(): Indx | lastIndx = " + Indx + " | " + lastIndx);

        int i = 255, head = 0;
        int tPop;

        while (i > 0) {
            tPop = hist[i];
            if (tPop > 4) {
                head = i;
                break;
            }
            i--;
        }

        double  adjust;

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
        } else { // done
            adjust = 0;
        }

        Log.d(TAG, "  ===calibrate(): Fm | head | value | round # | IPState = " + Indx + " | " + head + " | " + adjust
                + " | " + timesCalibrated + " | " + IPState);

        adjustExposure(adjust);

    }

    private void adjustExposure(double adjustValue) {
        //adjustValue is EV

        if(IPAutoFocusBusy) { // if(IPAutoFocusBusy || IPState == S_CALIB) {
            Log.d(TAG, "=== adjustExposure() aborted due to same active State ===");
            return;
        }

        IPState          = S_CALIB;
        cameraParameters = mCamera.getParameters();

        int     expMin = cameraParameters.getMinExposureCompensation();
        int     expMax = cameraParameters.getMaxExposureCompensation();
        int    expIndx = cameraParameters.getExposureCompensation();
        float  expStep = cameraParameters.getExposureCompensationStep();
        double      EV = expIndx * expStep;
        double     wEV = EV + adjustValue;
        double  wIndex = wEV / expStep;

//        Log.d(TAG, "=== adjustExposure - wanted exposure | current | step = " + (int)wIndex+" | "+expIndx+" | "+expStep);
        // make sure it's supported!
        if (expMin == 0 && expMax == 0) {
            Log.d(TAG, "=== adjustExposure() - Feature Not Supported on this device!");

            timesCalibrated = 100;
            IPinCalib       = false;
            return;
        }

        //make sure we're in range
        if ((wIndex >= expMin) && (wIndex <= expMax)) {
            Log.d(TAG, "=== adjustExposure() - adjustValue = " + adjustValue);
            cameraParameters.setExposureCompensation((int) wIndex);
            mCamera.setParameters(cameraParameters);
        } else {
            if (wIndex > 0 && expIndx < expMax) {
                wIndex = expMax;
                cameraParameters.setExposureCompensation((int) wIndex);
                mCamera.setParameters(cameraParameters);

//                Log.d("   === adjustExposure", "set exposure to " + (int)wIndex);
            } else if (wIndex < 0 && expIndx > expMin) {
                wIndex = expMin;
                cameraParameters.setExposureCompensation((int) wIndex);
                mCamera.setParameters(cameraParameters);

//                Log.d(TAG, "   === adjustExposure() - set exposure to " + (int)wIndex);
            } else { // apply ISO change

                ISOValues = getISOValues();
                String[] supportedISOs = ISOValues.split(",");

                if(ISOValuesParameter != null) {

                    ISOParameter = ISOValuesParameter.replace("-values", "");
                    String mISO  = cameraParameters.get(ISOParameter);
                    if( mISO.equals("100") ) {

                        for (int i = 0; i < supportedISOs.length; i++) {
                            if (supportedISOs[i].equals("100")) {
                                cameraParameters.set(ISOParameter, "100");
                                Log.d(TAG, "  -------adjustExposure() setting ISO to 100!");

                                break;
                            }
                        }

                        mCamera.setParameters(cameraParameters);
                        mCamera.setParameters(cameraParameters);
                    } else {
                        Log.d(TAG, "  --- adjustExposure() ISO was 100 ---");
                    }
                } else {
                    Log.d(TAG, "  ------------ adjustExposure() Can't Set ISO; not supported.");
                    // get the updated ISO value
//                    String ISO = cameraParameters.get(ISOParameter);
//                    Toast.makeText(this,"ISO set to: " + ISO, Toast.LENGTH_SHORT).show();
                }

            }
        }

        if( !IPAutoFocusBusy ) {
            //run the auto focus first to use as a delay
            IPAutoFocusBusy = true;
            IPinCalib       = true;

            Log.d(TAG, "  -------adjustExposure() autoFocus activated...");

            mCamera.autoFocus(new AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    timesCalibrated++;
                    IPAutoFocusBusy = false;
                    IPinCalib       = false;

                    if (timesCalibrated >= 3) {
                        // if we've calibrated 3 times, we can start the IP, state change is handled in the function
                        Log.d(TAG, "  -----adjustExposure()/autoFocus() calling startIP().");

                        DeviceTimerRating--;

                        startIP();
                    } else {
                        Log.d(TAG, " !!!! adjustExposure()/autoFocus() executed !!!");

                        IPState   = S_CALIB;
                    }
                }
            });

        }
    }

    // returns a list with supported ISO values
    private String  getISOValues() {
        ISOValuesParameter = getISOValuesParameter();
        Camera.Parameters params = mCamera.getParameters();
        ISOValues = params.get(ISOValuesParameter);

        if(ISOValues == null) {
            Log.d(TAG, " XXX getISOValues() - current device doesn't support ISO change XXX");
        }

        return ISOValues!=null ? ISOValues : "ISO not supported";
    }


    // this will return the name of the ISO parameter containing supported ISO values
    private String  getISOValuesParameter() {
        Camera.Parameters params = mCamera.getParameters();

        String flatten = params.flatten();
        String[] paramsSeparated = flatten.split(";");
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


//---------------------------------------------------- shooting in progress functions --------------------------------------------------------

    private int[] showShotsByMasterList(int seqID, ArrayList<Integer> mHList) {
        //
        //  updated ShowHit function.                                                             05/25/2017
        //
        //  updated return array to include valid last shot's time for Split-Time calculation.    11/06/2018
        //  (we return the shot time so that it's easier to verify the result.)
        //
        //-----------------------------------------------------------------------------------------------------------------------
        //
        // returns int[6]:
        //      [0]:    -1 - nothing to show (empty or bad list)
        //          gSeqID - actual sequence ID, based on the given seqID, used in the display
        //
        //      [1]:    -1 - failed to locate shot
        //          session index;
        //
        //      [2]:    -1 - failed to locate shot
        //          session hit index;
        //
        //      [3]:    -1 - failed to locate shot
        //          hit time in 1/100 seconds;
        //
        //      [4]:    -1 - failed to locate shot
        //          total hit counts in current session;
        //
        //      [5]:    -1 - no valid value found
        //          hit time of last hit in same session in 1/100 seconds;
        //

        Log.d(TAG, "showShotsByMasterList() > seqID = " + seqID);
        int     info[] = {-1, -1, -1, -1, -1, -1};      // [ master hit list index, session index, session hit index, hit time,
                                                        //   total hit counts (session/record), Last Shot hit time ]

        if (seqID < 0 || mHList.size() < 3 || SessionsLimit < 1) {
            Log.d(TAG, " -- showShotsByMasterList() Bad Data: mHList.size() = " + mHList.size() +", # Sessions = "+SessionsLimit);
            return info;
        }

        int nTime = (int) (System.currentTimeMillis() / 60000);
        if (mHList.get(0) < 24930000 || mHList.get(0) > nTime) { // failed sanity check
            Log.d(TAG, " -- showShotsByMasterList() Bad Timestamp: nTime | stamp"+nTime+" | "+mHList.get(0));
            return info;
        }

//        splitTimeBar.setText("");

        if (shots < 1 || (seqID == 0 && mHList.size() > 0)) { // show All
            Log.d(TAG, " -- showShotsByMasterList() calling showAllShots().");

            showAllShots();
            info[0] = 0;
            info[4] = shots;

            return info;
        }


        if( (seqID-1)%(ShotsLimit +1) == 0 ) { // need to show hit group in a session
            // first make sure that we have a non-empty session
            Log.d(TAG, " -- showShotsByMasterList() in Session Header section");

            if (mHList.get(seqID) > 0) { // got the right header
                info[0] = seqID;
                info[1] = (seqID - 1) / (ShotsLimit + 1);
                info[4] = mHList.get(seqID);

                Bitmap bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
                Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker), 40, 40, false);
                Canvas    canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(bgImage, new Matrix(), null);

                for (int j = seqID + 1; j <= seqID + ShotsLimit; j++) {
                    if (mHList.get(j) > 0) { // one hit in session
                        final int fmID = mHList.get(j);
                        laserHit wHit;

                        for (int i = 0; i < HitList.size(); i++) {
                            wHit = HitList.get(i);

                            if (wHit.FmNum == fmID) {
                                int[]  sPos = wHit.findAvgXYPlus(5000, true);
                                if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0]-20, sPos[1]-20, null);

                                break;
                            }
                        }
                    } else {
                        break; // done!
                    }
                }
                //need to rotate this 90 degrees
                Matrix matrix = new Matrix();
                matrix.preRotate(90);

                final Bitmap finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);
                final TimerActivity mActivity = (TimerActivity) getContext();

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.drawImage(finalImg);
                    }
                });
            }

            else { // need to locate the header!
                boolean found = false;
                int hIndx = seqID;
                int gIndx = (seqID - 1) / (ShotsLimit + 1);

                while (hIndx < mHList.size() && gIndx < SessionsLimit - 1 && !found) {
                    //                gIndx++;
                    //                hIndx += ShotsLimit +1;
                    if (mHList.get(hIndx) > 0) {
                        info[0] = hIndx;
                        info[1] = gIndx;
                        found = true;
                    }
                    gIndx++;
                    hIndx += ShotsLimit + 1;
                }

                if (!found) {
                    hIndx = 1;
                    gIndx = 0;
                    while (hIndx < seqID && gIndx < SessionsLimit - 1 && !found) {
                        if (mHList.get(hIndx) > 0) {
                            info[0] = hIndx;
                            info[1] = gIndx;
                            found = true;
                        } else {
                            gIndx++;
                            hIndx += ShotsLimit + 1;
                        }
                    }
                }

                if (!found) {
                    Log.d(TAG, "XXXXX showShotsByMasterList() failed to find valid session to show!");
                    return info;
                }

//                Log.d(TAG, "   -- showShotsByMasterList() found group to display for session ID: " + gIndx);


                for (int j = hIndx + 1; j <= hIndx + ShotsLimit; j++) {
                    if (mHList.get(j) > 0) { // one hit in session
                        final int fmID = mHList.get(j);
                        laserHit wHit;

                        for (int i = 0; i < HitList.size(); i++) {
                            wHit = HitList.get(i);

                            if (wHit.FmNum == fmID) {

                                Bitmap bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
                                Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker), 40, 40, false);
                                Canvas    canvas = new Canvas(bmOverlay);
                                canvas.drawBitmap(bgImage, new Matrix(), null);

                                int[]  sPos = wHit.findAvgXYPlus(5000, true);
                                if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0]-20, sPos[1]-20, null);

                                //need to rotate this 90 degrees
                                Matrix matrix = new Matrix();
                                matrix.preRotate(90);

                                final Bitmap finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);
                                final TimerActivity mActivity = (TimerActivity) getContext();

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
                }

                info[4] = sessionHitsCount(info[1]);
            }
            Log.d(TAG, "    -- showShotsByMasterList() returned session group hit info[]: "+info[0]+", "+info[1]+", "+info[2]+", "+info[3]+", "+info[4]);

            return info;
        } else { // draw single shot

            //  Locate Last Shot
            if(seqID == 2) {
                Log.d(TAG, "\n    -- showShotsByMasterList() first shot has no valid SplitTime! --\n");
                int  frameID = mHList.get(seqID);
                int info_b[] = showShotInfoFromFrameRcds_noSplit(frameID); // [ Session #, Session hit ID, Hit time lapse, Session Hit Count ]

                Log.d(TAG, "  -- showShotsByMasterList()/showShotInfoFromFrameRcds_old() returned info_b[]: " + info_b[0] + ", " + info_b[1] + ", " + info_b[2] + ", " + info_b[3]);

                info[0] = seqID;     // Master Hit Index
                info[1] = info_b[0]; // Session Index
                info[2] = info_b[1]; // Session Hit Index
                info[3] = info_b[2]; // Hit Time
                info[4] = info_b[3]; // Hit Population
                Log.d(TAG, "    -- showShotsByMasterList() found single hit info[]: " + info[0] + ", " + info[1] + ", " + info[2] + ", " + info[3] + ", " + info[4]);

            } else { // (seqID > 2)
                int frameID_a = mHList.get(seqID - 1);
                int info_a[] = getShotInfoFromFrameRcds(frameID_a); // [ Session #, Session hit ID, Hit time lapse, Session Hit Count ]

                Log.d(TAG, "    -- showShotsByMasterList() found LAST hit info[]: " + info_a[0] + ", " + info_a[1] + ", " + info_a[2] + ", " + info_a[3]);

                int  frameID = mHList.get(seqID);
                int info_b[] = showShotInfoFromFrameRcds(frameID, info_a[0], info_a[2]); // [ Session #, Session hit ID, Hit time lapse, Session Hit Count ]

                Log.d(TAG, "  -- showShotsByMasterList()/showShotInfoFromFrameRcds() returned info_b[]: " + info_b[0] + ", " + info_b[1] + ", " + info_b[2] + ", " + info_b[3]);

                info[0] = seqID;     // Master Hit Index
                info[1] = info_b[0]; // Session Index
                info[2] = info_b[1]; // Session Hit Index
                info[3] = info_b[2]; // Hit Time
                info[4] = info_b[3]; // Hit Population
                Log.d(TAG, "    -- showShotsByMasterList() found single hit info[]: " + info[0] + ", " + info[1] + ", " + info[2] + ", " + info[3] + ", " + info[4]);


                if (info_a[0] == info[1] && info[3] > info_a[2]) { // same Session, take it
                    info[5] = info_a[2];

                    // write Split Time
                    Log.d(TAG, "    -- showShotsByMasterList() full output info[]: " + info[0] + ", " + info[1] + ", " + info[2] + ", " + info[3] + ", " + info[4] + ", " + info[5]);
                } else {
                    Log.d(TAG, "\n    -- showShotsByMasterList() didn't find valid SplitTime! --\n");
                }
            }

            return info;
        }
    }


    private int[] showShotsByMasterList_old(int seqID, ArrayList<Integer> mHList) {
        // updated ShowHit function. 05/25/2017
        //
        // returns int[5]:
        //      [0]:    -1 - nothing to show (empty or bad list)
        //          gSeqID - actual sequence ID, based on the given seqID, used in the display
        //
        //      [1]:    -1 - failed to locate shot
        //          session index;
        //
        //      [2]:    -1 - failed to locate shot
        //          session hit index;
        //
        //      [3]:    -1 - failed to locate shot
        //          hit time in 1/100 seconds;
        //
        //      [4]:    -1 - failed to locate shot
        //          total hit counts in current session;
        //

        Log.d(TAG, "showShotsByMasterList_old() > seqID = " + seqID);
        int     info[] = {-1, -1, -1, -1, -1}; // [ master hit list index, session index, session hit index, hit time, total hit counts (session/record) ]

        if (seqID < 0 || mHList.size() < 3 || SessionsLimit < 1) {
            Log.d(TAG, " -- showShotsByMasterList() Bad Data: mHList.size() = " + mHList.size() +", # Sessions = "+SessionsLimit);
            return info;
        }

        int nTime = (int) (System.currentTimeMillis() / 60000);
        if (mHList.get(0) < 24930000 || mHList.get(0) > nTime) { // failed sanity check
            Log.d(TAG, " -- showShotsByMasterList() Bad Timestamp: nTime | stamp"+nTime+" | "+mHList.get(0));
            return info;
        }

        if (shots < 1 || (seqID == 0 && mHList.size() > 0)) { // show All
            Log.d(TAG, " -- showShotsByMasterList() calling showAllShots().");

            showAllShots();
            info[0] = 0;
            info[4] = shots;

            return info;
        }

        int  sIndx = 0;
        if( (seqID-1)%(ShotsLimit +1) == 0 ) { // need to show hit group in a session
            // first make sure that we have a non-empty session
            Log.d(TAG, " -- showShotsByMasterList() in Session Header section");

            if (mHList.get(seqID) > 0) { // got the right header
                info[0] = seqID;
                info[1] = (seqID - 1) / (ShotsLimit + 1);
                info[4] = mHList.get(seqID);

                Bitmap bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
                Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker), 40, 40, false);
                Canvas    canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(bgImage, new Matrix(), null);

                for (int j = seqID + 1; j <= seqID + ShotsLimit; j++) {
                    if (mHList.get(j) > 0) { // one hit in session
                        final int fmID = mHList.get(j);
                        laserHit wHit;

                        for (int i = 0; i < HitList.size(); i++) {
                            wHit = HitList.get(i);

                            if (wHit.FmNum == fmID) {
                                int[]  sPos = wHit.findAvgXYPlus(5000, true);
                                if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0]-20, sPos[1]-20, null);

                                break;
                            }
                        }
                    } else {
                        break; // done!
                    }
                }
                //need to rotate this 90 degrees
                Matrix matrix = new Matrix();
                matrix.preRotate(90);

                final Bitmap finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);
                final TimerActivity mActivity = (TimerActivity) getContext();

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.drawImage(finalImg);
                    }
                });
            }

            else { // need to locate the header!
                boolean found = false;
                int hIndx = seqID;
                int gIndx = (seqID - 1) / (ShotsLimit + 1);

                while (hIndx < mHList.size() && gIndx < SessionsLimit - 1 && !found) {
                    //                gIndx++;
                    //                hIndx += ShotsLimit +1;
                    if (mHList.get(hIndx) > 0) {
                        info[0] = hIndx;
                        info[1] = gIndx;
                        found = true;
                    }
                    gIndx++;
                    hIndx += ShotsLimit + 1;
                }

                if (!found) {
                    hIndx = 1;
                    gIndx = 0;
                    while (hIndx < seqID && gIndx < SessionsLimit - 1 && !found) {
                        if (mHList.get(hIndx) > 0) {
                            info[0] = hIndx;
                            info[1] = gIndx;
                            found = true;
                        } else {
                            gIndx++;
                            hIndx += ShotsLimit + 1;
                        }
                    }
                }

                if (!found) {
                    Log.d(TAG, "XXXXX showShotsByMasterList() failed to find valid session to show!");
                    return info;
                }

//                Log.d(TAG, "   -- showShotsByMasterList() found group to display for session ID: " + gIndx);


                for (int j = hIndx + 1; j <= hIndx + ShotsLimit; j++) {
                    if (mHList.get(j) > 0) { // one hit in session
                        final int fmID = mHList.get(j);
                        laserHit wHit;

                        for (int i = 0; i < HitList.size(); i++) {
                            wHit = HitList.get(i);

                            if (wHit.FmNum == fmID) {

                                Bitmap bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
                                Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker), 40, 40, false);
                                Canvas    canvas = new Canvas(bmOverlay);
                                canvas.drawBitmap(bgImage, new Matrix(), null);

                                int[]  sPos = wHit.findAvgXYPlus(5000, true);
                                if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0]-20, sPos[1]-20, null);

                                //need to rotate this 90 degrees
                                Matrix matrix = new Matrix();
                                matrix.preRotate(90);

                                final Bitmap finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);
                                final TimerActivity mActivity = (TimerActivity) getContext();

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
                }

                info[4] = sessionHitsCount(info[1]);
            }
            Log.d(TAG, "    -- showShotsByMasterList_old() B2 returned session group hit info[]: "+info[0]+", "+info[1]+", "+info[2]+", "+info[3]+", "+info[4]);

            return info;
        } else { // draw single shot

            int  frameID = mHList.get(seqID);

            int info_b[] = showShotInfoFromFrameRcds_noSplit(frameID); // [ Session #, Session hit ID, Hit time lapse, Session Hit Count ]

            Log.d(TAG, "  -- showShotsByMasterList()/showShotInfoFromFrameRcds() returned info_b[]: "+info_b[0]+", "+info_b[1]+", "+info_b[2]+", "+info_b[3]);

            info[0] = seqID;     // Master Hit Index
            info[1] = info_b[0]; // Session Index
            info[2] = info_b[1]; // Session Hit Index
            info[3] = info_b[2]; // Hit Time
            info[4] = info_b[3]; // Hit Population
            Log.d(TAG, "    -- showShotsByMasterList() B3 returned single hit info[]: "+info[0]+", "+info[1]+", "+info[2]+", "+info[3]+", "+info[4]);
            return info;
        }
    }




    private int sessionHitsCount(int sessIndx) {
        int   hLen = HitSMaster.size();
        int  count = 0;

        if( hLen < 3) return count;

        count = HitSMaster.get(sessIndx*(ShotsLimit +1) + 1);

        Log.d(TAG, "sessionHitsCount() session | returned count: "+sessIndx +" | "+count);

        return count;
    }


    private  void showShotforStartFrame(int frameID)
    // Real-time shot display function for a single-frame shot

    {
        Log.d(TAG, "showShotforStartFrame > Frame = " +frameID);
/*
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                splitTimeBar.setText("");
            }
        });
*/

        laserHit wHit;
        for(int i = 0; i<HitList.size(); i++)
        {
            wHit = HitList.get(i);

            if(wHit.FmNum == frameID)
            {
                Bitmap bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
                Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker),40,40,false);
                Canvas    canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(bgImage, new Matrix(), null);

                int[]  sPos = wHit.findAvgXYPlus(5000, true);
                if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0] - 20, sPos[1] - 20, null);

                Matrix matrix = new Matrix();
                matrix.preRotate(90);

                final         Bitmap  finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);
                final  TimerActivity mActivity = (TimerActivity) getContext();

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


    private  int[] getShotInfoFromFrameRcds(int frame) {
        // shot info extraction function from shot frame records

        Log.d(TAG, "getShotInfoFromFrameRcds() - looking for Frame Index = " + frame);

        int[]  info = {-1, -1, -1, -1}; // [ Session #, Session hit ID, Session time lapse, Session Hit Count ]


        boolean  found = false;
        laserHit wHit;
        int      i, size = 0;
        int      listLen  = HitList.size();

        if(listLen < 1 || frame < 0) {
            Log.d(TAG, "XXXXXXX getShotInfoFromFrameRcds() bad data: listLen | frame = "+listLen+" | "+frame);
            return info;
        }

        for (i = 0; i < listLen; i++) {
            wHit = HitList.get(i);
            if (wHit.FmNum == frame) {
                found      = true;
                info[0]    = wHit.session;
                info[1]    = wHit.hitID;
                info[2]    = (int)wHit.cTime;
                Log.d(TAG, "----getShotInfoFromFrameRcds() found sessionNum, HitID, lapse time: "+info[0]+", "+info[1]+", "+info[2]);

                break;
            }
        }

        if(!found) {
            Log.d(TAG, "XXXXXXX getShotInfoFromFrameRcds() failed locating frame!");
            return info;
        }

        int hID = -1;
        Log.d(TAG, "----getShotInfoFromFrameRcds() calculating HitCount for session: "+info[0]);
        for (i = 0; i < listLen; i++) {
            wHit = HitList.get(i);
//            Log.d(TAG, "----getShotInfoFromFrameRcds() checking session# | HitID: "+wHit.session+" | "+wHit.hitID);
            if (wHit.session == info[0] && hID != wHit.hitID) {
                hID = wHit.hitID;
                size++;
            }
        }

        info[3] = size;

        return info;
    }


    private  int[] showShotInfoFromFrameRcds(int frame, int LSsession, int LStime) {
        // Accurate shot display function from shot frame records

        Log.d(TAG, "showShotInfoFromFrameRcds() - looking for Frame Index = " + frame);
        Log.d(TAG, "   showShotInfoFromFrameRcds() - Last Hit Session / Hit Time = " + LSsession + " / " + LStime);

        int[]  info = {-1, -1, -1, -1}; // [ Session #, Session hit ID, Session time lapse, Session Hit Count ]

        // draw background first
        Bitmap bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
        Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker), 40, 40, false);
        Canvas    canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bgImage, new Matrix(), null);


        boolean  found = false;
        laserHit wHit;
        laserHit wHitH = null;
        int      i, size = 0;
        int      listLen  = HitList.size();

        if(listLen < 1 || frame < 0) {
            Log.d(TAG, "XXXXXXX showShotInfoFromFrameRcds() bad data: listLen | frame = "+listLen+" | "+frame);
            return info;
        }

        for (i = 0; i < listLen; i++) {
            wHit = HitList.get(i);
            if (wHit.FmNum == frame) {
                found      = true;
                info[0]    = wHit.session;
                info[1]    = wHit.hitID;
                info[2]    = (int)wHit.cTime;
                Log.d(TAG, "----showShotInfoFromFrameRcds() found sessionNum, HitID, lapse time: "+info[0]+", "+info[1]+", "+info[2]);

                wHitH = HitList.get(i);
                break;
            }
        }

        if(!found) {
            Log.d(TAG, "XXXXXXX showShotInfoFromFrameRcds() failed locating frame!");
            return info;
        }

        int hID = -1;
        Log.d(TAG, "----showShotInfoFromFrameRcds() calculating HitCount for session: "+info[0]);
        for (i = 0; i < listLen; i++) {
            wHit = HitList.get(i);
//            Log.d(TAG, "----showShotInfoFromFrameRcds() checking session# | HitID: "+wHit.session+" | "+wHit.hitID);
            if (wHit.session == info[0] && hID != wHit.hitID) {
                hID = wHit.hitID;
                size++;
            }
        }

        info[3] = size;

        int[]  sPos = wHitH.findAvgXYPlus(5000, true);
        if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0] - 20, sPos[1] - 20, null);

        //need to rotate this 90 degrees
        Matrix matrix = new Matrix();
        matrix.preRotate(90);

        final  Bitmap         finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);
        final  TimerActivity mActivity = (TimerActivity) getContext();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.drawImage(finalImg);
            }
        });

        return info;
    }

    private  int[] showShotInfoFromFrameRcds_SplitTimeOL(int frame, int LSsession, int LStime) {
        // Accurate shot display function from shot frame records

        Log.d(TAG, "showShotInfoFromFrameRcds() - looking for Frame Index = " + frame);
        Log.d(TAG, "   showShotInfoFromFrameRcds() - Last Hit Session / Hit Time = " + LSsession + " / " + LStime);

        int[]  info = {-1, -1, -1, -1}; // [ Session #, Session hit ID, Session time lapse, Session Hit Count ]

        // draw background first
        Bitmap bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
        Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker), 40, 40, false);
        Canvas    canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bgImage, new Matrix(), null);


        boolean  found = false;
        laserHit wHit;
        laserHit wHitH = null;
        int      i, size = 0;
        int      listLen  = HitList.size();

        if(listLen < 1 || frame < 0) {
            Log.d(TAG, "XXXXXXX showShotInfoFromFrameRcds() bad data: listLen | frame = "+listLen+" | "+frame);
            return info;
        }

        for (i = 0; i < listLen; i++) {
            wHit = HitList.get(i);
            if (wHit.FmNum == frame) {
                found      = true;
                info[0]    = wHit.session;
                info[1]    = wHit.hitID;
                info[2]    = (int)wHit.cTime;
                Log.d(TAG, "----showShotInfoFromFrameRcds() found sessionNum, HitID, lapse time: "+info[0]+", "+info[1]+", "+info[2]);

                wHitH = HitList.get(i);
                break;
            }
        }

        if(!found) {
            Log.d(TAG, "XXXXXXX showShotInfoFromFrameRcds() failed locating frame!");
            return info;
        }

        int hID = -1;
        Log.d(TAG, "----showShotInfoFromFrameRcds() calculating HitCount for session: "+info[0]);
        for (i = 0; i < listLen; i++) {
            wHit = HitList.get(i);
//            Log.d(TAG, "----showShotInfoFromFrameRcds() checking session# | HitID: "+wHit.session+" | "+wHit.hitID);
            if (wHit.session == info[0] && hID != wHit.hitID) {
                hID = wHit.hitID;
                size++;
            }
        }

        info[3] = size;

        int[]  sPos = wHitH.findAvgXYPlus(5000, true);
        if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0] - 20, sPos[1] - 20, null);

        // Handle Split Time
        if(LSsession == info[0] && LStime < info[2] ) { // same Session, take it

            final int    sp1000   =  info[2] - LStime;
            final int    spSubSec = sp1000%1000;
            String         spTime = "Split Time: " + Integer.toString(sp1000/1000) + ":";

            if      (spSubSec >= 100)  spTime += Integer.toString(spSubSec);
            else if (spSubSec >= 10 )  spTime += ("0" + Integer.toString(spSubSec));
            else if (spSubSec >= 1  )  spTime += ("00" + Integer.toString(spSubSec));
            else                       spTime += "000";

            Log.d(TAG, "----showShotInfoFromFrameRcds()_old writing Split Time: "+spTime);

//            splitTimeBar.setText(spTime);
        }

        //need to rotate this 90 degrees
        Matrix matrix = new Matrix();
        matrix.preRotate(90);

        final  Bitmap         finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);
        final  TimerActivity mActivity = (TimerActivity) getContext();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.drawImage(finalImg);
            }
        });

        return info;
    }


    private  int[] showShotInfoFromFrameRcds_noSplit(int frame) {
        // Accurate shot display function from shot frame records.
        //
        // This function doesn't compute Split-Time!
        //

        Log.d(TAG, "showShotInfoFromFrameRcds_old() - looking for Frame Index = " + frame);

        int[]  info = {-1, -1, -1, -1}; // [ Session #, Session hit ID, Session time lapse, Session Hit Count ]

        // draw background first
        Bitmap bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
        Bitmap hitMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker), 40, 40, false);
        Canvas    canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bgImage, new Matrix(), null);


        boolean  found = false;
        laserHit wHit;
        laserHit wHitH = null;
        int      i, size = 0;
        int      listLen  = HitList.size();

        if(listLen < 1 || frame < 0) {
            Log.d(TAG, "XXXXXXX showShotInfoFromFrameRcds_old() bad data: listLen | frame = "+listLen+" | "+frame);
            return info;
        }

        for (i = 0; i < listLen; i++) {
            wHit = HitList.get(i);
            if (wHit.FmNum == frame) {
                found      = true;
                info[0]    = wHit.session;
                info[1]    = wHit.hitID;
                info[2]    = (int)wHit.cTime;
                Log.d(TAG, "----showShotInfoFromFrameRcds_old() found sessionNum, HitID, lapse time: "+info[0]+", "+info[1]+", "+info[2]);

                wHitH = HitList.get(i);
                break;
            }
        }

        if(!found) {
            Log.d(TAG, "XXXXXXX showShotInfoFromFrameRcds_old() failed locating frame!");
            return info;
        }

        int hID = -1;
        Log.d(TAG, "----showShotInfoFromFrameRcds_old() calculating HitCount for session: "+info[0]);
        for (i = 0; i < listLen; i++) {
            wHit = HitList.get(i);
//            Log.d(TAG, "----showShotInfoFromFrameRcds() checking session# | HitID: "+wHit.session+" | "+wHit.hitID);
            if (wHit.session == info[0] && hID != wHit.hitID) {
                hID = wHit.hitID;
                size++;
            }
        }

        info[3] = size;

        int[]  sPos = wHitH.findAvgXYPlus(5000, true);
        if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0] - 20, sPos[1] - 20, null);

        //need to rotate this 90 degrees
        Matrix matrix = new Matrix();
        matrix.preRotate(90);

        final  Bitmap         finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);
        final  TimerActivity mActivity = (TimerActivity) getContext();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.drawImage(finalImg);
            }
        });

        return info;
    }


    private int findNextSessionStartIndx(int startID, ArrayList<Integer> mHList, int dir) {
        // find the closest session header index in HitMasterList and return it
        //
        //  success: a positive int
        //  failure: -1
        Log.d(TAG, "findNextSessionStartIndx() entered: startID = "+startID +", dir = "+dir);

        if(mHList.size() < 2 || startID < 0 || startID >= mHList.size()) return -1;

        if(dir < 0)    dir = 0; // incremental search
        if(dir > 1)    dir = 1; // decremental search

//        final int         hLen  = mHList.size();
//        final int         hLenm = hLen - 1;
        final int   ShotsLimitp = ShotsLimit + 1;
        int           org_gIndx = (startID == 0)? -1 : (startID-1)/ShotsLimitp;
        int        org_headIndx = (startID == 0)?  0 : org_gIndx * ShotsLimitp + 1;


        /// 1. define the header positions and the range
        /// 2. check if hIndx is at a header position; if so, return it
        /// 3. If not, check the closet header position and return it

//        Log.d(TAG, "-- findNextSessionStartIndx() started with org_gIndx | org_headIndx = "+org_gIndx +" | "+org_headIndx);
        if(startID == 1 && dir == 1) {
//            Log.d(TAG, "-- findNextSessionStartIndx()-a returns 0");
            return 0;
        }

//        final int maxHePos = ShotsLimitp*(SessionsLimit-1) + 1;

        // first we build the valid header position array
        ArrayList<Integer>  HeaderList;
        HeaderList = new ArrayList<>();
        HeaderList.add(0);

        for(int i=0; i<SessionsLimit; i++) {
            if(mHList.get(ShotsLimitp * i + 1) > 0) {
                HeaderList.add(ShotsLimitp * i + 1);
            }
        }
        final int      hListLen = HeaderList.size();
        int       org_hListIndx = 0;

//        Log.d(TAG, "-- findNextSessionStartIndx() built HeaderList[]: "+HeaderList);

        // find the closest NON-ZERO session header index in the direction dir
        if (org_gIndx == -1) {
            org_hListIndx = 0;
        } else {
            for(int i=0; i<hListLen; i++) {
                if(HeaderList.get(i) == org_headIndx) {
                    org_hListIndx = i;
//                    Log.d(TAG, " -- findNextSessionStartIndx() found org_hListIndx: "+org_hListIndx);
                    break;
                }
            }
        }

        if(dir == 1) { // next smaller one
            if(org_hListIndx == 0) {
                final int ngIndx = HeaderList.get(hListLen - 1);

//                Log.d(TAG, "-- findNextSessionStartIndx()-a returns: " + ngIndx);
                return ngIndx;
            } else {
                final int ngIndx = HeaderList.get(org_hListIndx - 1);

//                Log.d(TAG, "-- findNextSessionStartIndx()-b returns: " + ngIndx);
                return ngIndx;
            }

        } else { // next bigger one
            if(org_hListIndx == hListLen - 1) {

//                Log.d(TAG, "-- findNextSessionStartIndx()-c returns: 0");
                return 0;
            } else {
                final int ngIndx = HeaderList.get(org_hListIndx + 1);

//                Log.d(TAG, "-- findNextSessionStartIndx()-d returns: " + ngIndx);
                return ngIndx;
            }
        }
    }


    //----------------------------------------------------- review functions ---------------------------------------------------------------


    //direction 0 is left to right, 1 is right to left
    private void ReviewbySwipe(int direction)
    {
//        Log.d(TAG, "===ReviewbySwipe() entered...currentreviewShotID = "+currentreviewShotID);

        if(shots < 2) return; // at most a single hit

        final int  hLen = HitSMaster.size();
        final int hLenm = hLen - 1;
        boolean   found = false;

        if(direction == 0)
        {
            for(int i = currentreviewShotID-1; i > 0; i--) {
                if(HitSMaster.get(i) > 0) {
                    found               = true;
                    currentreviewShotID = i;
                    break;
                }
            }
            if(!found) {
                for(int i = hLenm; i > currentreviewShotID; i--) {
                    if(HitSMaster.get(i) > 0) {
                        found               = true;
                        currentreviewShotID = i;
                        break;
                    }
                }
            }

            if(!found) {
//                Log.d(TAG, " XXXXXX ReviewbySwipe() failed to find a valid hit!");
                return;
            }

            if(currentreviewShotID == 0)
            {
                showAllShots();
            } else {
//                int  sInfo_old[] = ReviewbySwipe(currentreviewShotID,  HitSMaster);

                int  sInfo[]     = showShotsByMasterList(currentreviewShotID,  HitSMaster);

                if (sInfo[0] > 1 && sInfo[2] > 0) {
                    SpannableStringBuilder builder = new SpannableStringBuilder();
                    if(sInfo[1] == 0) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_1_green_24dp), 0);
                    } else if(sInfo[1] == 1) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_2_green_24dp), 0);
                    } else if(sInfo[1] == 2) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_3_green_24dp), 0);
                    } else if(sInfo[1] == 3) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_4_green_24dp), 0);
                    } else if(sInfo[1] == 4) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_5_green_24dp), 0);
                    }  else if(sInfo[1] == 5) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_6_green_24dp), 0);
                    } else if(sInfo[1] == 6) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_7_green_24dp), 0);
                    } else if(sInfo[1] == 7) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_8_green_24dp), 0);
                    }

                    int     spSubSec = (sInfo[3]%1000)/10;
                    String  spSubSecString;

                    if       (spSubSec < 1  )  spSubSecString = "00";
                    else if  (spSubSec < 10 )  spSubSecString = ("0" + Integer.toString(spSubSec));
                    else                       spSubSecString = Integer.toString(spSubSec);

                    // ---- Split Time
                    final int      sp1000 =  sInfo[3] - sInfo[5];
                    final boolean    spOn = (sInfo[2] > 1 && sp1000 > 100);

                    //---------------------------------------

                    builder.append("   ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_adjust_green_24dp), 0)
                            .append("  "+ sInfo[2])
                            .append("   ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_timer_green_24dp), 0)
                            .append("  "+ sInfo[3]/1000+"."+spSubSecString)
                            .append("   ");

                    if(spOn) {
                        String    spSubSecString1;
                        spSubSec  = (sp1000%1000)/10;

                        if       (spSubSec < 1  )  spSubSecString1 = "00";
                        else if  (spSubSec < 10 )  spSubSecString1 = ("0" + Integer.toString(spSubSec));
                        else                       spSubSecString1 = Integer.toString(spSubSec);

                        Log.d(TAG, "----ReviewbySwipe() writing Split Time: "+spSubSecString1);

                        builder.append("    ", new ImageSpan(this.getContext(), R.mipmap.ic_splittime_green_24dp), 0)
                                .append("  " + sp1000 / 1000 + "." + spSubSecString1);
                    } else  {
                        builder.append("    ", new ImageSpan(this.getContext(), R.mipmap.ic_splittime_green_24dp), 0)
                                .append("  --.--- ");
                    }

//                    if (spSubSec > 0 && spSubSec%10 == 0 )       builder.append("0");  // for 3-digit display

                    statusBar.setText(builder);
                    currentreviewShotID = sInfo[0];
                } else if (sInfo[0] > 0 && sInfo[2] < 0) {
                    SpannableStringBuilder builder = new SpannableStringBuilder();

                    if(sInfo[1] == 0) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_1_green_24dp), 0);
                    } else if(sInfo[1] == 1) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_2_green_24dp), 0);
                    } else if(sInfo[1] == 2) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_3_green_24dp), 0);
                    } else if(sInfo[1] == 3) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_4_green_24dp), 0);
                    } else if(sInfo[1] == 4) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_5_green_24dp), 0);
                    }  else if(sInfo[1] == 5) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_6_green_24dp), 0);
                    } else if(sInfo[1] == 6) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_7_green_24dp), 0);
                    } else if(sInfo[1] == 7) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_8_green_24dp), 0);
                    }

                    builder.append("    Session Shots: " + sInfo[4]);
                    statusBar.setText(builder);

                    currentreviewShotID = sInfo[0];
                } else {
                    String msg = "Shot " + currentreviewShotID + " can't be found.";
                    statusBar.setText(msg);
                }
            }
            return;
        } else if(direction == 1) {
            for(int i = currentreviewShotID+1; i <= hLenm; i++) {
                if(HitSMaster.get(i) > 0) {
                    found               = true;
                    currentreviewShotID = i;
                    Log.d(TAG, " ---ReviewbySwipe()-c <1> found currentreviewShotID "+currentreviewShotID);
                    break;
                }
            }
            if(!found) {
                for(int i = 0; i < currentreviewShotID; i++) {
                    if(HitSMaster.get(i) > 0) {
                        found               = true;
                        currentreviewShotID = i;
                        Log.d(TAG, " ---ReviewbySwipe()-d <1> found currentreviewShotID "+currentreviewShotID);
                        break;
                    }
                }
            }

            if(!found) {
                Log.d(TAG, " XXXXXX ReviewbySwipe() failed to find a valid hit!");
                return;
            }

            if(currentreviewShotID == 0)
            {
                showAllShots();
            } else {
                int  sInfo[] = showShotsByMasterList(currentreviewShotID,  HitSMaster);

                if (sInfo[0] > 1 && sInfo[2] > 0) {

                    SpannableStringBuilder builder = new SpannableStringBuilder();
                    if(sInfo[1] == 0) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_1_green_24dp), 0);
                    } else if(sInfo[1] == 1) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_2_green_24dp), 0);
                    } else if(sInfo[1] == 2) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_3_green_24dp), 0);
                    } else if(sInfo[1] == 3) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_4_green_24dp), 0);
                    } else if(sInfo[1] == 4) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_5_green_24dp), 0);
                    }  else if(sInfo[1] == 5) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_6_green_24dp), 0);
                    } else if(sInfo[1] == 6) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_7_green_24dp), 0);
                    } else if(sInfo[1] == 7) {
                        builder.append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_8_green_24dp), 0);
                    }

                    int     spSubSec = (sInfo[3]%1000)/10;
                    String  spSubSecString;

                    if       (spSubSec < 1  )  spSubSecString = "00";
                    else if  (spSubSec < 10 )  spSubSecString = ("0" + Integer.toString(spSubSec));
                    else                       spSubSecString = Integer.toString(spSubSec);

                    // ---- Split Time
                    final int      sp1000 =  sInfo[3] - sInfo[5];
                    final boolean    spOn = (sInfo[2] > 1 && sp1000 > 100);

                    //---------------------------------------

                    builder.append("   ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_adjust_green_24dp), 0)
                            .append("  "+ sInfo[2])
                            .append("   ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_timer_green_24dp), 0)
                            .append("  "+ sInfo[3]/1000+"."+spSubSecString)
                            .append("   ");

                    if(spOn) {
                        String    spSubSecString1;
                        spSubSec  = (sp1000%1000)/10;

                        if       (spSubSec < 1  )  spSubSecString1 = "00";
                        else if  (spSubSec < 10 )  spSubSecString1 = ("0" + Integer.toString(spSubSec));
                        else                       spSubSecString1 = Integer.toString(spSubSec);

                        Log.d(TAG, "----ReviewbySwipe() writing Split Time: "+spSubSecString1);

                        builder.append("    ", new ImageSpan(this.getContext(), R.mipmap.ic_splittime_green_24dp), 0)
                                .append("  " + sp1000 / 1000 + "." + spSubSecString1);
                    } else  {
                        builder.append("    ", new ImageSpan(this.getContext(), R.mipmap.ic_splittime_green_24dp), 0)
                                .append("  --.--- ");
                    }

//                    if (spSubSec > 0 && spSubSec%10 == 0 )       builder.append("0");  // for 3-digit display

                    statusBar.setText(builder);
                    currentreviewShotID = sInfo[0];

                } else if (sInfo[0] > 0 && sInfo[2] < 0) {
                    SpannableStringBuilder builder = new SpannableStringBuilder();

                    if(sInfo[1] == 0) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_1_green_24dp), 0);
                    } else if(sInfo[1] == 1) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_2_green_24dp), 0);
                    } else if(sInfo[1] == 2) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_3_green_24dp), 0);
                    } else if(sInfo[1] == 3) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_4_green_24dp), 0);
                    } else if(sInfo[1] == 4) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_5_green_24dp), 0);
                    }  else if(sInfo[1] == 5) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_6_green_24dp), 0);
                    } else if(sInfo[1] == 6) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_7_green_24dp), 0);
                    } else if(sInfo[1] == 7) {
                        builder.append(" ")
                                .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_8_green_24dp), 0);
                    }

                    builder.append("    Session Shots: " + sInfo[4]);
                    statusBar.setText(builder);

                    currentreviewShotID = sInfo[0];
                } else {
                    String msg = "Shot " + currentreviewShotID + " can't be found.";
                    statusBar.setText(msg);
                }
            }
            return;
        } else if(direction == 2) {
            currentreviewShotID = findNextSessionStartIndx(currentreviewShotID, HitSMaster, 0);

            int  sInfo[] = showShotsByMasterList(currentreviewShotID,  HitSMaster);

            if(sInfo[0] != 0) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                if(sInfo[1] == 0) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_1_green_24dp), 0);
                } else if(sInfo[1] == 1) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_2_green_24dp), 0);
                } else if(sInfo[1] == 2) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_3_green_24dp), 0);
                } else if(sInfo[1] == 3) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_4_green_24dp), 0);
                } else if(sInfo[1] == 4) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_5_green_24dp), 0);
                } else if(sInfo[1] == 5) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_6_green_24dp), 0);
                } else if(sInfo[1] == 6) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_7_green_24dp), 0);
                } else if(sInfo[1] == 7) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_8_green_24dp), 0);
                }

                builder.append("    Session Shots: " + sInfo[4]);

                statusBar.setText(builder);
            }
            currentreviewShotID = sInfo[0];

//            Log.d(TAG, "  =ReviewbySwipe() <2> display next session shots: "+currentreviewShotID);
            return;
        } else if(direction == 3) {
            currentreviewShotID = findNextSessionStartIndx(currentreviewShotID, HitSMaster, 1);

            int  sInfo[] = showShotsByMasterList(currentreviewShotID,  HitSMaster);

            if(sInfo[0] != 0) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                if(sInfo[1] == 0) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_1_green_24dp), 0);
                } else if(sInfo[1] == 1) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_2_green_24dp), 0);
                } else if(sInfo[1] == 2) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_3_green_24dp), 0);
                } else if(sInfo[1] == 3) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_4_green_24dp), 0);
                } else if(sInfo[1] == 4) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_5_green_24dp), 0);
                } else if(sInfo[1] == 5) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_6_green_24dp), 0);
                } else if(sInfo[1] == 6) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_7_green_24dp), 0);
                } else if(sInfo[1] == 7) {
                    builder.append(" ")
                            .append("  ", new ImageSpan(this.getContext(), R.mipmap.ic_filter_8_green_24dp), 0);
                }

                builder.append("    Session Shots: " + sInfo[4]);

                statusBar.setText(builder);
            }
            currentreviewShotID = sInfo[0];

//            Log.d(TAG, "  =ReviewbySwipe() <3> display previous session shots: "+currentreviewShotID);

        }
    }


    private void showAllShots()
    {
//        Log.d(TAG, " showAllShots() entered...shots = "+ shots+"  / MasterList: "+HitSMaster);

        if (IPState != S_REVIEW) {
            final int oldState = IPState;
            IPState            = S_REVIEW;

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.ActivityStateChange(oldState, S_REVIEW);
                }
            });
        }

//        splitTimeBar.setText("");

        final int  FmRate = IPFmRate;
        Bitmap  bmOverlay = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
        Bitmap hitMarker  = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.hitmarker),40,40,false);
        Canvas     canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bgImage, new Matrix(), null);

        int      wFrame;
        laserHit wHit;

        for (int i = 0; i < HitStarts.size(); i++) {
            wFrame = HitStarts.get(i);
//            Log.d(TAG, "showAllShots() - final - hitStart: " +wFrame);

            for (int j = 0; j < HitList.size(); j++) {
                wHit = HitList.get(j);

//                Log.d(TAG, "final - wantedHit" +wHit.FmNum);

                if (wHit.FmNum == wFrame) {
                    int[]  sPos = wHit.findAvgXYPlus(5000, true);
                    if(sPos[0] > 0) canvas.drawBitmap(hitMarker, sPos[0]-20, sPos[1]-20, null);

                    Matrix matrix = new Matrix();
                    matrix.preRotate(90);

                    break;
                }
            }
        }

        Matrix matrix = new Matrix();
        matrix.preRotate(90);

        final Bitmap  finalImg = Bitmap.createBitmap(bmOverlay, 0, 0, bmOverlay.getWidth(), bmOverlay.getHeight(), matrix, true);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.drawImage(finalImg);

                statusBar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

                String msg = shots + " shots detected (" + FmRate + " f/s)";

                statusBar.setText(msg);
            }
        });
    }


    //----------------------------------------------------- user control functions ---------------------------------------------------------------

    public boolean onTouchEvent(MotionEvent touchevent) {
//        Log.d(TAG, " onTouchEvent() in Mode: "+IPState);

        if(IPIgnoreTouch) {
            Log.d(TAG, " XXXXXXX onTouchEvent() - touch ignored due to Ignore Settings!");
            return true;
        }

        if(IPState == S_REVIEW)
        {
//            Log.d(TAG, " onTouchEvent() in Review Mode - touchevent: "+touchevent);
            final int mdist = 60; // 20;

            switch (touchevent.getAction()) {
                // when user first touches the screen we get x and y coordinate
                case MotionEvent.ACTION_DOWN: {
                    tc_x1 = touchevent.getX();
                    tc_y1 = touchevent.getY();
//                    Log.d(TAG, "  === touch down - x1= " +tc_x1+ ", y1= " +tc_y1);

                    return true;
                }
                case MotionEvent.ACTION_UP:
                {
                    tc_x2 = touchevent.getX();
                    tc_y2 = touchevent.getY();
                    final float  dstx = Math.abs(tc_x1 - tc_x2);
                    final float  dsty = Math.abs(tc_y1 - tc_y2);

//                    Log.d(TAG, " ===touch UP: x2=" +tc_x2+", y2=" +tc_y2);

                    if (tc_x1 < tc_x2 - mdist && dstx > dsty * 3) {
//                        Log.d(TAG, "   ===touch LR");
                        ReviewbySwipe(0);
                    } else if (tc_x1 > tc_x2 + mdist && dstx > dsty * 3) {
//                        Log.d(TAG, "   ===touch RL");
                        ReviewbySwipe(1);
                    } else if (tc_y1 < tc_y2 - mdist && dsty > dstx * 4) {
//                        Log.d(TAG, "   ===touch UD");
                        ReviewbySwipe(2);
                    } else if (tc_y1 > tc_y2 + mdist && dsty > dstx * 4) {
//                        Log.d(TAG, "   ===touch DU");
                        ReviewbySwipe(3);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int  action = event.getAction();
        int keyCode = event.getKeyCode();

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

    public  void setActivity(TimerActivity ShootingActivity) {

        mActivity = ShootingActivity;
    }



////Session Controls
////------------------------------------------------------------------------------------------------

    public void STimerNewSessionControl()
    // ShotTimer Session Controller for a NEW session as indexed by (currentSession - 1)
    //
    // Note: <currentsession> starts at 1 and ends at SessionCount -
    {
        final int  sessIndx = currentSession - 1;

        Log.d(TAG, "==STimerNewSessionControl() - entered for currentSession Index = "+sessIndx);

        if(sessIndx == 0) {
            if(IPSessionStartFrame[sessIndx] < 1) startNewSession();
        } else if(sessIndx > 0 && sessIndx < SessionsLimit) {
            if(IPSessionEndFrame[sessIndx - 1] < 1) {
                Log.d(TAG, " XXXXXXX STimerNewSessionControl() called too soon!");

                currentSession--;
                sessionIndx = currentSession - 1;

//                return;
            } else {
                startNewSession();
            }
        } else {
            // no more sessions, stop it and display results
            Log.d(TAG, "  XXX Unexpected XXX STimerNewSessionControl() called to end Activity!!!");

            currentSession = SessionsLimit + 1;
            sessionIndx    = SessionsLimit;

            stopShootingAll();
        }
    }


    public void startNewSession()
    // Manager of new ShotTimer Session -
    //
    //   We need to make sure that all measurements are reset and the clocks and frame indices are reset properly.
    //   We then fire off the timed activities.
    //
    //   Note: buttons are disabled during the countdown!
    {
        long         mtime = System.currentTimeMillis();
        final int iSession = currentSession - 1;
        int     dReadyTime = SessionDelay; // in sec; replaced by CountDown time for the 1st session.

        Log.d(TAG, "===startNewSession() enter at time = "+mtime+ "; currentSession Index | IPState = "+iSession+" | "+IPState);

//        splitTimeBar.setText("");

        if(iSession == 0) {
            Log.d(TAG, " ===startNewSession() - 1st Session countdown Time = "+CountDown);

            dReadyTime          = CountDown;
            final int  oldState = IPState;
            IPState             = S_COUNTDOWN;

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.ActivityStateChange(oldState, S_COUNTDOWN);
                }
            });

            ShowCountDown5(CountDown);
        } else if(IPSessionEndFrame[iSession-1] < 1) { // validate OP Status
            Log.d(TAG, " XXXXXXX startNewSession() - last Session not done!");
            return;
        }

        Timer      timerReadyTime = new Timer();
        final int  delayReadyTime = dReadyTime * 1000; // in msec;
        final int  oldState       = IPState;
        IPState                   = S_RANDOM;

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.ActivityStateChange(oldState, S_RANDOM);
            }
        });

        IPIgnoreTouch  = true;
        timerReadyTime.schedule( new TimerTask()
        {
            //   1. Play the CountDown clock and wait for 'delayReadyTime' before firing the Ready signal
            //   2. Draw screen shade and wait for the random amount of time before removing the screen and firing the Start Shooting buzzer
            //   3. Start the session timer for timing calculation
            //
            //      Detail timing should be kept by the Start() function

            public void run() {

//                mActivity.sdpool.play(rdSoundID, 0.99f, 0.99f, 1, 0, 0.99f); // 'shooters ready'

                 int   tDelay;
                 Timer timeri = new Timer();

                 if(RandomModeOn) {
                     Random rand = new Random();
                     int rn2to5 = rand.nextInt(7) + 3; // 3 to 10 random #

                     tDelay = rn2to5 * 1000; // in msec; account for the Voice Command length
                 } else {
                     tDelay = 10;
                 }

                 Log.d(TAG, "  ===startNewSession: delTime | RandTime = " + delayReadyTime + " | "+tDelay);

                 timeri.schedule(new TimerTask() {

                            public void run() {

                      mActivity.sdpool.play(mActivity.bzSoundID, 0.99f, 0.99f, 1, 0, 0.99f); // play 'shooting buzzer' sound

                      final int  oldState    = IPState;
                      final long IPTimerZero = System.currentTimeMillis();
                      final int  IPFrameZero = mIPFrame;
                      IPSessionStartTime[iSession]  = IPTimerZero;
                      IPSessionStartFrame[iSession] = IPFrameZero;
                      IPSessionHits[iSession]       = 0;
                      IPIgnoreTouch                 = false;
                      IPinSessionStoppng            = false;
                      IPState                       = S_DETECT;

                      mActivity.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                           mActivity.ActivityStateChange(oldState, S_DETECT);

                                // change title to Ready
                                statusBar.setText(R.string.ready);
                            }
                                                 });
	Log.d(TAG, " StartTimerGame() setting IPState to Detect for session | IPFrameZero | IPTimerZero = "+iSession +" | "+IPFrameZero+" | "+IPTimerZero);
                  }

                }, tDelay);
            }
        }, delayReadyTime);

        if(iSession > 0) {

            ShowCountDown5f(SessionDelay);

        }
    } // startNewSession()


    public void stopSession()
    //  ShotTimer Session Break Handler. This function determines and executes what happens next
    //  when the current session is ended.

    {
        if(IPinSessionStoppng) return;

        IPinSessionStoppng = true;
        final int iSession = currentSession - 1;

        if(iSession >= SessionsLimit - 1) {
    Log.d(TAG, "XXXXX stopSession( ) called for wrong Session: Session Index | SessionsLimit | IPState = "+iSession+" | "+SessionsLimit +" | "+IPState);

            return;
        }

        Log.d(TAG, "===stopSession( ) - entered. Session Index | IPState = "+iSession+" | "+IPState);

        if(IPState != S_WAIT) { //====//
            // already in process
            Log.d(TAG, "XXXXX stopSession( ) in wrong state (non-S_WAIT) - skipping process!");
            return;
        }

        Log.d(TAG, "   ===stopSession( ) - last session Indx, Frame rate = "+iSession+", "+IPSessionFmRate[iSession]);

        Timer timerNextRound = new Timer();
        timerNextRound.schedule(new TimerTask() {
            public void run() {

                currentSession++;
                sessionIndx = currentSession - 1;

                Log.d(TAG, "  ===stopSession( ) - ready to call STimerNewSessionControl(); currentSession Index now " + (currentSession - 1));

                STimerNewSessionControl();

            }
        }, 500); // delay to avoid sound overlap with the last shot

    }

    public void ShowCountDown5(int tCountDown) {

        final int        pWidth  = cameraParameters.getPreviewSize().width;
        final int        pHeight = cameraParameters.getPreviewSize().height;
        boolean        showTimer = true; // false;
        float              scale = getResources().getDisplayMetrics().density;
        final String[]        hs = {" ", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        float               offx = 0.0f;  // spWidth = pWidth*scale;
        float               offy = 0.0f;  // spHeight = pHeight*scale;


        if ( IPCountOn) return;

        IPCountOn  = true;
////        mActivity.sdpool.play(sbSoundID, 0.99f, 0.99f, 1, 0, 0.99f); // 'stand-by'

        if(showTimer) {
            Matrix matrix = new Matrix();
            matrix.preRotate(90);

            final  Bitmap          mBitmap = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
            final  Bitmap         finalImg = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
            final  int                tlen = (tCountDown+1) * 1000 -1;
            final  int               foffx = (int)offx;
            final  int               foffy = (int)offy;
            final  Canvas           canvas = new Canvas(finalImg);
            final  Paint               cp= new Paint();
            final  TimerActivity mActivity = (TimerActivity) getContext();

            cp.setColor(Color.RED);
            cp.setTextSize(160*scale);
            cp.setStrokeWidth(13.5f);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CountDownTimer tstart = new CountDownTimer(tlen, 1000) {

                        public void onTick(long millisUntilFinished) {
                            final int i = (int) millisUntilFinished / 1000;

                            canvas.drawColor(Color.WHITE);

                            if(i <= 9) {
                                Rect bounds = new Rect();
                                cp.getTextBounds(hs[i], 0, 1, bounds);

                                canvas.drawText(hs[i], 100 + foffy, pWidth - 110 + foffx, cp);
                                // need to move to the right a little - 40 pix?

//   Log.d("===ShowCountDown5( )","displaying countdown clock count = "+i+" | pWidth, pHeight, scale = "+pWidth +", "+pHeight+", "+tScale);
//   Log.d("  ===ShowCountDown5( )","displaying countdown : foffy, foffx, (100 + foffy),  (pWidth - 110 + foffx) = "+foffy+", "+foffx+", "+(100 + foffy) +", "+ (pWidth - 110 + foffx));

                            }
                            mActivity.drawImage(finalImg);
                        }

                        public void onFinish()
                        {
                            finalImg.recycle();
                            mActivity.drawImage(mBitmap);
                            IPCountOn = false;

                            if( RandomModeOn && (!GetReadyMuteOn) ) {
                                mActivity.sdpool.play(mActivity.rdSoundID, 0.99f, 0.99f, 1, 0, 0.99f); // 'shooters ready'
                            }

                            ///// Change title to StandBy
                            statusBar.setText(R.string.session_time_limit);

                        }
                    }.start();
                }
            });
        }
    }

    public void ShowCountDown5f(int tCountDown) {
        // for followup sessions

        final int    pWidth  = cameraParameters.getPreviewSize().width;
        final int    pHeight = cameraParameters.getPreviewSize().height;
        boolean    showTimer = true; // false;
        float          scale = getResources().getDisplayMetrics().density;
        final String[]    hs = {" ", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        float           offx = 0.0f;  // spWidth = pWidth*scale;
        float           offy = 0.0f;  // spHeight = pHeight*scale;
        final Paint       cp = new Paint();
        cp.setColor(Color.RED);
        cp.setTextSize(160*scale);
        cp.setStrokeWidth(13.5f);

        if ( IPCountOn == true) return;

        IPCountOn  = true;
////        mActivity.sdpool.play(sbSoundID, 0.99f, 0.99f, 1, 0, 0.99f); // 'stand-by'

        if(showTimer) {
            Matrix matrix = new Matrix();
            matrix.preRotate(90);

            final  Bitmap          mBitmap = Bitmap.createBitmap(bgImage.getWidth(), bgImage.getHeight(), bgImage.getConfig());
            final  Bitmap         finalImg = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
            final  int                tlen = (tCountDown+1) * 1000 -1;
            final  int               foffx = (int)offx;
            final  int               foffy = (int)offy;
            final  float            tScale = scale;
            final  Canvas           canvas = new Canvas(finalImg);
            final  Paint               tCp = cp;
            final  TimerActivity mActivity = (TimerActivity) getContext();

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusBar.setText(R.string.reload);

                    CountDownTimer tstart = new CountDownTimer(tlen, 1000) {

                        public void onTick(long millisUntilFinished) {
                            final int i = (int) millisUntilFinished / 1000;

                            canvas.drawColor(Color.WHITE);

                            if(i <= 9) {
                                Rect bounds = new Rect();
                                tCp.getTextBounds(hs[i], 0, 1, bounds);
                                int height = bounds.height();
                                int width  = bounds.width();

                                canvas.drawText(hs[i], 100 + foffy, pWidth - 110 + foffx, cp);
                                // need to move to the right a little - 40 pix?

//   Log.d("===ShowCountDown5( )","displaying countdown clock count = "+i+" | pWidth, pHeight, scale = "+pWidth +", "+pHeight+", "+tScale);
//   Log.d("  ===ShowCountDown5( )","displaying countdown : foffy, foffx, (100 + foffy),  (pWidth - 110 + foffx) = "+foffy+", "+foffx+", "+(100 + foffy) +", "+ (pWidth - 110 + foffx));

                            }
                            mActivity.drawImage(finalImg);
                        }

                        public void onFinish()
                        {
                            finalImg.recycle();
                            mActivity.drawImage(mBitmap);
                            IPCountOn = false;

                            if( RandomModeOn && (!GetReadyMuteOn) ) {
                                mActivity.sdpool.play(mActivity.rdSoundID, 0.99f, 0.99f, 1, 0, 0.99f); // 'shooters ready'
                            }

                            ///// Change title to StandBy
                            statusBar.setText("Standby");

                        }
                    }.start();
                }
            });
        }
    }

////-------------------------------------------------------------------------------------------------

    public native int    GetThreshold(int i1, int i2, int i3);
    public native int    GetThresholdLow(int i1, int i2, int i3);
    public native int    GetIPBK(int i1, int i2, int i3);
}



