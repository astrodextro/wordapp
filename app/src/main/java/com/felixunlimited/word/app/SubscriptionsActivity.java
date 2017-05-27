package com.felixunlimited.word.app;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.Button;
import android.widget.TimePicker;

import com.felixunlimited.word.app.data.MessageContract;

public class SubscriptionsActivity
        extends Activity
        implements LoaderManager.LoaderCallbacks<Cursor>
{
    private static final int SUBSCRIPTIONS_LOADER = 0;
    static final String SUBSCRIPTIONS_URI = "URI";
    private Uri mUri;
    private int mPrayersSubscription;
    private int mDeclarationsSubscription;
    private Button prayersButton;
    private Button declarationsButton;
    private TimePicker prayersAlarm;
    private TimePicker declarationsAlarm;

    private static final String[] SUBSCRIPTIONS_COLUMNS = {
            MessageContract.UserEntry.TABLE_NAME + "." + MessageContract.UserEntry._ID,
            MessageContract.UserEntry.COLUMN_USER_EMAIL,
            MessageContract.UserEntry.COLUMN_DOWNLOADS,
            MessageContract.UserEntry.COLUMN_PURCHASES,
            MessageContract.UserEntry.COLUMN_STREAMS,
            MessageContract.UserEntry.COLUMN_DECLARATIONS_SUBSCRIPTION,
            MessageContract.UserEntry.COLUMN_PRAYERS_SUBSCRIPTION
    };

    // These indices are tied to DETAIL_COLUMNS.  If DETAIL_COLUMNS changes, these
    // must change.
    public static final int USER_ID =           0;
    public static final int COL_USER_EMAIL =    1;
    public static final int COL_DOWNLOADED =    2;
    public static final int COL_PURCHASED =     3;
    public static final int COL_STREAMED =      4;
    public static final int COL_DECLARATIONS =  5;
    public static final int COL_PRAYERS =       6;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriptions);
        getLoaderManager().initLoader(SUBSCRIPTIONS_LOADER, null, (android.app.LoaderManager.LoaderCallbacks<Cursor>) this);

        declarationsButton = (Button) findViewById(R.id.declarations_subscriptions_button);
        prayersButton = (Button) findViewById(R.id.prayers_subscription_button);
        declarationsAlarm = (TimePicker) findViewById(R.id.declarations_timePicker);
        prayersAlarm = (TimePicker) findViewById(R.id.prayers_timePicker);
    }

//    private void setAlarm(){
//        Context context = getApplicationContext();
//
//        AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
//        Intent i = new Intent(context, MessageReceiver.class);
//        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
//        Calendar myCal = Calendar.getInstance();
//        myCal.setTimeInMillis(12);
//        mgr.set(AlarmManager.RTC_WAKEUP, myCal.getTimeInMillis(), pi);
//        Log.i("", "alarm set for " + myCal.getTime().toLocaleString());
//        Toast.makeText(getApplicationContext(),"Alarm set for " + myCal.getTime().toLocaleString(), Toast.LENGTH_LONG).show();
//
//    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if ( null != mUri ) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    this,
                    mUri,
                    SUBSCRIPTIONS_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            mDeclarationsSubscription = data.getInt(COL_DECLARATIONS);
            mPrayersSubscription = data.getInt(COL_PRAYERS);

//            declarationsAlarm.
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
