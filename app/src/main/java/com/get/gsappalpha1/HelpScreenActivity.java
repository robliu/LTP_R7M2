package com.get.gsappalpha1;

/**
 *  ===== This work is copyrighted! Do not copy or reuse without Guidance permission! ======
 *
 *    For detail, contact www.guidance-edu.com
 *
 *
 *     Created by Rob on 4/9/2017.
 *
 *  Copy Rights 2017, Guidance Education Technologies, All Rights Reserved.
 *
 */

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;


public class HelpScreenActivity extends AppCompatActivity {

    private    Button         trainButton;
    private    Button          homeButton;

    private final String TAG      = "HelpScreenActivity";
    private final    int MaxPages = 8;
    private float tc_x1, tc_x2;                                 // touch coordinates
    private float tc_y1, tc_y2;                                 // touch coordinates
    private int   pageIndx = 0;
//    private final String pDrawables[] = {"@drawable/helpdoc1", "@drawable/helpdoc2", "@drawable/helpdoc3", "@drawable/helpdoc4", "@drawable/helpdoc5"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_screen);
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

        homeButton  = (Button) findViewById(R.id.homeButton);
        trainButton = (Button) findViewById(R.id.trainButton);

        homeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                HomePress();
            }
        });

        trainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TrainPress();
            }
        });

    }


    public void updateHelpPage(int pIndx) {

        Log.d(TAG, "HomePress() entered... pIndx = "+pIndx);

        if(pIndx < 0 || pIndx >= MaxPages) return;

        ImageView pageView = (ImageView)findViewById(R.id.imageViewHelp);

        if(pIndx == 0) {
            Drawable pSrc = getResources().getDrawable(R.drawable.helpdoc1);
            pageView.setImageDrawable(pSrc);
        } else if(pIndx == 1) {
            Drawable pSrc = getResources().getDrawable(R.drawable.helpdoc2);
            pageView.setImageDrawable(pSrc);
        } else if(pIndx == 2) {
            Drawable pSrc = getResources().getDrawable(R.drawable.helpdoc3);
            pageView.setImageDrawable(pSrc);
        } else if(pIndx == 3) {
            Drawable pSrc = getResources().getDrawable(R.drawable.helpdoc4);
            pageView.setImageDrawable(pSrc);
        } else if(pIndx == 4) {
            Drawable pSrc = getResources().getDrawable(R.drawable.helpdoc5);
            pageView.setImageDrawable(pSrc);
        } else if(pIndx == 5) {
            Drawable pSrc = getResources().getDrawable(R.drawable.helpdoc6);
            pageView.setImageDrawable(pSrc);
        } else if(pIndx == 6) {
            Drawable pSrc = getResources().getDrawable(R.drawable.helpdoc7);
            pageView.setImageDrawable(pSrc);
        }

    }


    public void HomePress( )
    {
        Log.d(TAG, "HomePress() entered....");

        Intent intent = new Intent(this, MainScreenActivity.class);

        startActivity(intent);
        finish();
    }

    public void TrainPress( )
    {
        Log.d(TAG, "TrainPress() entered....");
        Log.d(TAG, " onCreate() detected android.os.Build.VERSION.SDK_INT: "+android.os.Build.VERSION.SDK_INT);

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M){

            return;
        }

        Intent intent = new Intent(this, TrainingSelectActivity.class);

        startActivity(intent);
        finish();
    }

    public boolean onTouchEvent(MotionEvent touchevent) {

        if(pageIndx >= 0 && pageIndx < MaxPages)
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
                        if (pageIndx > 0) {
                            pageIndx--;

                            //// change the page content
                            updateHelpPage(pageIndx);

                        }
                    } else if (tc_x1 > tc_x2 + mdist && dstx > dsty * 3) {
//                        Log.d(TAG, "   ===touch RL");
                        if (pageIndx < MaxPages - 1) {
                            pageIndx++;

                        //// change the page content
                            updateHelpPage(pageIndx);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
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
}
