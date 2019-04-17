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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class TestActivity extends Activity {
    private    Camera                  mCamera = null;
    private    TestCameraView      mCameraView = null;
    private    Button               mainButton;
    private    Button              trainButton;
    private    Button               homeButton;
    private    Button              zPlusButton;
    private    Button             zMinusButton;
    private    boolean         cameraException = false;
    private    boolean        versionException = false;
    private    boolean             KBConnected = false;
    private    int                     IPState;
    private    int                   shotLimit;
    private    ImageView                 image;
    private    String               deviceName = "";

    //-----------------------------
    private static final int                C_WAIT = -2;  // calibration wait for start,
    private static final int               C_CALIB = -1;  // calibration in progress,
    private static final int            C_PARAMCAL = 0;   // just starting IP,
    private static final int              C_DETECT = 1;   // in detection,
    private static final int            C_POSTPROC = 2;   // post-detection processing
    private static final int              C_REVIEW = 3;   // in review
    //-----------------------------

    public     int            currentZoomLevel = 0;
    public     int                maxZoomLevel = 0;
    private    long       mainBtnLastClickTime;
    private    long       homeBtnLastClickTime;
    private    long      trainBtnLastClickTime;
    private    long      zPlusBtnLastClickTime;
    private    long     zMinusBtnLastClickTime;
//  private    Button               helpButton;

    private    Camera.Parameters    mCameraParameters;

    public  SharedPreferences        mpreferences;
    public  SharedPreferences.Editor       editor;

    public AudioAttributes spAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

    public SoundPool sdpool = new SoundPool.Builder()
            .setAudioAttributes(spAttributes)
            .setMaxStreams(2)
            .build();

    public  AudioManager            mAudioManager;

    public  int                     ssSoundID;
    public  int                     sbSoundID;
    public  int                     rdSoundID;
    public  int                     bzSoundID;
    public  int                     SpeakerVol = 7;

    private static final String     TAG = "=TestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate() entered.");

        final EditText hideKB = (EditText)findViewById(R.id.kbinput);

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
        shotLimit     = intent.getIntExtra("int", -1);
        deviceName    = intent.getStringExtra("string");

        Log.d(TAG, "onCreate() retrieved deviceName = " + deviceName);

        try {
            mCamera = Camera.open(0);//you can use open(int) to use different cameras

        } catch (Exception e){
            Log.e(TAG, "onCreate() camera.open() exception: " + e.getMessage());
            cameraException = true;
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            versionException = true;
        }

        FrameLayout camera_view;
        hideKB.setVisibility(View.INVISIBLE);

        if(mCamera != null && !versionException) {
            mCameraView = new TestCameraView(this, mCamera);

            mCameraView.setActivity(this);
            camera_view = (FrameLayout)findViewById(R.id.camera_view);

            camera_view.addView(mCameraView);//add the SurfaceView to the layout
            mCameraView.shotLimit = shotLimit;

        } else {
            Log.d(TAG, "!!!!onCreate()  mCamera is null!");
            return;
        }

        IPState    = C_WAIT;
        mainButton = (Button) findViewById(R.id.button);
        mainButton.setText(R.string.WordSTART);

        homeButton    = (Button) findViewById(R.id.homeButton);
//        helpButton  = (Button) findViewById(R.id.helpButton);
        trainButton   = (Button) findViewById(R.id.trainButton);
        zPlusButton   = (Button) findViewById(R.id.buttonZPlus);
        zMinusButton  = (Button) findViewById(R.id.buttonZMinus);

        zMinusButton.setBackgroundColor(Color.GRAY);
        zMinusButton.setAlpha(0.73f);

        if(!cameraException) {
            image = (ImageView) findViewById(R.id.imageView1);
            final FrameLayout target = camera_view;
            target.post(new Runnable()

            {

                @Override
                public void run() {
                    int width = target.getWidth();
                    int height = target.getHeight();
                    int x = target.getLeft();
                    int y = target.getTop();

                    image.getLayoutParams().width = width;
                    image.getLayoutParams().height = height;
                    image.setX(x);
                    image.setY(y);

                    TextView statusBar = (TextView) findViewById(R.id.StatusBar);

                    if (statusBar == null) {
                        Log.d(TAG, "XXXXXX unexpected - statusBar is null");
                    } else {

                        statusBar.setText("Zoom in on target; press Start");
                        statusBar.setAlpha(0.73f);
                    }
                    mCameraView.statusBar = statusBar;

                }

            });

            mainBtnLastClickTime   = SystemClock.elapsedRealtime();
            homeBtnLastClickTime   = SystemClock.elapsedRealtime();
            trainBtnLastClickTime  = SystemClock.elapsedRealtime();
            zPlusBtnLastClickTime  = SystemClock.elapsedRealtime();
            zMinusBtnLastClickTime = SystemClock.elapsedRealtime();

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

//            zoomControls = (ZoomControls) findViewById(R.id.CAMERA_ZOOM_CONTROLS);

            zPlusButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mCamera != null && IPState == C_WAIT) {
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
                            }

                            mCameraParameters.setZoom(currentZoomLevel);
                            mCamera.setParameters(mCameraParameters);
                            zMinusButton.setBackgroundColor(getResources().getColor(R.color.colorGreenButton));
                        } else {
                            zPlusButton.setBackgroundColor(Color.GRAY);
                        }

                        zPlusButton .setAlpha(0.73f);
                        zMinusButton.setAlpha(0.73f);
                    }
                }
            });

            zMinusButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mCamera != null && IPState == C_WAIT && currentZoomLevel > 0) {
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
                        }

                        mCameraParameters.setZoom(currentZoomLevel);
                        mCamera.setParameters(mCameraParameters);
                        zPlusButton.setBackgroundColor(getResources().getColor(R.color.colorGreenButton));
//                    zMinusButton.setBackgroundColor(getResources().getColor(R.color.colorGreenButton));
                    } else {
                        zMinusButton.setBackgroundColor(Color.GRAY);
                    }

                    zPlusButton .setAlpha(0.73f);
                    zMinusButton.setAlpha(0.73f);
                }
            });



            mpreferences = getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
            editor = mpreferences.edit();
            SpeakerVol = mpreferences.getInt("SpeakerVolume", SpeakerVol);

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

    // @OnClick(R.id.IdForAppCompatButton)
    public void pressButton() {

        if(cameraException) return;

        // to start it
        if(IPState == C_WAIT) {
//            zoomControls.setVisibility(View.GONE);
            Log.d(TAG, "--- pressButton() removing Zoom Buttons...");
            mainButton  .setText(R.string.WordWAIT);
            mainButton  .setClickable(false);
            mainButton  .setAlpha(0.73f);
            zPlusButton .setVisibility(View.GONE);
            zMinusButton.setVisibility(View.GONE);
            mCameraView .Start();
        }

        // to stop it
        else if(IPState == C_DETECT)
        {
            Log.d(TAG, "--- pressButton() to stop on-going activity...");
            mCameraView.stopShooting(1);
            mainButton .setBackgroundResource(R.drawable.button);
            mainButton .setText(R.string.TryAgain);
            mainButton .setAlpha(0.73f);
        }

        else if(IPState == C_POSTPROC)
        {
//            zoomControls.setVisibility(View.VISIBLE);
            Log.d(TAG, "--- pressButton() to reset; display Zoom Buttons.");
            mCameraView .reset();
            mainButton  .setText(R.string.WordSTART);    // setText("START");
            mainButton  .setAlpha(0.73f);
            zPlusButton .setVisibility(View.VISIBLE);
            zMinusButton.setVisibility(View.VISIBLE);

            IPState = C_WAIT;
        }

    }

    public  void drawImage(final Bitmap bmpimage)
    {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                image.setImageBitmap(bmpimage);
            }
        });

    }



    //this function allows the camera view to communicate with its parent(this class) by telling it what state it's in
    public void stateChange(int state)
    {
        IPState = state;

        if(state == C_DETECT)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainButton.  setText(R.string.WordSTOP);
                    mainButton.  setClickable(true);
                    zPlusButton. setVisibility(View.GONE);
                    zMinusButton.setVisibility(View.GONE);

                    if (KBConnected) {
                        mainButton.setBackgroundResource(R.drawable.stopbuttonbt);
                    } else {
                        mainButton.setBackgroundResource(R.drawable.stopbutton);
                    }
                    mainButton.setAlpha(0.73f);
                }
            });
        }

        else if(state == C_POSTPROC) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (KBConnected) {
                        mainButton.setBackgroundResource(R.drawable.buttonbt);
                    } else {
                        mainButton.setBackgroundResource(R.drawable.button);
                    }
                    mainButton.setText(R.string.TryAgain);
                    mainButton.setAlpha(0.73f);
                }
            });
        }
    }


    public void pressHelp() {

        Intent intent = new Intent(this, HelpScreenActivity.class);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mainButton  .setOnClickListener(null);
        homeButton  .setOnClickListener(null);
        trainButton .setOnClickListener(null);
        zPlusButton .setOnClickListener(null);
        zMinusButton.setOnClickListener(null);

        startActivity(intent);
        finish();
    }

    public void pressHome() {

        Intent intent = new Intent(this, MainScreenActivity.class);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mainButton  .setOnClickListener(null);
        homeButton  .setOnClickListener(null);
        trainButton .setOnClickListener(null);
        zPlusButton .setOnClickListener(null);
        zMinusButton.setOnClickListener(null);
        startActivity(intent);
        finish();
    }

    public void pressTrain() {

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = new Intent(this, TrainingSelectActivity.class);
        mainButton .setOnClickListener(null);
        homeButton .setOnClickListener(null);
        trainButton.setOnClickListener(null);
        startActivity(intent);
        finish();
    }

    public void remotePressed() {
        Log.d(TAG, "remotePressed() entered... will be treated as 'pressButton()'. IPState = "+IPState);

////        pressButton(); // OnClick already had the code!!
    }

    public void changeStatusBar(String text)
    {
        TextView statusBar = (TextView) findViewById(R.id.StatusBar);
        statusBar.setText(text);

        if(text == "This may cause issues")
        {
            statusBar.setBackgroundColor(Color.parseColor("#C40202"));
        }
        else
        {
            statusBar.setBackgroundColor(Color.parseColor("#92c83d"));
        }
        statusBar.setAlpha(0.73f);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(cameraException) return;

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = new Intent(this, TrainingSelectActivity.class);
        mainButton.setOnClickListener(null);
        homeButton.setOnClickListener(null);
        trainButton.setOnClickListener(null);
        startActivity(intent);
        finish();

        Utils.clearTextLineCache();
    }


    @Override
    protected void onResume(){

        super.onResume();

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
    public void onConfigurationChanged(Configuration newConfig)
    {
        KBConnected = mpreferences.getBoolean("keyboardconnected", false);

        if(newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            // A hardware keyboard is being connected
            Log.d(TAG, "onConfigurationChanged() - KB connected!");
            KBConnected = true;
        }
        else if(newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            // A hardware keyboard is being disconnected
            Log.d(TAG, "onConfigurationChanged() - KB disconnected!");
            KBConnected = false;
        }

        editor.putBoolean("keyboardconnected", KBConnected);
        editor.commit();
        stateChange(IPState);

    }
}


