package com.get.gsappalpha1;

/**
 *  ===== This work is copy ighted! Do not copy or reuse without Guidance permission! ======
 *
 *    For detail, contact www.guidance-edu.com
 *
 *
 *     Created by Rob on 11/23/2016.
 *
 *  Copy Rights 2016, 2017, Guidance Education Technologies, All Rights Reserved.
 *
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private                 String         deviceName;
    private static final    String         TAG          = "MainActivity";

    public      SharedPreferences          mpreferences;
    public      SharedPreferences.Editor   editor;

    // Used to load the 'cpp-lib' library on application startup.
    static {
        System.loadLibrary("glt21xxlib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mpreferences      = getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        editor            = mpreferences.edit();

        long    dateFirstLaunch   = mpreferences.getLong("date_firstlaunch", 0);
//      long    LastLaunchTime    = mpreferences.getLong("LastLaunchTime", 0);     // last time used
//      long    ThisLaunchTime    = mpreferences.getLong("ThisLaunchTime", 0);     // last time used
        int     launchCount       = mpreferences.getInt("launchCount", 0);        // # of times App used; updated in AppRater()
        String  oldDeviceName     = mpreferences.getString("MyDeviceName", "");
        String  oldDeviceModel    = mpreferences.getString("MyDeviceModel", "");
        int     SessionCount      = mpreferences.getInt("SessionCount", 0);        // Sessions activated in this launch
        int     TotalSessionsDone = mpreferences.getInt("TotalSessionsDone", 0);   // # of all sessions done so far
        String  manufacturer      = Build.MANUFACTURER;
        String  model             = Build.MODEL;


        if (dateFirstLaunch == 0) {
            dateFirstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", dateFirstLaunch);
            editor.commit();

            editor.putLong("ThisLaunchTime", dateFirstLaunch);
            editor.commit();

            editor.putLong("LastLaunchTime", 0);
            editor.commit();

            launchCount = 1;
            editor.putLong("launchCount", launchCount);
            editor.commit();
        } else {
            long LastLaunchTime  = mpreferences.getLong("ThisLaunchTime", 0);
            editor.putLong("LastLaunchTime", LastLaunchTime);
            editor.commit();

            long ThisLaunchTime = System.currentTimeMillis();
            editor.putLong("ThisLaunchTime", ThisLaunchTime);
            editor.commit();

            launchCount+=1;
            editor.putLong("launchCount", launchCount);
            editor.commit();
        }

        if (model.startsWith(manufacturer)) {
            deviceName = capitalize(model);
        } else {
            deviceName = capitalize(manufacturer) + " " + model;
        }

        if(oldDeviceName.length() < 1 && deviceName.length() > 1) {
            editor.putString("MyDeviceName", deviceName);
            editor.commit();

            Log.d(TAG, "onCreate - MyDeviceName set to: "+deviceName);

        } else if( oldDeviceName.length() < 1 || !oldDeviceName.matches(deviceName) ) {
            editor.putString("MyDeviceName", deviceName);
            editor.commit();

            Log.d(TAG, "onCreate - MyDeviceName set to: "+deviceName);
        } else {
            Log.d(TAG, "onCreate - MyDeviceName kept at: "+deviceName);
        }

        if(oldDeviceModel.length() < 1 && model.length() > 1) {
            editor.putString("MyDeviceModel", model);
            editor.commit();

            Log.d(TAG, "onCreate - MyDeviceModel set to: "+model);

        } else if( oldDeviceModel.length() < 1 || !oldDeviceModel.matches(model) ) {
            editor.putString("MyDeviceModel", model);
            editor.commit();

            Log.d(TAG, "onCreate - MyDeviceModel set to: "+model);
        } else {
            Log.d(TAG, "onCreate - MyDeviceModel kept at: " + model);
        }
    }


    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
