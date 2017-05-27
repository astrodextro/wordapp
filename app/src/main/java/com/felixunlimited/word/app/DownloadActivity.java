package com.felixunlimited.word.app;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felixunlimited.word.app.data.MessageContract;

import org.json.JSONException;

import static com.felixunlimited.word.app.Utility.updateDB;

public class DownloadActivity extends AppCompatActivity {
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void onStateNotSaved() {
        super.onStateNotSaved();
    }

    private String mDownloadUrl;
    private String mDownloadFile;
    private int messageID;
    private TextView status1, title, preacher, date, cost, status2;
    Button downloadButton;
    ProgressDialog mProgressDialog;

    @SuppressLint("ParcelCreator")
    private class DownloadReceiver extends ResultReceiver {
        public DownloadReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == DownloadService.UPDATE_PROGRESS) {
                int progress = resultData.getInt("progress");
                mProgressDialog.setProgress(progress);
                if (progress == 100 && mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    try {
                        updateDB(DownloadActivity.this, "downloaded", 0, 0, 0, System.currentTimeMillis());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String string = bundle.getString(DownloadService.FILEPATH);
                int resultCode = bundle.getInt(DownloadService.RESULT);
                if (resultCode == RESULT_OK) {
                    Toast.makeText(getBaseContext(),
                            "Download complete.",
                            Toast.LENGTH_LONG).show();
                    status1.setText("Your download is complete. Thanks for downloading");
                    status2.setText("We celebrate you!");
                    String[] selectionArgs = { "" + messageID };
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MessageContract.MessageEntry.COLUMN_DOWNLOADED, 1);

                    getContentResolver().update(MessageContract.MessageEntry.CONTENT_URI, contentValues,
                            MessageContract.MessageEntry._ID + "= ?", selectionArgs);

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
                            finish();
                        }
                    }, SPLASH_TIME_OUT);
                } else {
                    Toast.makeText(getBaseContext(), "Download failed",
                            Toast.LENGTH_LONG).show();
                    status1.setText("Sorry! Your download could not be completed");
                    status2.setText("Please ensure you have sufficient storage space and good network connection and try again");
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Utility.isConnected(this))
        {
            Toast.makeText(this, "Please check your internet connectivity and try again", Toast.LENGTH_LONG).show();
            onDestroy();
        }
        setContentView(R.layout.activity_download);
        status1 = (TextView) findViewById(R.id.status1);
        title = (TextView) findViewById(R.id.title);
        preacher = (TextView) findViewById(R.id.preacher);
        date = (TextView) findViewById(R.id.date);
        cost = (TextView) findViewById(R.id.cost);
        status2 = (TextView) findViewById(R.id.status2);
        mDownloadUrl = getIntent().getStringExtra(DownloadService.URL);
        mDownloadFile = getIntent().getStringExtra(DownloadService.FILENAME);
        downloadButton = (Button) findViewById(R.id.download_button);
        mProgressDialog = new ProgressDialog(DownloadActivity.this);
        mProgressDialog.setMessage("Downloading: "+getIntent().getStringExtra("title"));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);//        Utility.chooseTheme(this);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(100);

        title.append(getIntent().getStringExtra("title"));
        preacher.append(getIntent().getStringExtra("preacher"));
        date.append(getIntent().getStringExtra("date"));
        cost.append(getIntent().getStringExtra("cost"));
        messageID = getIntent().getIntExtra("message_id",0);
// instantiate it within the onCreate method
    }

    @Override
    protected void onResume() {
//        Utility.chooseTheme(this);
        super.onResume();
        registerReceiver(receiver, new IntentFilter(DownloadService.NOTIFICATION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void onClick(View view) {
        downloadButton.setEnabled(false);
        mProgressDialog.show();
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra("receiver", new DownloadReceiver(new Handler()));

        // add infos for the service which file to download and where to store
        intent.putExtra(DownloadService.FILENAME, mDownloadFile);
        intent.putExtra(DownloadService.URL,
                mDownloadUrl);
        intent.putExtra("title", getIntent().getStringExtra("title"));
        startService(intent);
        status1.setText("You are downloading: ");
        status2.setText("Your download has started");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_download, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
//        if (id == R.id.action_subscriptions) {
//            startActivity(new Intent(this, SubscriptionsActivity.class));
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }
}