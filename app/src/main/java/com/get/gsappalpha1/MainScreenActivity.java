package com.get.gsappalpha1;

/**
 *  ===== This work is copy ighted! Do not copy or reuse without Guidance permission! ======
 *
 *    For detail, contact www.guidance-edu.com
 *
 *
 *     Created by Rob on 12/23/2016.
 *
 *  Copy Rights 2016, 2017, Guidance Education Technologies, All Rights Reserved.
 *
 */

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class MainScreenActivity extends AppCompatActivity {

    private                     AppRater   mAppRater = new AppRater();
    private                         long   trainScreenBtnLastClickTime;
    private                         long   helpBtnLastClickTime;
    private                         long   trainBtnLastClickTime;

    public             SharedPreferences   mpreferences;
    public      SharedPreferences.Editor   editor;

    public                  AudioManager   mAudioManager;
    public                           int   SpeakerVol = 7;
    private static final    String   TAG   = "MainScreenActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }

            /*
            if( G_IsBeta ) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            }
            */
        }

        mpreferences      = getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        editor            = mpreferences.edit();

        SpeakerVol        = mpreferences.getInt("SpeakerVolume", SpeakerVol);


        Button helpButton        = (Button) findViewById(R.id.helpButton);
        Button trainButton       = (Button) findViewById(R.id.trainButton);
        Button trainButtonScreen = (Button) findViewById(R.id.buttonTrainScreen);

        trainScreenBtnLastClickTime   = SystemClock.elapsedRealtime();
        helpBtnLastClickTime          = SystemClock.elapsedRealtime();
        trainBtnLastClickTime         = SystemClock.elapsedRealtime();

        trainButtonScreen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (SystemClock.elapsedRealtime() - trainScreenBtnLastClickTime < 2000) {
                    return;
                }
                trainScreenBtnLastClickTime = SystemClock.elapsedRealtime();
                HelpPress();
                TrainPress();
            }
        });

        trainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (SystemClock.elapsedRealtime() - trainBtnLastClickTime < 2000) {
                    return;
                }
                trainBtnLastClickTime = SystemClock.elapsedRealtime();
                TrainPress();
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (SystemClock.elapsedRealtime() - helpBtnLastClickTime < 2000) {
                    return;
                }
                helpBtnLastClickTime = SystemClock.elapsedRealtime();
                HelpPress();
            }
        });

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setSpeakerphoneOn(true);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SpeakerVol, 0);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mAppRater.appLaunched(this);
    }


    public void TrainPress( )
    {
        Log.d(TAG, " onCreate() detected android.os.Build.VERSION.SDK_INT: "+android.os.Build.VERSION.SDK_INT);

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M){
            Log.d(TAG, "TrainPressFromView() failed Android version test. Is "+android.os.Build.VERSION.SDK_INT);
            return;
        }

        /*
        G_IsBeta = G_IsBeta && (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if( G_IsBeta ) {
            Date   date1 = Calendar.getInstance().getTime();
            String mdate = date1.toString();

            mdate = "  ---- Debug Log Test ---- "+mdate;

            appendLog(mdate);
        }
        */

        Intent intent = new Intent(this, TrainingSelectActivity.class);

        startActivity(intent);
        finish();
    }

    public void HelpPress( ) {

        Intent intent = new Intent(this, HelpScreenActivity.class);

        startActivity(intent);
        finish();
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

                        Log.d(TAG, "  == SoundPool volume forced to: " +mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) +
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

}
