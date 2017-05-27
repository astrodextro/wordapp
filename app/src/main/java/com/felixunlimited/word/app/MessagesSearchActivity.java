package com.felixunlimited.word.app;

import android.app.Activity;

public class MessagesSearchActivity extends Activity
{

}
//
//import android.app.SearchManager;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.net.Uri;
//import android.os.Bundle;
//import android.preference.PreferenceManager;
//import android.support.v7.app.ActionBar;
//import android.support.v7.app.AppCompatActivity;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.widget.Toast;
//
//import com.felixunlimited.word.app.data.AndroidDatabaseManager;
//
//
///**
// * An activity representing a list of Messages. This activity
// * has different presentations for handset and tablet-size devices. On
// * handsets, the activity presents a list of items, which when touched,
// * lead to a {@link MessageDetailActivity} representing
// * item details. On tablets, the activity presents the list of items and
// * item details side-by-side using two vertical panes.
// * <p/>
// * The activity makes heavy use of fragments. The list of items is a
// * {@link UserMessagesListFragment} and the item details
// * (if present) is a {@link MessageDetailFragment1}.
// * <p/>
// * This activity also implements the required
// * {@link UserMessagesListFragment.Callbacks} interface
// * to listen for item selections.
// */
//public class MessagesSearchActivity extends AppCompatActivity
//        implements MessagesSearchFragment.Callbacks{
//
//    int Utility.getUniquePsuedoID();
//    // Debug tag, for logging
//    static final String TAG = UserMessagesListActivity.class.getSimpleName();
//    private static final String MESSAGEDETAILFRAGMENT_TAG = "MDFTAG";
//    private SharedPreferences sharedPreferences;
//
//    /**
//     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
//     * device.
//     */
//    private boolean mTwoPane;
//    public int mCount;
//    public int mStart;
//    public int mEnd;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MessagesSearchActivity.this);
//        final SharedPreferences.Editor editor = sharedPreferences.edit();
//        setContentView(R.layout.activity_message_list);
//
//// Get the intent, verify the action and get the query
////        Intent intent = getIntent();
////        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
////            String query = intent.getStringExtra(SearchManager.QUERY);
////            Toast.makeText(MessagesSearchActivity.this, query, Toast.LENGTH_LONG).show();
////        }
//
//        handleIntent(getIntent());
//        if (findViewById(R.id.message_detail_container) != null) {
//            // The detail container view will be present only in the
//            // large-screen layouts (res/values-large and
//            // res/values-sw600dp). If this view is present, then the
//            // activity should be in two-pane mode.
//            mTwoPane = true;
//
//            if (savedInstanceState == null) {
//                getSupportFragmentManager().beginTransaction()
//                        .replace(R.id.message_detail_container, new MessageDetailFragment1(), MESSAGEDETAILFRAGMENT_TAG)
//                        .commit();
//            }
//
//        } else {
//            mTwoPane = false;
//            ActionBar actionBar = getSupportActionBar();
//            if (actionBar != null) {
//                actionBar.setElevation(0f);
//            }
//        }
//
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//
//        handleIntent(intent);
//    }
//
//    private void handleIntent(Intent intent) {
//
//        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
//            String query = intent.getStringExtra(SearchManager.QUERY);
//            //use the query to search your data somehow
//            Toast.makeText(MessagesSearchActivity.this, query, Toast.LENGTH_LONG).show();
//        }
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.messagelist, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            startActivity(new Intent(this, AndroidDatabaseManager.class));
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//    }
//
//
//    /**
//     * Callback method from {@link UserMessagesListFragment.Callbacks}
//     * indicating that the item with the given ID was selected.
//     */
//    @Override
//    public void onItemSelected(Uri contentUri) {
//        if (mTwoPane) {
//            // In two-pane mode, show the detail view in this activity by
//            // adding or replacing the detail fragment using a
//            // fragment transaction.
//            Bundle arguments = new Bundle();
//            arguments.putParcelable(MessageDetailFragment1.MESSAGE_DETAIL_URI, contentUri);
//
//            MessageDetailFragment1 fragment = new MessageDetailFragment1();
//            fragment.setArguments(arguments);
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.message_detail_container, fragment)
//                    .commit();
//
//        } else {
//            // In single-pane mode, simply start the detail activity
//            // for the selected item ID.
//            Intent detailIntent = new Intent(this, MessageDetailActivity.class);
//            detailIntent.setData(contentUri);
//            startActivity(detailIntent);
//        }
//    }
//
//}
