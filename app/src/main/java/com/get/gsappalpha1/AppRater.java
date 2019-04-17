package com.get.gsappalpha1;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Rob, Guidance Education Technologies, on 6/17/2017.
 *
 *  Copy Rights 2017. All Rights Reserved.
 */

public class AppRater {
    private final String             APP_TITLE = "G-Sight LTP";         // App Name
    private final String             APP_PNAME = "com.get.gsappalpha1"; // Package Name
    private double           DAYS_UNTIL_PROMPT = 2.5;                   //  Min number of days passed
    private int          LAUNCHES_UNTIL_PROMPT = 4;                     //  Min number of launches

    public void appLaunched(Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences("AppRater", 0);
        if (prefs.getBoolean("dontshowagain", false)) { return ; }

        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        int launch_count = prefs.getInt("launchCount", 0);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }

        // Wait at least n days before opening
        if (launch_count % LAUNCHES_UNTIL_PROMPT == 3) {
            if (System.currentTimeMillis() >= date_firstLaunch +
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog(mContext, editor);
            }
        }

        editor.commit();
    }

    public void showRateDialog(final Context mContext, final SharedPreferences.Editor editor) {
        final Dialog dialog = new Dialog(mContext);
        dialog.setTitle("Rate " + APP_TITLE);

        LinearLayout ll = new LinearLayout(mContext);
        ll.setOrientation(LinearLayout.VERTICAL);

        TextView tv = new TextView(mContext);

        String  msg = " If you like " + APP_TITLE + ", please take a moment to rate it. Thanks for your support!";

        tv.setText(msg);
        tv.setWidth(680);
        tv.setTextSize(18.0f);
        tv.setPadding(20, 20, 20, 20);
        ll.addView(tv);

        Button b1 = new Button(mContext);
        msg       = "Rate " + APP_TITLE;

        b1.setText(msg);
        b1.setTextSize(18.0f);
        b1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
                dialog.dismiss();
            }
        });
        ll.addView(b1);

        Button b2 = new Button(mContext);
        msg       = "Remind me later";

        b2.setText(msg);
        b2.setTextSize(18.0f);
        b2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        ll.addView(b2);

        Button b3 = new Button(mContext);
        msg       = "No, thanks";

        b3.setText(msg);
        b3.setTextSize(18.0f);
        b3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (editor != null) {
                    editor.putBoolean("dontshowagain", true);
                    editor.commit();
                }
                dialog.dismiss();
            }
        });
        ll.addView(b3);

        dialog.setContentView(ll);
        dialog.show();
    }
}
