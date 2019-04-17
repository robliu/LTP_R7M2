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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

public class WarningScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning_screen);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    public void ContinuePress(View view)
    {
        Intent intent = new Intent(this, MainScreenActivity.class);

        startActivity(intent);
        finish();
    }
}
