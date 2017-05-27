package com.felixunlimited.word.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.felixunlimited.word.app.data.MessageContract;

public class EventsActivity extends AppCompatActivity {

    private static final String[] EVENTS_PROJECTION = new String[] {
            MessageContract.EventsEntry._ID,
            MessageContract.EventsEntry.COLUMN_DATE,
            MessageContract.EventsEntry.COLUMN_TIME,
            MessageContract.EventsEntry.COLUMN_TITLE,
            MessageContract.EventsEntry.COLUMN_DESCRIPTION,
            MessageContract.EventsEntry.COLUMN_VENUE
    };

    // these indices must match the projection
    private static final int EVENT_ID = 0;
    private static final int EVENT_DATE = 1;
    private static final int EVENT_TIME = 2;
    private static final int EVENT_TITLE = 3;
    private static final int EVENT_DESCRIPTION = 4;
    private static final int EVENT_VENUE = 5;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NestedScrollView parent_ = (NestedScrollView) inflater.inflate(R.layout.activity_events,
                null);
        LinearLayout parent = (LinearLayout) parent_.findViewById(R.id.event_layout);

        Uri eventsUri = MessageContract.EventsEntry.CONTENT_URI;

        // we'll query our contentProvider, as always
        Cursor eventsCursor = getContentResolver().query(eventsUri, EVENTS_PROJECTION, null, null, null);

        assert eventsCursor != null;
        eventsCursor.moveToFirst();

        while(!eventsCursor.isAfterLast()) {

            View custom = inflater.inflate(R.layout.event, null);
            ImageView iv = (ImageView) custom.findViewById(R.id.event_image_view);
            Utility.setImage(Utility.choosePreferredDir(context).getAbsolutePath(), "e"+eventsCursor.getInt(EVENT_ID)+".jpg", iv);

            TextView tv = (TextView) custom.findViewById(R.id.event_title_view);
            tv.setText(eventsCursor.getString(EVENT_TITLE));
            tv = (TextView) custom.findViewById(R.id.event_date_view);
            tv.setText(eventsCursor.getString(EVENT_DATE));
            tv = (TextView) custom.findViewById(R.id.event_time_view);
            tv.setText(eventsCursor.getString(EVENT_TIME));
            tv = (TextView) custom.findViewById(R.id.event_description);
            tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            tv = (TextView) custom.findViewById(R.id.event_description_view);
            tv.setText(eventsCursor.getString(EVENT_DESCRIPTION));
            tv = (TextView) custom.findViewById(R.id.event_venue_view);
            tv.setText(eventsCursor.getString(EVENT_VENUE));

            parent.addView(custom);
            eventsCursor.moveToNext();
        }

        setContentView(parent_);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0f);
        }

        eventsCursor.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
