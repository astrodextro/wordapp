package com.felixunlimited.word.app;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

/**
 * Created by Ando on 14/06/2016.
 */
public class PayLoadTask extends AsyncTask<String, Void, String> {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onPreExecute() {
//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(get);

        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {
        String method = params[1];

        return null;
    }

    @Override
    protected void onPostExecute(String aString) {
        super.onPostExecute(aString);
    }

    public PayLoadTask() {
        super();
    }
}
