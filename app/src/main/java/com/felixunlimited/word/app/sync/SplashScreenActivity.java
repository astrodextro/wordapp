package com.felixunlimited.word.app.sync;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.felixunlimited.word.app.R;

public class SplashScreenActivity extends Activity {

    SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
//        final SharedPreferences.Editor editor = sharedPreferences.edit();

        setContentView(R.layout.activity_splash_screen);

        int SPLASH_TIME_OUT = 5000;
        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                ((TextView) findViewById(R.id.welcome_text_view)).setText("and hearing...");
            }
        }, SPLASH_TIME_OUT);
        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                ((TextView) findViewById(R.id.welcome_text_view)).setText("and hearing..");
            }
        }, SPLASH_TIME_OUT);
        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                ((TextView) findViewById(R.id.welcome_text_view)).setText("Welcome to WordApp");
            }
        }, SPLASH_TIME_OUT);
        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                ((TextView) findViewById(R.id.welcome_text_view)).setText("We celebrate you!");
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}