package com.get.gsappalpha1;

/**
 *  ===== This work is copy righted! Do not copy or reuse without Guidance permission! ======
 *
 *    For detail, contact www.guidance-edu.com
 *
 *
 *     Created by Rob on 3/7/2017.
 *
 *  Copy Rights 2017, Guidance Education Technologies, All Rights Reserved.
 *
 */

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.get.gsappalpha1.util.IabHelper;
import com.get.gsappalpha1.util.IabResult;
import com.get.gsappalpha1.util.Inventory;
import com.get.gsappalpha1.util.Purchase;


public class TrainingSelectActivity extends AppCompatActivity {

    boolean                          unlimitedShotActivated;
    boolean                          shotTimerActivated;
    boolean                          KBConnected;

    private        int               DeviceTimerRating = -1;  // Present device Timer feature qualification status.
                                                              // 1.  will allow each device to run 5 qualification runs,
                                                              //     after which it must be a bought feature
                                                              // 2.  Even if feature is bought, will not run on a dis-
                                                              //     qualified device.

    private        IabHelper         mHelper;
    private        boolean           firstRun      = true;
    private        boolean           isPermitted   = false;
    private        int               SpeakerVol    = 7;
    static  final  String            APP_ITEM_TYPE = IabHelper.ITEM_TYPE_INAPP;
    static  final  String            ITEM_EXTEND   = "extended_shooting";
    static  final  String            ITEM_TIMER    = "shot_timer";
    private        String            deviceName;

    static  final  String            base64EncodedPublicKey =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg9I/lIQuHBnPDnBXjNz8ZGQPOCnacgeiuQqZUKKigK77Xp+NhnZPzL+mzR+4xt2Q9ZSBDAWhuY/A3xrDmK+PgntMtZj5ODh3d3Z9LdJwuOuPsTFwnEaRL5ut3c8Uy+XlTT1JRifmFCXdK2gE9RaBCAtA9DMirL0UrNGNaH3/fmSZHFNY11e1giT3W+qETzmdjZX8uH3an9bnCKlkhrgRpVNOFA06OFZ2mRRlyoxlCfy0djOHZU62Yo9bL/Qc7a0eGkH36RriXyLbMmcyOIGFodjlDbPHjoIvXhQwRoprPxk9dhp6Kq8ge9/QvPGrvPYTuXohFADDr9o9qHLeyQnm5QIDAQAB";

    private            SharedPreferences   mpreferences;
    private     SharedPreferences.Editor   editor;
    private                 AudioManager   mAudioManager;
    private static final          String   TAG   = "===TrainingSelectAct";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_select);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mpreferences           = getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        editor                 = mpreferences.edit();
        SpeakerVol             = mpreferences.getInt("SpeakerVolume", SpeakerVol);
        unlimitedShotActivated = false;
        unlimitedShotActivated = mpreferences.getBoolean("unlimitedshotsactivated", false);
        shotTimerActivated     = false;
        shotTimerActivated     = mpreferences.getBoolean("shotTimerActivated", false);
        KBConnected            = false;
        KBConnected            = mpreferences.getBoolean("keyboardconnected", false);
        DeviceTimerRating      = mpreferences.getInt("DeviceTimerRating", DeviceTimerRating);

        Log.d(TAG, "onCreate() - DeviceTimerRating value: " + DeviceTimerRating);


        if((!unlimitedShotActivated || !shotTimerActivated) && firstRun) {

            // need to retrieve buy record and recover cache if necessary
            mHelper = new IabHelper(this, base64EncodedPublicKey);

            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                //////            @Override
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {  //error
                        Log.d(TAG, "onIabSetupFinished in failure: " + result);
                    }
                    if (result.isSuccess()) {
                        Log.d(TAG, "*** IAB is fully set up ***");

                        try { //queryPurchasedItems;
                            mHelper.queryInventoryAsync(mHelper.mGotInventoryListener);

//                            Log.d(TAG, "IAB inventory acquired!");
                        } catch (IabHelper.IabAsyncInProgressException ex) {
//                            Log.d(TAG, "IabAsyncInProgressException caused IAB inventory recover failure!");
                        }
                    }
                }
            });

            mHelper.mGotInventoryListener
                    = new IabHelper.QueryInventoryFinishedListener() {
                public void onQueryInventoryFinished(IabResult result,
                                                     Inventory inventory) {

                    if (result.isFailure()) {
                        Log.d(TAG, " IAB inventory recovery final stage failed!");
                    } else {
                        unlimitedShotActivated = inventory.hasPurchase(ITEM_EXTEND);

                        if(unlimitedShotActivated) {
//                            Log.d(TAG, " unlimitedshotsactivated restated - update UI...");

                            editor.putBoolean("unlimitedshotsactivated", true);
                            editor.apply();

//                            Log.d(TAG, " calling updateIAPButton( "+ unlimitedShotActivated +" )");
                            updateIAPButton();

                        }

                        shotTimerActivated = inventory.hasPurchase(ITEM_TIMER);

                        if(shotTimerActivated) {
                            Log.d(TAG, " shotTimerActivated restated - update UI...");

                            editor.putBoolean("shotTimerActivated", true);
                            editor.commit();

                            Log.d(TAG, " calling updateIAPButton( "+ shotTimerActivated +" )");
                            updateIAPButton();

                        }
                    }
                }
            };

            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); // this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.setSpeakerphoneOn(true);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, SpeakerVol, 0);

            firstRun = false;
        }

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

        updateIAPButton();

        Log.d(TAG, " onCreate() calling checkRunTimePermission()...");

        checkRunTimePermission();
    }


    protected final void onResume(Bundle savedInstanceState) {
        super.onResume();

        final Boolean ExtendedShot = savedInstanceState.getBoolean("unlimitedShotActivated");

        unlimitedShotActivated = ExtendedShot;
//        Log.d(TAG, "onResume() recovered unlimitedShotActivated value = "+unlimitedShotActivated);
//
        final Boolean TimerShot = savedInstanceState.getBoolean("shotTimerActivated");

        shotTimerActivated = TimerShot;
//        Log.d(TAG, "onResume() recovered shotTimerActivated value = "+shotTimerActivated);

        checkRunTimePermission();
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called.");

        super.onDestroy();
        if (mHelper != null) {
            try {
                mHelper.dispose();
                mHelper = null;
            } catch (IabHelper.IabAsyncInProgressException ex) {
//                Log.d(TAG, "IabAsyncInProgressException occurred during IabHelp Destroy!");
            }
        }
    }

//    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionsResult() entered...requestCode = "+requestCode);

        if (requestCode == 11111) {
            for (int i = 0; i < grantResults.length; i++) {
//                String permission = permissions[i];

                isPermitted = (grantResults[i] == PackageManager.PERMISSION_GRANTED)? true : false;

                Log.d(TAG, " -- onRequestPermissionsResult() entered...isPermitted = "+isPermitted);

                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission - alert user and quit

                    break;
                }
            }

            if (!isPermitted) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Permission Warning");
                alertDialogBuilder.setMessage("App won't work without Camera Permission.");

                alertDialogBuilder.setNegativeButton("OK",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }

        }
    }
//    */

    private void checkRunTimePermission() {
        Log.d(TAG, "!! checkRunTimePermission() entered with isPermitted = "+isPermitted);

        String[] permissionArrays = new String[]{Manifest.permission.CAMERA};
//        String[] permissionArrays = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isPermitted) {  // minSdkVersion 23
            requestPermissions(permissionArrays, 11111);
        } else {

        }
    }


    public void HomePress(View view)
    {
        Log.d(TAG, "HomePress() entered....");

        checkRunTimePermission();

        Intent intent = new Intent(this, MainScreenActivity.class);

        startActivity(intent);
        finish();
    }


    public void HelpPress(View view)
    {
        Log.d(TAG, "HelpPress() entered....");

        Intent intent = new Intent(this, HelpScreenActivity.class);

        startActivity(intent);
        finish();
    }


    public void Shot10Press(View view)
    {
        checkRunTimePermission();

        if (!isPermitted) {
            // disable all activity buttons
            AlertDialog mAlertDialog = new AlertDialog.Builder(this).create();

            if(mAlertDialog != null) {
                mAlertDialog.setTitle("Disabled Function Warning");
                String msg = "Activity can't start without Camera Permission.";
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

        Intent    intent    = new Intent(this, ShootingActivity.class);
        int       shotLimit = 10;
        intent.putExtra("int", shotLimit);
        intent.putExtra("string", deviceName);
        startActivity(intent);
        finish();
    }

    public void Shot30Press(View view)
    {
        checkRunTimePermission();

        Log.d(TAG, "Shot30Press() entered with isPermitted = "+isPermitted);

        if (!isPermitted) {
            // disable all activity buttons
            AlertDialog mAlertDialog = new AlertDialog.Builder(this).create();

            if(mAlertDialog != null) {
                mAlertDialog.setTitle("Disabled Function Warning");
                String msg = "Activity can't start without Camera Permission.";
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

        Log.d(TAG, "ExtendedShot Selected. Ownership = "+unlimitedShotActivated);

        if(unlimitedShotActivated) {
            Intent intent = new Intent(this, ShootingActivity.class);
            int shotLimit = 5000;
            intent.putExtra("int", shotLimit);
            intent.putExtra("string", deviceName);
            startActivity(intent);
            finish();
        } else {

//            Log.d(TAG, "ExtendedShot Selected. To launchPurchaseFlow().... ");

            try {
                mHelper.launchPurchaseFlow(this, ITEM_EXTEND, APP_ITEM_TYPE, null, 10001, mHelper.mPurchaseListener, "");

//                Log.d(TAG, "mHelper.launchPurchaseFlow() fired!");

                mHelper.mPurchaseListener =
                        new IabHelper.OnIabPurchaseFinishedListener() {

                            public void onIabPurchaseFinished(IabResult result, Purchase purchase) {

                        Log.d(TAG, "onIabPurchaseFinished entered. IabResult = "+result);

                                // Don't complain if cancelling
                                if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                                    return;
                                }

                                if (!result.isSuccess()) {
//                            Log.d(TAG, "Error purchasing: " + result.getMessage());

                                    AlertDialog mAlertDialog = new AlertDialog.Builder(getApplicationContext()).create();

                                    if(mAlertDialog != null) {
                                        mAlertDialog.setTitle("Purchase Failure"); // ("Session Alert");
                                        String msg = "Please check internet connection and Google account settings.";
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
                                    return;
                                }

//                        Log.d(TAG, "onIabPurchaseFinished entered. purchase = "+purchase);

                                // Purchase was success! Update accordingly
                                if (purchase.getSku().equals(ITEM_EXTEND)) {
                                    unlimitedShotActivated = true;
                                    editor.putBoolean("unlimitedshotsactivated", true);
                                    editor.commit();

                            Log.d(TAG, "unlimitedShotActivated ON; update UI... ");
                                    updateIAPButton();
                                }
                            }
                        };

            } catch (IabHelper.IabAsyncInProgressException ex){
                Log.d(TAG, "IabAsyncInProgressException occurred!");
            }
        }
    }

    public void ShotTimerPress(View view)
    {
        checkRunTimePermission();

        if (!isPermitted) {
            // disable all activity buttons
            AlertDialog mAlertDialog = new AlertDialog.Builder(this).create();

            if(mAlertDialog != null) {
                mAlertDialog.setTitle("Disabled Function Warning");
                String msg = "Activity can't start without Camera Permission.";
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

        Log.d(TAG, "ShotTimerPress(): Ownership / DeviceTimerRating = "+
                shotTimerActivated+" / "+ DeviceTimerRating);





        if( DeviceTimerRating < 0 && DeviceTimerRating > -5) { // Device has not been qualified
            // Display Test Run message
            Log.d(TAG, "ShotTimerPress() to qualify device:  ODeviceTimerRating = "+ DeviceTimerRating);



            final Intent intent = new Intent(this, TimerActivity.class);

            intent.putExtra("string", deviceName);
            startActivity(intent);
            finish();
        } else if(shotTimerActivated && DeviceTimerRating > 0) { // bought Timer and Device is qualified

            Log.d(TAG, "ShotTimerPress() running on qualified device.");

            final Intent intent = new Intent(this, TimerActivity.class);

            intent.putExtra("string", deviceName);
            startActivity(intent);
            finish();

        } else if(shotTimerActivated && DeviceTimerRating <= -5) { // Device is dis-qualified
            // Put up a message about device is not good

            Log.d(TAG, "ShotTimerPress(): Timer won't run on disabled device!");






        } else { //  Device has been qualified but feature not bought yet

//            Log.d(TAG, "ExtendedShot Selected. To launchPurchaseFlow().... ");

            try {
                mHelper.launchPurchaseFlow(this, ITEM_TIMER, APP_ITEM_TYPE, null, 10002, mHelper.mPurchaseListener, "");

//                Log.d(TAG, "mHelper.launchPurchaseFlow() fired!");

                mHelper.mPurchaseListener =
                        new IabHelper.OnIabPurchaseFinishedListener() {

                            public void onIabPurchaseFinished(IabResult result, Purchase purchase) {

//                        Log.d(TAG, "onIabPurchaseFinished entered. IabResult = "+result);

                                // Don't complain if cancelling
                                if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
                                    return;
                                }

                                if (!result.isSuccess()) {
                            Log.d(TAG, "Error purchasing: " + result.getMessage());

                                    AlertDialog mAlertDialog = new AlertDialog.Builder(getApplicationContext()).create();

                                    if(mAlertDialog != null) {
                                        mAlertDialog.setTitle("Purchase Failure"); // ("Session Alert");
                                        String msg = "Please check internet connection and Google account settings.";
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
                                    return;
                                }

                        Log.d(TAG, "onIabPurchaseFinished entered. purchase = "+purchase);

                                // Purchase was success! Update accordingly
                                if (purchase.getSku().equals(ITEM_TIMER)) {
                                    shotTimerActivated = true;
                                    editor.putBoolean("shotTimerActivated", true);
                                    editor.commit();

                            Log.d(TAG, "shotTimerActivated ON; update UI... ");
                                    updateIAPButton();
                                }
                            }
                        };

            } catch (IabHelper.IabAsyncInProgressException ex){
                Log.d(TAG, "IabAsyncInProgressException occurred!");
            }
        }
    }

    public void updateIAPButton()
    {
        Button   button = (Button) findViewById(R.id.unlimitedshotbutton);
        Drawable image  = getResources().getDrawable(R.drawable.bp2);
        Drawable imageb = getResources().getDrawable(R.drawable.bp2bf);

        if(unlimitedShotActivated) {
            Log.d(TAG, "updateIAPButton - unlimitedshots no Text");

            button.setBackground(image);
        } else {
            Log.d(TAG, "updateIAPButton - unlimitedshots yes Text");

            button.setBackground(imageb);
        }

        button = (Button) findViewById(R.id.shottimerbutton);
        image  = getResources().getDrawable(R.drawable.bp3);
        imageb = getResources().getDrawable(R.drawable.bp3bf);

        if(shotTimerActivated) {
            Log.d(TAG, "updateIAPButton - shottimerbutton no Text");

            button.setBackground(image);
        } else {
            Log.d(TAG, "updateIAPButton - shottimerbutton yes Text");

            button.setBackground(imageb);
        }

        button.invalidate();
    }


    public void TestPress(View view)
    {
        Intent intent = new Intent(this, TestActivity.class);
      //  int shotLimit = 5000;
       // intent.putExtra("int", shotLimit);
        intent.putExtra("string", deviceName);
        startActivity(intent);
        finish();
    }

    public void updateKBIcon(boolean isOn) {
        //
        if(isOn) {
            // show Remote Icon on screen
            Log.d(TAG, "updateKBIcon() to show KB symbol");

        } else {
            // hide Remote Icon on screen
            Log.d(TAG, "updateKBIcon() to hide KB symbol");

        }
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
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        editor                        = preferences.edit();

        KBConnected = preferences.getBoolean("keyboardconnected", false);

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
        editor.apply();

        updateKBIcon(KBConnected);
    }
}



