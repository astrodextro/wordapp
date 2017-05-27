package com.felixunlimited.word.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.felixunlimited.word.app.data.MessageContract;
import com.felixunlimited.word.app.sync.WordAppSyncAdapter;

/**
 * A list fragment representing a list of Messages. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link MessageDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class UserMessagesListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private MessagesAdapter mMessagesAdapter;
    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUseLatestLayout;
    private static final String SELECTED_KEY = "selected_position";
    private static final int MESSAGES_LIST_LOADER = 0;

    public static final String[] MESSAGES_COLUMNS = {
            MessageContract.MessageEntry.TABLE_NAME + "." + MessageContract.MessageEntry._ID,
            MessageContract.MessageEntry.COLUMN_DATE,
            MessageContract.MessageEntry.COLUMN_TITLE,
            MessageContract.MessageEntry.COLUMN_PREACHER_KEY,
            MessageContract.MessageEntry.COLUMN_CATEGORY,
            MessageContract.MessageEntry.COLUMN_OVERVIEW,
            MessageContract.MessageEntry.COLUMN_DOWNLOADS,
            MessageContract.MessageEntry.COLUMN_PURCHASES,
            MessageContract.MessageEntry.COLUMN_STREAMS,
            MessageContract.MessageEntry.COLUMN_RATING,
            MessageContract.MessageEntry.COLUMN_DOWNLOADED,
            MessageContract.MessageEntry.COLUMN_PURCHASED,
            MessageContract.MessageEntry.COLUMN_PREACHER,
            MessageContract.MessageEntry.COLUMN_PRICE,
            MessageContract.MessageEntry.COLUMN_PAID,
            MessageContract.MessageEntry.COLUMN_TIMESTAMP
    };

    // These indices are tied to DETAIL_COLUMNS.  If DETAIL_COLUMNS changes, these
    // must change.
    public static final int COL_MESSAGE_ID =                0;
    public static final int COL_MESSAGE_DATE =              1;
    public static final int COL_MESSAGE_TITLE =             2;
    public static final int COL_MESSAGE_PREACHER_KEY =      3;
    public static final int COL_MESSAGE_CATEGORY =          4;
    public static final int COL_MESSAGE_OVERVIEW =          5;
    public static final int COL_MESSAGE_DOWNLOADS =         6;
    public static final int COL_MESSAGE_PURCHASES =         7;
    public static final int COL_MESSAGE_STREAMS =           8;
    public static final int COL_MESSAGE_RATING =            9;
    public static final int COL_MESSAGE_DOWNLOADED =        10;
    public static final int COL_MESSAGE_PURCHASED =         11;
    public static final int COL_PREACHER_NAME =             12;
    public static final int COL_PRICE =                     13;
    public static final int COL_PAID =                      14;
    public static final int COL_TIMESTAMP =                 15;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(Uri uri);
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public UserMessagesListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
//        Cursor cursor = getContext().getContentResolver().query(MessageContract.MessageEntry.CONTENT_URI, null, "message_id = 2", null, null);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.messagelistfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateMessage();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateMessage() {
        if (Utility.isConnected(getContext())) {
            WordAppSyncAdapter.syncImmediately(getActivity());
        }
        else
            Toast.makeText(getContext(), "WordApp needs internet connectivity to function properly", Toast.LENGTH_LONG).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The messageAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mMessagesAdapter = new MessagesAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_message_list, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.messages_listview);

       mListView.setAdapter(mMessagesAdapter);
        // We'll call our Messages
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    ((Callbacks) getActivity()).onItemSelected(MessageContract.MessageEntry
                            .buildMessageUriWithDate(cursor.getString(COL_MESSAGE_DATE)));
                }
                mPosition = position;
            }
        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mMessagesAdapter.setUseLatestLayout(mUseLatestLayout);

        return rootView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(MESSAGES_LIST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, filter the query to return messages only for
        // dates after or including today.

        // Sort order:  Ascending, by date.
        String sortOrder = MessageContract.MessageEntry._ID + " DESC";

        Uri messageUri = MessageContract.MessageEntry.CONTENT_URI;
        String[] args = {"yes"};

        return new CursorLoader(
                getActivity(),
                messageUri,
                MESSAGES_COLUMNS,
                MessageContract.MessageEntry.COLUMN_OVERVIEW+" = ? ",
                args,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mMessagesAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mMessagesAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseLatestLayout = useTodayLayout;
        if (mMessagesAdapter != null) {
            mMessagesAdapter.setUseLatestLayout(mUseLatestLayout);
        }
    }
}