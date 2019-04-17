package com.get.gsappalpha1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;


public class SplashActivity extends AppCompatActivity {

    /** Duration of wait **/
    private final static int SPLASH_DISPLAY_LENGTH = 6000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final Intent mainIntent = new Intent(this, WarningScreenActivity.class);
        mainIntent.setFlags(mainIntent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);

        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */

                ImageView imageView = (ImageView)findViewById(R.id.splashscreen);
                Drawable drawable = imageView.getBackground();
                if (drawable instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    bitmap.recycle();
                }

                finish();
                startActivity(mainIntent);
//                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}
