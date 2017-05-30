package com.felixunlimited.word.app;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.transition.Slide;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.felixunlimited.word.app.sync.SplashScreenActivity;
import com.felixunlimited.word.app.sync.WordAppSyncAdapter;

//import com.felixunlimited.word.app.data.AndroidDatabaseManager;

//import com.felixunlimited.word.app.data.AndroidDatabaseManager;
//import com.felixunlimited.word.app.data.AndroidDatabaseManager;


/**
 * An activity representing a list of Messages. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link MessageDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link UserMessagesListFragment} and the item details
 * (if present) is a {@link MessageDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link UserMessagesListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class UserMessagesListActivity extends AppCompatActivity
        implements UserMessagesListFragment.Callbacks{

    // Debug tag, for logging
    static final String TAG = UserMessagesListActivity.class.getSimpleName();
    private static final String MESSAGEDETAILFRAGMENT_TAG = "MDFTAG";
    private SharedPreferences sharedPreferences;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    // The helper object

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(UserMessagesListActivity.this);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

//        String preferredDir = sharedPreferences.getString(getString(R.string.pref_dir_chooser_key), "internal");
//        if (preferredDir.equals("internal"))
//            if (getExternalFilesDir(null) != null)
//                if (getExternalFilesDir(null).length() > 0)
//                    new Utility.MoveFilesTask (this).execute(getExternalFilesDir(null), getFilesDir());

        if (Utility.isConnected(this)) {
            WordAppSyncAdapter.initializeSyncAdapter(this);
        }
        else
            Toast.makeText(this, "WordApp needs internet connectivity to function properly", Toast.LENGTH_LONG).show();

        if (!sharedPreferences.getBoolean("started", false))
        {
            new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

                @Override
                public void run() {
                    // This method will be executed once the timer is over
                    // Start your app main activity
                    editor.putBoolean("started", true);
                    editor.apply();
                    Intent i = new Intent(UserMessagesListActivity.this, SplashScreenActivity.class);
                    startActivity(i);
                }
            }, 1);
        }


        setContentView(R.layout.activity_message_list);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setupWindowAnimations();

        if (findViewById(R.id.message_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.message_detail_container, new MessageDetailFragment(), MESSAGEDETAILFRAGMENT_TAG)
                        .commit();
            }

        } else {
            mTwoPane = false;
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setElevation(0f);
            }
        }

        UserMessagesListFragment userMessagesListFragment =  ((UserMessagesListFragment)getSupportFragmentManager()
                .findFragmentById(R.id.messages_list));
        userMessagesListFragment.setUseTodayLayout(!mTwoPane);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupWindowAnimations() {
        Fade fade = new Fade();
        fade.setDuration(1000);
        //getWindow().setTr
        getWindow().setEnterTransition(fade);

        Slide slide = new Slide();
        slide.setDuration(1000);
        getWindow().setReturnTransition(slide);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.messagelist, menu);

        // Associate searchable configuration with the SearchView
/*
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
*/
        //searchView.set
//        String[] LOG_PROJECTION = new String[] {
//                MessageContract.LogEntry._ID,
//                MessageContract.LogEntry.COLUMN_TIMESTAMP,
//                MessageContract.LogEntry.COLUMN_USER_ID,
//                MessageContract.LogEntry.COLUMN_EVENT
//        };
//        Uri logUri = MessageContract.MessageEntry.CONTENT_URI;
//
//        // we'll query our contentProvider, as always
//        Cursor cursor = getContentResolver().query(logUri, new String[]{"*"}, null, null, null);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_events:
                startActivity(new Intent(this, EventsActivity.class));
                return true;
            case R.id.action_declarations:
                startActivity(new Intent(this, DeclarationsActivity.class));
                return true;

//        if (id == R.id.action_search) {
//        startSearch(null, false, null, false);
////            startActivity(new Intent(this, MessagesSearchActivity.class));
//            return true;
//        }
//            case R.id.action_show_db:
//                startActivity(new Intent(this, AndroidDatabaseManager.class));
//                return true;
//            case R.id.action_subscriptions:
//                startActivity(new Intent(this, TriviaDrive.class));
//                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
//        registerReceiver(receiver, new IntentFilter("test"));
        Utility.chooseTheme(this);
        super.onResume();
    }


    /**
     * Callback method from {@link UserMessagesListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putParcelable(MessageDetailFragment.MESSAGE_DETAIL_URI, contentUri);

            MessageDetailFragment fragment = new MessageDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.message_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, MessageDetailActivity.class);
            detailIntent.setData(contentUri);

            View sharedView = findViewById(R.id.list_item_preacher_picture);
            String transitionName = "dex";

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                ActivityOptions transitionActivityOptions = ActivityOptions.makeSceneTransitionAnimation(UserMessagesListActivity.this, sharedView, transitionName);
//                 startActivity(detailIntent, transitionActivityOptions.toBundle());
//            }
//            else
                startActivity(detailIntent);
        }
    }
}
