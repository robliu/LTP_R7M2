package com.get.gsappalpha1;

/**
 *  ===== This work is copyrighted! Do not copy or reuse without Guidance permission! ======
 *
 *    For detail, contact www.guidance-edu.com
 *
 *
 *     Created by Rob on 4/11/2017.
 *
 *  Copy Rights 2017, Guidance Education Technologies, All Rights Reserved.
 *
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import static com.get.gsappalpha1.R.id.CountdownLengthSlider;
import static com.get.gsappalpha1.R.id.SessionLengthSlider;


public class TimerActivity extends Activity {
    private Camera                  mCamera = null;
    private TimerCameraView     mCameraView = null;
    private Button               mainButton;
    private Button              trainButton;
    private Button               homeButton;
    private Button           settingsButton;
    private Button              zPlusButton;
    private Button             zMinusButton;
    private String               deviceName = "";
    private ImageView                 image;
    private TextView           splitTimeBar;
    private int                  ActIPState;      // state of the TimerActivity - should match IPState in IPCameraView,

    public  int            currentZoomLevel = 0;
    public  int                maxZoomLevel = 0;
    public  int           DeviceTimerRating;
    private long       mainBtnLastClickTime;
    private long       homeBtnLastClickTime;
    private long      trainBtnLastClickTime;
    private long    settingBtnLastClickTime;
    private long      zPlusBtnLastClickTime;
    private long     zMinusBtnLastClickTime;

    private static final int         S_WAIT = -2; //               -2: calibration wait until completion,
    private static final int        S_CALIB = -1; //               -1: calibration in progress,
    private static final int     S_PARAMCAL = 0;  //                0: pre-detection; detections param calculation,
    private static final int    S_COUNTDOWN = 1;  //                1: countdown in progress,
    private static final int       S_RANDOM = 2;  //                2: random pause (after READY call) in progress,
    private static final int       S_DETECT = 3;  //                3. shooting started; detection in progress,
    private static final int     S_POSTPROC = 4;  //                4: break period (no detection) in between sessions,
    private static final int       S_REVIEW = 5;  //                5: review,
    private static final int        S_DONE  = 6;  //                6: done.
    private static final int        S_PAUSE = 7;

    public  boolean                    ExitCamera;            // kill camera switch for onPause() / onDestroy()
    private boolean               cameraException = false;
    private boolean              versionException = false;
    private boolean                   KBConnected = false;
    public  SharedPreferences        mpreferences;
    public  SharedPreferences.Editor       editor;

    private    Camera.Parameters    mCameraParameters;

    public boolean GetReadyMuteOn = false;   // to mute the "Get Ready" sound
    public boolean RandomModeOn   = true;    // to use the Random Timer (instead of Fixed)

    public int SessionsLimit      = 2;       // # of sessions to be performed
    public int ShotsLimit         = 3;       // max number shots each session can have. Session ends when this number is reached or time runs out
    public int SessionDelay       = 5;       // time between sessions. Starts counting when session ends and stops when the next session begins.
                                             //      Give the user time to get into position.

    public int SessionLength      = 15;      // max time in each session
    public int CountDown          = 5;       // time before the first session starts. Give the user time to get into position etc.

    private long  resumeTime      = 0L;      // Used for ignoring leftover touch events between activity sessions

    public AudioAttributes spAttributes = new AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_GAME)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .build();

    public SoundPool            sdpool = new SoundPool.Builder()
                                        .setAudioAttributes(spAttributes)
                                        .setMaxStreams(2)
                                        .build();

    public  AudioManager            mAudioManager;

    public  int                     ssSoundID;
    public  int                     sbSoundID;
    public  int                     rdSoundID;
    public  int                     bzSoundID;
    public  int                     SpeakerVol = 7;

    private static final String TAG   = "~~~TimerActivity";
    private static final String TAG2  = "  ~~~TimerActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindow().getDecorView().setSystemUiVisibility(
                // Set the IMMERSIVE flag.
                // Set the content to appear under the system bars so that the content
                // doesn't resize when the system bars hide and show.
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        Intent intent = getIntent();
        deviceName    = intent.getStringExtra("string");
        ExitCamera    = false;

        Log.d(TAG, "onCreate() retrieved deviceName = " + deviceName);

        try {
            mCamera = Camera.open(0);
        } catch (Exception e){
            Log.e(TAG, "camera open(0) exception: " + e.getMessage());
            cameraException = true;

            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            versionException = true;
        }

        mpreferences       = getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        editor             = mpreferences.edit();

        SessionsLimit      = mpreferences.getInt("TimerSessionsLimit", SessionsLimit);
        ShotsLimit         = mpreferences.getInt("TimerShotsLimit", ShotsLimit);
        SessionDelay       = mpreferences.getInt("TimerSessionDelay", SessionDelay);
        SessionLength      = mpreferences.getInt("TimerSessionLength", SessionLength);
        CountDown          = mpreferences.getInt("TimerCountDown", CountDown);
        SpeakerVol         = mpreferences.getInt("SpeakerVolume", SpeakerVol);
        GetReadyMuteOn     = mpreferences.getBoolean("TimerReadyMuteOn", GetReadyMuteOn);
        RandomModeOn       = mpreferences.getBoolean("TimerRandomModeOn", RandomModeOn);
        DeviceTimerRating  = mpreferences.getInt("DeviceTimerRating", DeviceTimerRating);

        if(!cameraException) {
            FrameLayout camera_view;

            if (mCamera != null) {
                Log.d(TAG2, "onCreate() - mCamera was ready! Creating TimerCameraView now!");

                mCameraView = new TimerCameraView(this, mCamera); // create a SurfaceView to show camera data
                mCameraView.setActivity(this);
                camera_view = (FrameLayout) findViewById(R.id.camera_view);

                camera_view.addView(mCameraView);           // add the SurfaceView to the layout

                mCameraView.SessionsLimit     = SessionsLimit;
                mCameraView.ShotsLimit        = ShotsLimit;
                mCameraView.SessionDelay      = SessionDelay;
                mCameraView.SessionLength     = SessionLength;
                mCameraView.CountDown         = CountDown;
                mCameraView.GetReadyMuteOn    = GetReadyMuteOn;
                mCameraView.RandomModeOn      = RandomModeOn;
                mCameraView.DeviceTimerRating = DeviceTimerRating;
            } else {
                return;
            }

            ActIPState = S_WAIT; // 0;
            mainButton = (Button) findViewById(R.id.button);
            mainButton.setText(R.string.WordSTART);

            homeButton     = (Button) findViewById(R.id.homeButton);
            trainButton    = (Button) findViewById(R.id.trainButton);
            settingsButton = (Button) findViewById(R.id.settingsbutton);
            zPlusButton    = (Button) findViewById(R.id.buttonZPlus);
            zMinusButton   = (Button) findViewById(R.id.buttonZMinus);

            zMinusButton.setBackgroundColor(Color.GRAY);
            zMinusButton.setAlpha(0.73f);

            image          = (ImageView) findViewById(R.id.imageView1);
            splitTimeBar   = findViewById(R.id.SplitTimeRow);

            final FrameLayout target = camera_view; // (FrameLayout) findViewById(R.id.camera_view);

            target.post(new Runnable()

            {

                @Override
                public void run() {
                    int width  = target.getWidth();
                    int height = target.getHeight();
                    int x = target.getLeft();
                    int y = target.getTop();

                    image.getLayoutParams().width  = width;
                    image.getLayoutParams().height = height;
                    image.setX(x);
                    image.setY(y);

                    TextView statusBar = (TextView) findViewById(R.id.StatusBar);
                    if (statusBar == null) {
                        Log.d(TAG2, "XXXXXXXX statusBar not exist!!");
                    } else {
                        statusBar.setText(R.string.zoomMessage);
                        statusBar.setAlpha(0.73f);
                    }
                    mCameraView.statusBar    = statusBar;

                    TextView splitTimeBar = (TextView) findViewById(R.id.SplitTimeRow);
                    if (splitTimeBar == null) {
                        Log.d(TAG2, "XXXXXXXX splitTimeBar not exist!!");
                    } else {
                        splitTimeBar.setText("");
                        statusBar.setAlpha(0.73f);
                    }
                    mCameraView.splitTimeBar = splitTimeBar;
                }

            });

            mainBtnLastClickTime    = SystemClock.elapsedRealtime();
            homeBtnLastClickTime    = SystemClock.elapsedRealtime();
            trainBtnLastClickTime   = SystemClock.elapsedRealtime();
            settingBtnLastClickTime = SystemClock.elapsedRealtime();

            mainButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (SystemClock.elapsedRealtime() - mainBtnLastClickTime < 2000) {
                        return;
                    }
                    mainBtnLastClickTime = SystemClock.elapsedRealtime();
                    pressButton();
                }
            });

            homeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (SystemClock.elapsedRealtime() - homeBtnLastClickTime < 2000) {
                        return;
                    }
                    homeBtnLastClickTime = SystemClock.elapsedRealtime();
                    pressHome();
                }
            });

            trainButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (SystemClock.elapsedRealtime() - trainBtnLastClickTime < 2000) {
                        return;
                    }
                    trainBtnLastClickTime = SystemClock.elapsedRealtime();
                    pressTrain();
                }
            });

            settingsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (SystemClock.elapsedRealtime() - settingBtnLastClickTime < 2000) {
                        return;
                    }
                    settingBtnLastClickTime = SystemClock.elapsedRealtime();
                    pressSettings();
                }
            });

            zPlusButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                if (mCamera != null && ActIPState == S_WAIT) {
                    int step = 1;
//                    Log.d(TAG, " + zPlusButton clicked!");

                    mCameraParameters = mCamera.getParameters();
                    maxZoomLevel      = mCameraParameters.getMaxZoom();

                    if(currentZoomLevel < maxZoomLevel) {
                        if      (SystemClock.elapsedRealtime() - zPlusBtnLastClickTime < 750) step = 9;
                        else if (SystemClock.elapsedRealtime() - zPlusBtnLastClickTime < 800) step = 6;
                        else if (SystemClock.elapsedRealtime() - zPlusBtnLastClickTime < 850) step = 4;
                        else if (SystemClock.elapsedRealtime() - zPlusBtnLastClickTime < 900) step = 2;

                        zPlusBtnLastClickTime = SystemClock.elapsedRealtime();

                        currentZoomLevel += step;
                        if (currentZoomLevel >= maxZoomLevel) {
//                            Log.d(TAG, "   ++ zPlusButton topped!");

                            currentZoomLevel = maxZoomLevel;
                            zPlusButton.setBackgroundColor(Color.GRAY);
                        } else {
                            zPlusButton.setBackgroundColor(getResources().getColor(R.color.colorGreenButton));
                        }

                        mCameraParameters.setZoom(currentZoomLevel);
                        mCamera.setParameters(mCameraParameters);
                        zMinusButton.setBackgroundColor(getResources().getColor(R.color.colorGreenButton));

                        zPlusButton .setAlpha(0.73f);
                        zMinusButton.setAlpha(0.73f);
                    }
                }
                }
            });

            zMinusButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                if (mCamera != null && ActIPState == S_WAIT && currentZoomLevel > 0) {
                    int step = 1;
//                    Log.d(TAG, " - zMinusButton clicked!");

                    mCameraParameters = mCamera.getParameters();

                    if      (SystemClock.elapsedRealtime() - zMinusBtnLastClickTime < 750) step = 9;
                    else if (SystemClock.elapsedRealtime() - zMinusBtnLastClickTime < 800) step = 6;
                    else if (SystemClock.elapsedRealtime() - zMinusBtnLastClickTime < 850) step = 4;
                    else if (SystemClock.elapsedRealtime() - zMinusBtnLastClickTime < 900) step = 2;

                    zMinusBtnLastClickTime = SystemClock.elapsedRealtime();

                    currentZoomLevel-=step;
                    if(currentZoomLevel <= 0) {
//                        Log.d(TAG, "   -- zPlusButton bottomed!");

                        currentZoomLevel = 0;
                        zMinusButton.setBackgroundColor(Color.GRAY);
                    } else {
                        zMinusButton.setBackgroundColor(getResources().getColor(R.color.colorGreenButton));
                    }

                    mCameraParameters.setZoom(currentZoomLevel);
                    mCamera.setParameters(mCameraParameters);
                    zPlusButton.setBackgroundColor(getResources().getColor(R.color.colorGreenButton));

                    zPlusButton .setAlpha(0.73f);
                    zMinusButton.setAlpha(0.73f);
                }
                }
            });

            mAudioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.setSpeakerphoneOn(true);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SpeakerVol, 0);

            int  nativeSampleRate = Integer.parseInt(mAudioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
            if (nativeSampleRate == 48000) {
                Log.d(TAG, "  using 4.8KHz sound tracks!");

                ssSoundID = sdpool.load(this, R.raw.steelshot_48, 1);
                rdSoundID = sdpool.load(this, R.raw.shooterreadygs_48, 1);
                sbSoundID = sdpool.load(this, R.raw.standby_48, 1);
                bzSoundID = sdpool.load(this, R.raw.buzzer_48, 1);
            } else {
                ssSoundID = sdpool.load(this, R.raw.steelshot, 1);
                rdSoundID = sdpool.load(this, R.raw.shooterreadygs, 1);
                sbSoundID = sdpool.load(this, R.raw.standby, 1);
                bzSoundID = sdpool.load(this, R.raw.buzzer, 1);
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    public void pressButton() {
        //  ActIPState - state for TimerActivity; should match IPState in IPCameraView,
        //               -2: calibration wait until completion,
        //               -1: calibration in progress,
        //                0: pre-detection; detections param calculation,
        //                1: countdown in progress,
        //                2: random pause (after READY call) in progress,
        //                3. shooting started; detection in progress,
        //                4: break period (no detection) in between sessions,
        //                5: review,
        //                6: done.

        Log.d( TAG, "pressButton() entered...ActIPState = "+ ActIPState);
        if(cameraException) return;

        //to start session; (light cal)
        if(ActIPState == S_WAIT) {

//// Display Qualification Run message if needed
            final  int runLeft = 6 + DeviceTimerRating;

            Log.d(TAG2, "pressButton() triggering START ACTION! runLeft = "+runLeft);

            if( runLeft > 0 && runLeft < 100) {

                AlertDialog mAlertDialog = new AlertDialog.Builder(this).create();
                // AlertDialog mAlertDialog = new AlertDialog.Builder(mActivity.getApplicationContext()).create();

                if(mAlertDialog != null) {

                    mAlertDialog.setTitle("Device Requirements Warning"); // ("Session Alert");
                    String msg = "This activity requires high performance device. This device has "
                            +runLeft +" qualification test runs left.\n"
                            + "\n  Tap OK to continue, Quit to quit.";

                    mAlertDialog.setMessage( msg );
                    mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "\n  OK ",   //  BUTTON_NEUTRAL
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    dialog.dismiss();

                                    mainButton    .setText(R.string.WordWAIT);
                                    mainButton    .setClickable(false);
                                    mainButton    .setAlpha(0.73f);
                                    settingsButton.setVisibility(View.GONE);
                                    zPlusButton   .setVisibility(View.GONE);
                                    zMinusButton  .setVisibility(View.GONE);
                                    splitTimeBar  .setText("");

                                    mCameraView   .Start();
                                    return;
                                }
                            });

                    mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "\n Quit ",   //  BUTTON_NEUTRAL
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    dialog.dismiss();

                                    //// need to return to Activity Select screen




                                    /////////////////////////////////////////////
                                }
                            });
                    mAlertDialog.show();
                    return;
                }
            } else if( runLeft < 0) {  // already disqualified
                AlertDialog mAlertDialog = new AlertDialog.Builder(this).create();

                if (mAlertDialog != null) {
                    mAlertDialog.setTitle("Device Requirements Warning"); // ("Session Alert");
                    String msg = "This device doesn't meet the performance requirements to run this feature.\n"+
                            " Current activity will not be executed.";
                    mAlertDialog.setMessage(msg);
                    mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "\n       OK ",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    dialog.dismiss();

                                    //// need to return to Activity Select screen




                                    /////////////////////////////////////////////
                                }
                            });
                    mAlertDialog.show();

                }
                return;
            } else {  // already passed

                mainButton    .setText(R.string.WordWAIT);
                mainButton    .setClickable(false);
                mainButton    .setAlpha(0.73f);
                settingsButton.setVisibility(View.GONE);
                zPlusButton   .setVisibility(View.GONE);
                zMinusButton  .setVisibility(View.GONE);
                splitTimeBar  .setText("");

                mCameraView   .Start();
                return;
            }

        }
        else if(ActIPState == S_DETECT || ActIPState == S_POSTPROC) // ActIPState == S_COUNTDOWN || ActIPState == S_RANDOM
        { // 1, 2, 3, 4, 5
//            Log.d(TAG2, "pressButton() triggered STOP ACTIVITY request process...");
            if(mCameraView.IPinStopping) {
//                Log.d(TAG2, "  --pressButton() - STOPPING was already in progress!");
                return;
            }

//            Log.d(TAG2, "  --pressButton() - executing user initiated STOPPING request!");
            final int oldState = ActIPState;

            mCameraView.setSessionNum(SessionsLimit + 1);
            mCameraView.stopShootingAll();
            mainButton .setBackgroundResource(R.drawable.button);
            mainButton .setText(R.string.TrainAgain);
            mainButton .setAlpha(0.73f);

            ActIPState = S_REVIEW;
            ActivityStateChange(oldState, S_REVIEW);

//            Log.d(TAG, "  >>> pressButton() existing with ActIPState = "+ActIPState);

        }
        else if(ActIPState == S_REVIEW)
        {
            Log.d(TAG2, "pressButton() triggered RESET ACTION!");

            final int oldState = ActIPState;
            mCameraView   .reset();
            mainButton    .setText(R.string.TrainAgain);
            mainButton    .setAlpha(0.73f);
            settingsButton.setVisibility(View.VISIBLE);
            settingsButton.setAlpha(0.73f);
            ActIPState = S_WAIT;
            ActivityStateChange(oldState, S_WAIT);

//            Log.d(TAG, "  >>> pressButton() existing with ActIPState = "+ActIPState);

        }
        else {
            Log.d(TAG2, "pressButton() ACTION Ignored!");

        }
    }

    public  void drawImage(Bitmap bmap)
    {
        Log.d(TAG, "drawImage() entered!");

        image.setImageBitmap(bmap);

    }


    // this function allows the camera view to communicate with its parent(this class) by telling it what state it's in
    public void ActivityStateChange(int oldState, int state)
    {

        Log.d(TAG, "ActivityStateChange() entered; oState | ActIPState | newState = "+oldState+" | "+ActIPState +" | "+state);
////////        if(oldState == state) return;

        if(oldState == state) { // appearance change only
            if (state == S_WAIT) {
                if(KBConnected) {
                    mainButton.setBackgroundResource(R.drawable.buttonbt);
                } else {
                    mainButton.setBackgroundResource(R.drawable.button);
                }
            } else if(state == S_REVIEW) {
                if(KBConnected) {
                    mainButton.setBackgroundResource(R.drawable.buttonbt);
                } else {
                    mainButton.setBackgroundResource(R.drawable.button);
                }
            } else {
                // Nothing special
            }
        } else { // real change
            ActIPState = state;

            if (state == S_WAIT) {
                if(KBConnected) {
                    mainButton.setBackgroundResource(R.drawable.buttonbt);
                } else {
                    mainButton.setBackgroundResource(R.drawable.button);
                }
                zPlusButton .setVisibility(View.VISIBLE);
                zMinusButton.setVisibility(View.VISIBLE);
                mainButton  .setText(R.string.train);
                mainButton  .setAlpha(0.73f);

                final FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
                final ImageView image = (ImageView) findViewById(R.id.imageView1);

/*
                camera_view.post(new Runnable() // target.post(new Runnable()
                {

                    @Override
                    public void run() {
                        int width = camera_view.getWidth();
                        int height = camera_view.getHeight();
                        int x = camera_view.getLeft();
                        int y = camera_view.getTop();

                        image.getLayoutParams().width = width;
                        image.getLayoutParams().height = height;
                        image.setX(x);
                        image.setY(y);

                        TextView statusBar = (TextView) findViewById(R.id.StatusBar);

                        if (statusBar == null) {
//                            Log.d(TAG2, "XXXXXXXX statusBar not exist!!");
                        } else {
                            statusBar.setText(R.string.ready);
                        }
//                        statusBar  .setAlpha(0.73f);
                        mCameraView.statusBar = statusBar;
                    }

                });
*/

                //// Display Qualification Run message if needed
                final  int runLeft = 6 + DeviceTimerRating;
                if( DeviceTimerRating >= 100) {

                     camera_view.post(new Runnable() // target.post(new Runnable()
                     {

                          @Override
                          public void run() {
                               int width = camera_view.getWidth();
                               int height = camera_view.getHeight();
                               int x = camera_view.getLeft();
                               int y = camera_view.getTop();

                               image.getLayoutParams().width = width;
                               image.getLayoutParams().height = height;
                               image.setX(x);
                               image.setY(y);

                               TextView statusBar = (TextView) findViewById(R.id.StatusBar);

                               if (statusBar == null) {
//                            Log.d(TAG2, "XXXXXXXX statusBar not exist!!");
                               } else {
                                   statusBar.setText(R.string.ready);
                               }

//                        statusBar  .setAlpha(0.73f);
                               mCameraView.statusBar = statusBar;
                          }
                      });
                     return;

                } else if( runLeft > 0 && runLeft < 100) {

                    AlertDialog mAlertDialog = new AlertDialog.Builder(this).create();
                    // AlertDialog mAlertDialog = new AlertDialog.Builder(mActivity.getApplicationContext()).create();

                    if(mAlertDialog != null) {

                        mAlertDialog.setTitle("Device Requirements Warning"); // ("Session Alert");
                        String msg = "Activity requires high performance device. This device has "
                                     +runLeft +" qualification test runs left.\n"
                                    + "\n  Tap OK to continue, Quit to quit.";

                        mAlertDialog.setMessage( msg );
                        mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "\n  OK ",   //  BUTTON_NEUTRAL
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        dialog.dismiss();

                                        camera_view.post(new Runnable() // target.post(new Runnable()
                                        {

                                            @Override
                                            public void run() {
                                                int width = camera_view.getWidth();
                                                int height = camera_view.getHeight();
                                                int x = camera_view.getLeft();
                                                int y = camera_view.getTop();

                                                image.getLayoutParams().width = width;
                                                image.getLayoutParams().height = height;
                                                image.setX(x);
                                                image.setY(y);

                                                TextView statusBar = (TextView) findViewById(R.id.StatusBar);

                                                if (statusBar == null) {
//                            Log.d(TAG2, "XXXXXXXX statusBar not exist!!");
                                                } else {
                                                    statusBar.setText(R.string.ready);
                                                }
//                        statusBar  .setAlpha(0.73f);
                                                mCameraView.statusBar = statusBar;
                                            }

                                        });

                                    }
                                });

                        mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "\n Quit ",   //  BUTTON_NEUTRAL
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        dialog.dismiss();
                                    }
                                });
                        mAlertDialog.show();
                        return;
                    }
                } else {
                    AlertDialog mAlertDialog = new AlertDialog.Builder(this).create();

                    if (mAlertDialog != null) {
                        mAlertDialog.setTitle("Device Requirements Warning"); // ("Session Alert");
                        String msg = "This doesn't meet the performance requirements to run this feature.\n"+
                                " Current activity will not be executed.";
                        mAlertDialog.setMessage(msg);
                        mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "\n       OK ",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        dialog.dismiss();
                                    }
                                });
                        mAlertDialog.show();

                    }
                }


                return;
            } else if (state == S_CALIB) {
                mainButton  .setBackgroundResource(R.drawable.button);
                zPlusButton .setVisibility(View.GONE);
                zMinusButton.setVisibility(View.GONE);
                mainButton  .setText(R.string.WordWAIT);
                mainButton  .setAlpha(0.73f);

                final FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
                final ImageView image = (ImageView) findViewById(R.id.imageView1);
///            final FrameLayout      target = camera_view; // (FrameLayout) findViewById(R.id.camera_view);

                camera_view.post(new Runnable() // target.post(new Runnable()
                {

                    @Override
                    public void run() {
                        int width = camera_view.getWidth();
                        int height = camera_view.getHeight();
                        int x = camera_view.getLeft();
                        int y = camera_view.getTop();

                        image.getLayoutParams().width = width;
                        image.getLayoutParams().height = height;
                        image.setX(x);
                        image.setY(y);

                        TextView statusBar = (TextView) findViewById(R.id.StatusBar);

                        if (statusBar == null) {
//                            Log.d(TAG2, "XXXXXXXX statusBar not exist!!");
                        } else {
//                            Log.d(TAG, "ActivityStateChange(S_CALIB) setText to <Please wait>");
                            statusBar.setText("Please wait...");
                            statusBar.setAlpha(0.73f);
                        }
                        mCameraView.statusBar = statusBar;
                    }

                });

                return;
            } else if (state == S_PARAMCAL) {
                // No Action required!
                Log.d(TAG, "stateChange() no action required!");

                return;
            } else if (state == S_RANDOM) {
                mainButton.setText(R.string.WordWAIT);
                mainButton.setClickable(false);
                mainButton.setAlpha(0.73f);
                mainButton.setBackgroundResource(R.drawable.button);
                return;
            } else if (state == S_COUNTDOWN) {
                mainButton.setText(R.string.WordWAIT);
                mainButton.setClickable(false);
                mainButton.setAlpha(0.73f);
                mainButton.setBackgroundResource(R.drawable.button);

                final FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
                final ImageView image = (ImageView) findViewById(R.id.imageView1);

                camera_view.post(new Runnable() // target.post(new Runnable()
                {

                    @Override
                    public void run() {
                        int width = camera_view.getWidth();
                        int height = camera_view.getHeight();
                        int x = camera_view.getLeft();
                        int y = camera_view.getTop();

                        image.getLayoutParams().width = width;
                        image.getLayoutParams().height = height;
                        image.setX(x);
                        image.setY(y);

                        TextView statusBar = (TextView) findViewById(R.id.StatusBar);

                        if (statusBar == null) {
//                            Log.d(TAG2, "XXXXXXXX statusBar not exist!!");
                        } else {
                            statusBar.setText(R.string.standby);
                            statusBar.setAlpha(0.73f);
                        }
                        mCameraView.statusBar = statusBar;
                    }

                });

                return;
            } else if (state == S_DETECT) {
                mainButton.setText(R.string.WordSTOP);
                mainButton.setClickable(true);
                mainButton.setAlpha(0.73f);
                mainButton.setBackgroundResource(R.drawable.stopbutton);

                final FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
                final ImageView image = (ImageView) findViewById(R.id.imageView1);
///            final FrameLayout      target = camera_view; // (FrameLayout) findViewById(R.id.camera_view);

                camera_view.post(new Runnable() // target.post(new Runnable()
                {

                    @Override
                    public void run() {
                        int width = camera_view.getWidth();
                        int height = camera_view.getHeight();
                        int x = camera_view.getLeft();
                        int y = camera_view.getTop();

                        image.getLayoutParams().width = width;
                        image.getLayoutParams().height = height;
                        image.setX(x);
                        image.setY(y);

                        TextView statusBar = (TextView) findViewById(R.id.StatusBar);

                        if (statusBar == null) {
//                            Log.d(TAG2, "XXXXXXXX statusBar not exist!!");
                        } else {
                            statusBar.setText(R.string.ready); // statusBar.setText("Reload");
                            statusBar.setAlpha(0.73f);
                        }
                        mCameraView.statusBar = statusBar;
                    }

                });

                return;
            } else if (state == S_POSTPROC) {
                if (oldState == S_DETECT) {
//                    Log.d(TAG, "stateChange() to " + ActIPState);
                } else {
//                    Log.d(TAG, "stateChange() no action required!");
                }
                return;
            } else if (state == S_REVIEW) {
//                mainButton.setBackgroundResource(R.drawable.button);

                if(KBConnected) {
                    mainButton.setBackgroundResource(R.drawable.buttonbt);
                } else {
                    mainButton.setBackgroundResource(R.drawable.button);
                }
                mainButton.setText(R.string.TrainAgain);
                mainButton.setAlpha(0.73f);

                final FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
                final ImageView         image = (ImageView) findViewById(R.id.imageView1);

                camera_view.post(new Runnable() // target.post(new Runnable()
                {

                    @Override
                    public void run() {
                        int width = camera_view.getWidth();
                        int height = camera_view.getHeight();
                        int x = camera_view.getLeft();
                        int y = camera_view.getTop();

                        image.getLayoutParams().width = width;
                        image.getLayoutParams().height = height;
                        image.setX(x);
                        image.setY(y);

                        TextView statusBar = (TextView) findViewById(R.id.StatusBar);

                        if (statusBar == null) {
                            Log.d(TAG2, "XXXXXXXX statusBar not exist!!");
                        }

                    }
                });
                return;
            } else {
                Log.d(TAG, "stateChange() undefined case!");
                return;
            }
        }
    }


    public void pressSettings()
    {
//        mCameraView.killTimer = true;
        Log.d(TAG2, "  pressSettings() entered..");

        Intent myintent = new Intent(this, TimerSettingsActivity.class);

//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mainButton.setOnClickListener(null);
        homeButton.setOnClickListener(null);
        trainButton.setOnClickListener(null);
        settingsButton.setOnClickListener(null);

        final SeekBar sk1 = findViewById(R.id.SessionNumSlider);
        if(sk1 != null) sk1.setProgress(SessionsLimit);

        final SeekBar sk2 = findViewById(R.id.SessionShotSlider);
        if(sk2 != null) sk2.setProgress(ShotsLimit);

        final SeekBar sk3 = findViewById(R.id.SessionDelaySlider);
        if(sk3 != null) sk3.setProgress(SessionDelay);

        final SeekBar sk4 = findViewById(CountdownLengthSlider);
        if(sk4 != null) sk4.setProgress(CountDown);

        final SeekBar sk5 = findViewById(SessionLengthSlider);
        if(sk5 != null) sk5.setProgress(SessionLength);

        final Switch switch_btn = findViewById(R.id.MakeReadySwitch);
        if(switch_btn != null) {

            Log.d(TAG2, "  switch_btn value = " + Boolean.toString(GetReadyMuteOn));

            if (GetReadyMuteOn) {
                Log.d(TAG2, "  setting switch_btn to Unchecked! ");

                switch_btn.setChecked(false);
            }
            else {
                Log.d(TAG2, "  setting switch_btn to Checked! ");

                switch_btn.setChecked(true);
            }

        }

        startActivity(myintent);
        finish();
    }


    public void pressHome() {
//        mCameraView.killTimer = true;
        Log.d(TAG, "  pressHome() entered..");
        Intent intent = new Intent(this, MainScreenActivity.class);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mainButton    .setOnClickListener(null);
        homeButton    .setOnClickListener(null);
        trainButton   .setOnClickListener(null);
        settingsButton.setOnClickListener(null);
        splitTimeBar  .setText("");
        startActivity(intent);
        finish();
        return;
    }

    public void pressTrain() {

//        mCameraView.killTimer = true;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = new Intent(this, TrainingSelectActivity.class);
        mainButton    .setOnClickListener(null);
        homeButton    .setOnClickListener(null);
        trainButton   .setOnClickListener(null);
        splitTimeBar  .setText("");
        startActivity(intent);
        finish();
        return;
    }

    public void changeStatusBar(String text)
    {
        TextView statusBar = (TextView) findViewById(R.id.StatusBar);
        statusBar.setText(text);
        statusBar.setAlpha(0.73f);
        return;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, " onPause() entered..");

        Utils.clearTextLineCache();
        super.onPause();
        return;
    }


    @Override
    protected void onResume(){
        Log.d(TAG, " onResume() entered.");
        super.onResume();

        setVisible(true);

        if(cameraException) {
            AlertDialog mAlertDialog = new AlertDialog.Builder(this).create();

            if (mAlertDialog != null) {
                mAlertDialog.setTitle("Permission Warning"); // ("Session Alert");
                String msg = "This App won't work without Camera Permission!";
                mAlertDialog.setMessage(msg);
                mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "\n       OK ",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();
                            }
                        });
                mAlertDialog.show();

            }
        } else if (versionException) {
            AlertDialog mAlertDialog = new AlertDialog.Builder(this).create();

            if (mAlertDialog != null) {
                mAlertDialog.setTitle("Version Incompatibility"); // ("Session Alert");
                String msg = "This App requires Android version 6.0 Marshmallow to work properly.";
                mAlertDialog.setMessage(msg);
                mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "\n       OK ",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();
                            }
                        });
                mAlertDialog.show();

            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getEventTime() < resumeTime) {
            Log.d(TAG, "Discarding all touch events earlier than onResume()!");
            return true;
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int  action = event.getAction();
        int keyCode = event.getKeyCode();

        if(mAudioManager == null) {
            Log.d(TAG, "  dispatchKeyEvent() - found Null mAudioManager!");
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            if (mAudioManager == null) {
                Log.d(TAG, "  dispatchKeyEvent() - mAudioManager is NULL!");

                return super.dispatchKeyEvent(event);
            }
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    SpeakerVol++;
                    if(SpeakerVol <= mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SpeakerVol, AudioManager.FLAG_SHOW_UI);

                        Log.d(TAG, "  == SoundPool volume forced to: " + mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) +
                                " | VolumeFixed = " + mAudioManager.isVolumeFixed());

                        editor.putInt("SpeakerVolume", SpeakerVol);
                        editor.commit();
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    SpeakerVol--;
                    if(SpeakerVol >= 0) {
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SpeakerVol, AudioManager.FLAG_SHOW_UI);

                        Log.d(TAG, "  == SoundPool volume forced to: " + mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) +
                                " | VolumeFixed = " + mAudioManager.isVolumeFixed());

                        editor.putInt("SpeakerVolume", SpeakerVol);
                        editor.commit();
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public void onDestroy() {
        ExitCamera = true;

//        Log.d(TAG, "onDestroy() called. ExitCamera = "+ ExitCamera);
        if(mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        super.onDestroy();
        return;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        if(newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            //A hardware keyboard is being connected
            Log.d(TAG, "onConfigurationChanged() - KB connected!");
        }
        else if(newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            //A hardware keyboard is being disconnected
            Log.d(TAG, "onConfigurationChanged() - KB disconnected!");
        }

        editor.putBoolean("keyboardconnected", KBConnected);
        editor.commit();
        ActivityStateChange(ActIPState, ActIPState);

//        updateKBIcon(KBConnected);

    }
}


