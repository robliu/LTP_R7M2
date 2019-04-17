package com.get.gsappalpha1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.get.gsappalpha1.util.IabHelper;

/**
 *  ===== This work is copyrighted! Do not copy or reuse without Guidance permission! ======
 *
 *    For detail, contact www.guidance-edu.com
 *
 *
 *     Created by Rob on 4/29/2017.
 *
 *  Copy Rights 2016, 2017, Guidance Education Technologies, All Rights Reserved.
 *
 */


public class TimerSettingsActivity extends AppCompatActivity {

    private                IabHelper  mHelper;
    public         SharedPreferences  preferences;
    public  SharedPreferences.Editor  editor;

    // MUST match the slider range in activity_shottimersettings.xml
    private static final         int  MaxSessionNumSlider = 8;
    private static final         int  MaxSessionShot      = 10;
    private static final         int  MaxSessionDelay     = 10;
    private static final         int  MaxSessionLength    = 30;
    private static final         int  MaxCountdownLength  = 20;

    private static final      String  TAG = "==TimerSettingsActivity";
    public                       int  SessionsLimit;
    public                       int  SessionLength;
    public                       int  ShotsLimit;
    public                       int  SessionDelay;
    public                       int  CountDown;
    public                   boolean  GetReadyMuteOn;
    public                   boolean  RandomModeOn;

    private                  SeekBar  sk1, sk2, sk3, sk4, sk5;
    private                 TextView  skv1, skv2, skv3, skv4, skv5, swch1, swch2;
    private                   Switch  switch_btn1, switch_btn2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, " $onCreate( ) entered !");
        Log.d(TAG, " ");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shottimersettings);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        preferences   = getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);

        SessionsLimit   = preferences.getInt("TimerSessionsLimit", 2);
        SessionLength   = preferences.getInt("TimerSessionLength", 15);
        ShotsLimit      = preferences.getInt("TimerShotsLimit", 3);
        SessionDelay    = preferences.getInt("TimerSessionDelay", 5);
        CountDown       = preferences.getInt("TimerCountDown", 5);
        GetReadyMuteOn  = preferences.getBoolean("TimerReadyMuteOn", true);
        RandomModeOn    = preferences.getBoolean("TimerRandomModeOn", true);

        Log.d(TAG, " $onCreate( ) retrieved: #sess, shots, time, leng, ctdwn, GetReadyMuteOn, RandomModeOn = "+SessionsLimit+", "
                +ShotsLimit+", "+SessionDelay+", "+SessionLength+", "+CountDown+", "+GetReadyMuteOn+", "+RandomModeOn);

        getWindow().getDecorView().setSystemUiVisibility(
                // Set the IMMERSIVE flag.
                // Set the content to appear under the system bars so that the content
                // doesn't resize when the system bars hide and show.
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );

        sk1  = findViewById(R.id.SessionNumSlider);
        skv1 = findViewById(R.id.skv1);
        skv1.setText(String.valueOf(SessionsLimit));

        if(sk1 != null) {
            sk1.setMax(MaxSessionNumSlider);
            sk1.setProgress(SessionsLimit);
            sk1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (seekBar.getProgress() < 1) {
                        seekBar.setProgress(1);
                        skv1.setText("1");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress < 1) progress = 1;
                    SessionsLimit = progress;
                    skv1.setText(String.valueOf(SessionsLimit));
                    // Update the TextView
                    Log.d(TAG, " SessionsLimit updated to: " + SessionsLimit);
                }
            });

        }

        sk2  = (SeekBar) findViewById(R.id.SessionShotSlider);
        skv2 = (TextView)findViewById(R.id.skv2);
        skv2.setText(String.valueOf(ShotsLimit));

        if(sk2 != null) {
            sk2.setMax(MaxSessionShot);
            sk2.setProgress(ShotsLimit);

            sk2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (seekBar.getProgress() < 1) {
                        seekBar.setProgress(1);
                        skv2.setText("1");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress < 1) progress = 1;
                    ShotsLimit = progress;
                    skv2.setText(String.valueOf(ShotsLimit));
                    // Update the TextView
                    Log.d(TAG, " ShotsLimit updated to: " + ShotsLimit);
                }
            });
        }

        sk3  = findViewById(R.id.SessionDelaySlider);
        skv3 = findViewById(R.id.skv3);
        skv3.setText(String.valueOf(SessionDelay));

        if(sk3 != null) {
            sk3.setMax(MaxSessionDelay);
            sk3.setProgress(SessionDelay);
            sk3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (seekBar.getProgress() < 2) {
                        seekBar.setProgress(2);
                        skv3.setText("2");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress < 2) progress = 2;
                    SessionDelay = progress;
                    skv3.setText(String.valueOf(SessionDelay));
                    // Update the Text View
                    Log.d(TAG, " SessionDelay updated to: " + SessionDelay);
                }
            });
        }

        sk4  = findViewById(R.id.CountdownLengthSlider);
        skv4 = findViewById(R.id.skv4);
        skv4.setText(String.valueOf(CountDown));

        if(sk4 != null) {
            sk4.setMax(MaxCountdownLength);
            sk4.setProgress(CountDown);
            sk4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (seekBar.getProgress() < 2) {
                        seekBar.setProgress(2);
                        skv4.setText("2");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress < 2) progress = 2;
                    CountDown = progress;
                    skv4.setText(String.valueOf(CountDown));
                    // Update the TextView
                    Log.d(TAG, " CountDown updated to: " + CountDown);
                }
            });
        }

        sk5  = (SeekBar) findViewById(R.id.SessionLengthSlider);
        skv5 = (TextView)findViewById(R.id.skv5);
        skv5.setText(String.valueOf(SessionLength));

        if(sk5 != null) {
            sk5.setMax(MaxSessionLength);
            sk5.setProgress(SessionLength);
            sk5.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (seekBar.getProgress() < 3) {
                        seekBar.setProgress(3);
                        skv5.setText("3");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress < 3) progress = 3;
                    SessionLength = progress;
                    skv5.setText(String.valueOf(SessionLength));
                    // Update the TextView
                    Log.d(TAG, " SessionLength updated to: " + SessionLength);
                }
            });
        }

        switch_btn1 = findViewById(R.id.MakeReadySwitch);
        swch1       = findViewById(R.id.MakeReady_Switch_On);

        if(switch_btn1 != null) {

            Log.d(TAG, "  switch_btn value = "+ Boolean.toString( GetReadyMuteOn) );

            if (GetReadyMuteOn) {
                switch_btn1.setChecked(false);
                swch1.setText(getResources().getString(R.string.Mute));
            }
            else {
                switch_btn1.setChecked(true);
                swch1.setText(getResources().getString(R.string.on));
            }

            switch_btn1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        // If the switch button is on
                        switch_btn1.setChecked(true);
                        GetReadyMuteOn = false;
                        swch1.setText(getResources().getString(R.string.on));
                    }
                    else {
                        // If the switch button is off
                        switch_btn1.setChecked(false);
                        GetReadyMuteOn = true;
                        swch1.setText(getResources().getString(R.string.Mute));
                    }
                }
            });
        } else {
            Log.d(TAG, "  !!!  Warning - can't get switch_btn1 value  !!!" );
        }

        switch_btn2 = findViewById(R.id.RandomModeSwitch);
        swch2       = findViewById(R.id.RandomMode_Switch_On);

        if(switch_btn2 != null) {

            Log.d(TAG, " Random Mode switch value = "+ Boolean.toString( RandomModeOn) );

            final TextView TxView = (TextView)findViewById(R.id.RandomModeScribe);

            if (RandomModeOn) {
                switch_btn2.setChecked(true);
                swch2.setText(getResources().getString(R.string.on));
                TxView.setText(getResources().getString(R.string.RandomTimerDescriptions));
            }
            else {
                switch_btn2.setChecked(false);
                swch2.setText(getResources().getString(R.string.off));
                TxView.setText(getResources().getString(R.string.FixedTimerDescriptions));
            }

            switch_btn2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        // If the switch button is on
                        switch_btn2.setChecked(true);
                        RandomModeOn = true;
                        swch2.setText(getResources().getString(R.string.on));
                        TxView.setText(getResources().getString(R.string.RandomTimerDescriptions));
                    }
                    else {
                        // If the switch button is off
                        switch_btn2.setChecked(false);
                        RandomModeOn = false;
                        swch2.setText(getResources().getString(R.string.off));
                        TxView.setText(getResources().getString(R.string.FixedTimerDescriptions));
                    }
                }
            });
        } else {
            Log.d(TAG, "  !!!  Warning - can't get switch_btn2 value  !!!" );
        }
    }

    public void onResume(Bundle savedInstanceState) {
//        protected final void onResume(Bundle savedInstanceState) {
        super.onResume();

        sk1.setProgress(SessionsLimit);
        sk2.setProgress(ShotsLimit);
        sk3.setProgress(SessionDelay);
        sk4.setProgress(CountDown);
        sk5.setProgress(SessionLength);

        if(GetReadyMuteOn) {
            switch_btn1.setChecked(false);
            swch1.setText(getResources().getString(R.string.Mute));
        } else {
            switch_btn1.setChecked(true);
            swch1.setText(getResources().getString(R.string.on));
        }

        if(RandomModeOn) {
            switch_btn2.setChecked(true);
            swch2.setText(getResources().getString(R.string.on));
        } else {
            switch_btn2.setChecked(false);
            swch2.setText(getResources().getString(R.string.off));
        }
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called.");

        super.onDestroy();
    }


    public void HomePress(View view)
    {
        Intent intent = new Intent(this, MainScreenActivity.class);

        startActivity(intent);
        finish();
    }

    public void TrainPress(View view)
    {
        Intent intent = new Intent(this, TrainingSelectActivity.class);

        startActivity(intent);
        finish();
    }

    public void OKPress(View view)
    {
        Intent intent = new Intent(this, TimerActivity.class);

        editor = preferences.edit();
        editor.putInt("TimerSessionsLimit", SessionsLimit);

        editor.putInt("TimerShotsLimit", ShotsLimit);

        editor.putInt("TimerSessionDelay", SessionDelay);

        editor.putInt("TimerSessionLength", SessionLength);

        editor.putInt("TimerCountDown", CountDown);

        editor.putBoolean("TimerReadyMuteOn", GetReadyMuteOn);

        editor.putBoolean("TimerRandomModeOn", RandomModeOn);
        editor.commit();

        Log.d(TAG, " $$$$$$ Preferences Saved: #sess, shots, time, leng, ctdwn, GetReadyMuteOn, RandomModeOn = "
                + SessionsLimit +", " + ShotsLimit +", " + SessionDelay +", " + SessionLength +", " + CountDown +
                ", " + GetReadyMuteOn +", " + RandomModeOn);

        startActivity(intent);
        finish();
    }


    public void CANCELPress(View view)
    {
        Intent intent = new Intent(this, TimerActivity.class);

        startActivity(intent);
        finish();
    }


}
