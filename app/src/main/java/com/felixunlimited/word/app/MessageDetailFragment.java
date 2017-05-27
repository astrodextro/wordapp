package com.felixunlimited.word.app;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.os.ResultReceiver;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.felixunlimited.word.app.data.MessageContract;
import com.felixunlimited.word.app.player.MessagePlayerService;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static com.felixunlimited.word.app.player.MessagePlayerService.getMediaPlayer;

/**
 * A fragment representing a single Message detail screen.
 * This fragment is either contained in a {@link UserMessagesListActivity}
 * in two-pane mode (on tablets) or a {@link MessageDetailActivity}
 * on handsets.
 */
public class MessageDetailFragment
        extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>
        /*MessageDialogFragment.MessageDialogListener*/ {

    private static final String PLAY_TIME = "saved_play_time";
    private ImageButton mForwardButton;
    private ImageButton mPlayButton;
    private ImageButton mPauseButton;
    private ImageButton mReverseButton;
    private ImageView mAlbumArt;
    private double startTime = 0;
    private double finalTime = 0;
    private Handler mHandler = new Handler();;
    private int forwardTime = 5000;
    private int backwardTime = 5000;
    private SeekBar mSeekBar;
    private TextView mStartTime;
    private TextView mFinalTime;
    private TextView mMsgTitle;
    private TextView mDate;
    private TextView mPreacher;
    private TextView mPrice;
    public static int oneTimeOnly = 0;
    Boolean isGompleted = false;
    //private Context getContext() = getContext();

    private static final int MESSAGE_DETAIL_LOADER = 0;
    static final String MESSAGE_DETAIL_URI = "URI";
    private static final String MESSAGE_SHARE_HASHTAG = " #WordApp";
    private ShareActionProvider mShareActionProvider;
    private String mShareText;
    private Uri mUri;
    private String mDownloadUrl;
    private String mDownloadDir;
    private String mDownloadFile;
    int mDownloaded = 0;
    Intent messagePlayerServiceIntent;
    WifiManager.WifiLock wifiLock;
    int message_id;

    public static final String[] DETAIL_COLUMNS = {
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
    private Context context;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MessageDetailFragment() { setHasOptionsMenu(true);
    }
    static SharedPreferences sharedPref;

//    private BroadcastReceiver receiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Bundle bundle = intent.getExtras();
//            if (bundle != null) {
//                String string = bundle.getString("hi");
//                Toast.makeText(getContext(), string,
//                        Toast.LENGTH_LONG).show();
//            }
//        }
//    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                isGompleted = bundle.getBoolean(MessagePlayerService.PLAYCOMPLETE);
                mPauseButton.setVisibility(View.GONE);
                mPlayButton.setVisibility(View.VISIBLE);
                mSeekBar.setProgress(0);
//                Toast.makeText(getContext(), isGompleted.toString() + " received", Toast.LENGTH_LONG).show();
            }
        }
    };

    ProgressDialog mProgressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(MessagePlayerService.NOTIFICATION));
        context = getContext();

        sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("com.felixiosystems.wordapp.isChecked", false);
        editor.apply();

        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setMessage("Downloading");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(true);//        Utility.chooseTheme(this);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(100);
//        if (getMediaPlayer() != null) {
//            getMediaPlayer().reset();
//            getMediaPlayer().release();
////            if (getMediaPlayer().isPlaying())
////                getMediaPlayer().stop();
////            else
////                getMediaPlayer().release();
//        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(MessageDetailFragment.MESSAGE_DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_message_detail, container, false);
        mForwardButton = (ImageButton) rootView.findViewById(R.id.forward_button);
        mPlayButton = (ImageButton) rootView.findViewById(R.id.play_button);
        mPauseButton = (ImageButton) rootView.findViewById(R.id.pause_button);
        mReverseButton = (ImageButton) rootView.findViewById(R.id.reverse_button);
        mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);
        mStartTime = (TextView) rootView.findViewById(R.id.start_time_view);
        mFinalTime = (TextView) rootView.findViewById(R.id.final_time_view);
        mMsgTitle =(TextView) rootView.findViewById(R.id.title_view);
        mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);
        mSeekBar = (SeekBar) rootView.findViewById(R.id.seek_bar);
        mDate = (TextView) rootView.findViewById(R.id.date_view);
        mPreacher = (TextView) rootView.findViewById(R.id.preacher_view);

        if (savedInstanceState != null && getMediaPlayer() != null)
        {
            startTime = savedInstanceState.getInt(PLAY_TIME, 0);
            if (getMediaPlayer().isPlaying())
            {
                mSeekBar.setMax(getMediaPlayer().getDuration());
                mSeekBar.setProgress((int) startTime);
                mHandler.postDelayed(UpdateSongTime, 100);
                mPlayButton.setVisibility(View.GONE);
                mPauseButton.setVisibility(View.VISIBLE);
            }
            else
            {
                mSeekBar.setMax(getMediaPlayer().getDuration());
                mSeekBar.setProgress((int) startTime);
                mPauseButton.setVisibility(View.GONE);
                mPlayButton.setVisibility(View.VISIBLE);
            }
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

//        if (mDownloaded == 1)
//        {
//            menu.findItem(R.id.action_download).setVisible(false);
//        }

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mShareText != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
//        if (id == R.id.action_download) {
//            Toast.makeText(getContext(), mDownloadUrl, Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(getContext(), DownloadActivity.class);
//            intent.putExtra(DownloadService.URL, mDownloadUrl);
//            intent.putExtra(DownloadService.DIR, mDownloadDir);
//            intent.putExtra(DownloadService.FILENAME, mDownloadFile);
////            intent.putExtra("messade_id", message_id);
//            startActivity(intent);
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (getMediaPlayer() != null)
        {
            outState.putInt(PLAY_TIME, getMediaPlayer().getCurrentPosition());
            outState.putInt("message_id", message_id);
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mShareText + MESSAGE_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(MESSAGE_DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private Runnable UpdateSongTime = new Runnable() {
        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        public void run() {
            if (getMediaPlayer() != null) {

                startTime = MessagePlayerService.getCurrentPosition();
                mStartTime.setText(String.format("%d:%d",

                                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                toMinutes((long) startTime)))
                );
                mSeekBar.setProgress((int) startTime);
                mHandler.postDelayed(this, 100);
            }
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if ( null != mUri ) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onDestroy() {
        //MessagePlayerService.pauseMessage();
        getActivity().unregisterReceiver(broadcastReceiver);

        // save last played
        sharedPref.edit().putInt("message_id", message_id).apply();

        // save current position
        if (getMediaPlayer() != null)
            sharedPref.edit().putInt(PLAY_TIME, getMediaPlayer().getCurrentPosition()).apply();
        //getContext().stopService(messagePlayerServiceIntent);
        super.onDestroy();
    }

    @SuppressLint("ParcelCreator")
    public class MyResultReceiver extends ResultReceiver {

//        private Receiver mReceiver;

        public MyResultReceiver(Handler handler) {
            super(handler);
            // TODO Auto-generated constructor stub
        }

//        public interface Receiver {
//            public void onReceiveResult(int resultCode, Bundle resultData);
//
//        }

//        public void setReceiver(Receiver receiver) {
//            mReceiver = receiver;
//        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            if (resultCode == MessagePlayerService.RESULT_CODE)
                finalTime = resultData.getInt("duration");
                        mSeekBar.setMax((int) finalTime);
            mFinalTime.setText(String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                    TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)))
            );

            mStartTime.setText(String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                    TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) startTime)))
            );

            mSeekBar.setProgress((int) startTime);
            mHandler.postDelayed(UpdateSongTime, 100);
            mPlayButton.setVisibility(View.GONE);
            mPauseButton.setVisibility(View.VISIBLE);

            if (resultData.getInt("complete") == 1) {
                mPauseButton.setVisibility(View.GONE);
                mPlayButton.setVisibility(View.VISIBLE);
                isGompleted = false;
            }

            if (resultData.getInt("prepared") == 1)
                mProgressDialog.dismiss();
            else
                mProgressDialog.show();

            if (resultData.getInt("buffering") > 0)
            {
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setMessage("Buffering");
                mProgressDialog.setIndeterminate(true);
//                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                //mProgressDialog.setCancelable(true);//        Utility.chooseTheme(this);
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.show();

            }
            else
                mProgressDialog.dismiss();
//            if (mReceiver != null) {
//                mReceiver.onReceiveResult(resultCode, resultData);
//            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {

            message_id = data.getInt(COL_MESSAGE_ID);

            getActivity().invalidateOptionsMenu();

            final String date = data.getString(COL_MESSAGE_DATE);
            final String message_title = data.getString(COL_MESSAGE_TITLE);
            final int preacher_id = data.getInt(COL_MESSAGE_PREACHER_KEY);
            final int downloaded = data.getInt(COL_MESSAGE_DOWNLOADED);
            String category = data.getString(COL_MESSAGE_CATEGORY);
            String overview = data.getString(COL_MESSAGE_OVERVIEW);
            String no_of_downloads = data.getString(COL_MESSAGE_DOWNLOADS);
            final int purchased = data.getInt(COL_MESSAGE_PURCHASED);
            final String preacher_name = data.getString(COL_PREACHER_NAME);
            final String price = data.getString(COL_PRICE);

            mShareText = "I'm listening to "+preacher_name+" share a word titled: "+message_title+" on ";

            getActivity().setTitle(message_title);

            mMsgTitle.setText(message_title);
            mMsgTitle.setSelected(true);
            mDate.setText(date);
            mPreacher.setText(preacher_name);

            mDownloadUrl = "http://www.felixunlimited.com/messages/"+String.valueOf(message_id)+".mp3";
            final File dir = Utility.choosePreferredDir(context);
            mDownloadDir = dir.getAbsolutePath();
            mDownloadFile = message_id+".mp3";
            mDownloaded = data.getInt(COL_MESSAGE_DOWNLOADED);
            String fileName = "p"+preacher_id+".png";
            File preacherFile = new File(dir, fileName);

            Utility.setImage(mDownloadDir, fileName, mAlbumArt);

//            Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.hyperspace_jump);
//            if (mAlbumArt != null) {
//                mAlbumArt.startAnimation(hyperspaceJumpAnimation);
//            }
            Animation zoomAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.zoom);
            if (mAlbumArt != null) {
                mAlbumArt.startAnimation(zoomAnimation);
            }

            if (sharedPref.getInt("message_id", 0) == message_id && getMediaPlayer() != null)
            {
                startTime = sharedPref.getInt(PLAY_TIME, 0);
                if (getMediaPlayer().isPlaying())
                {
                    mSeekBar.setMax(getMediaPlayer().getDuration());
                    mSeekBar.setProgress((int) startTime);
                    mHandler.postDelayed(UpdateSongTime, 100);
                    mPlayButton.setVisibility(View.GONE);
                    mPauseButton.setVisibility(View.VISIBLE);
                }
                else
                {
                    mSeekBar.setMax(getMediaPlayer().getDuration());
                    mSeekBar.setProgress((int) startTime);
                    mPauseButton.setVisibility(View.GONE);
                    mPlayButton.setVisibility(View.VISIBLE);
                }
            }

            mPlayButton.setOnClickListener(new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.KITKAT)
                @Override
                public void onClick(View v) {

                    if (UpdateSongTime == null)
                        mHandler.removeCallbacks(UpdateSongTime);
//                    getMediaPlayer().reset();

                    File msgFile = new File(dir, message_id+".mp3");

                    if (msgFile.exists() && downloaded == 1)
                    {
                        if (getMediaPlayer()  == null)
                        {
                            messagePlayerServiceIntent = new Intent(getContext(), MessagePlayerService.class);
                            messagePlayerServiceIntent.putExtra("receiver", new MyResultReceiver(new Handler()));
                            messagePlayerServiceIntent.putExtra("message_title", message_title);
                            messagePlayerServiceIntent.putExtra("preacher_name", preacher_name);
                            messagePlayerServiceIntent.putExtra("preacher_id", preacher_id);
                            messagePlayerServiceIntent.putExtra("message_id", message_id);
                            messagePlayerServiceIntent.putExtra("purchased", purchased);
                            messagePlayerServiceIntent.putExtra("downloaded", downloaded);
                            messagePlayerServiceIntent.putExtra(PLAY_TIME, startTime);
                            getActivity().startService(messagePlayerServiceIntent);
                        }
                        else
                        {
 //                           MessagePlayerService.resumeMessage();
                            getMediaPlayer().start();
                            mPauseButton.setVisibility(View.VISIBLE);
                            mPlayButton.setVisibility(View.GONE);
                        }
//                        MessagePlayerService.startMessage();

//                        finalTime = MessagePlayerService.getMessageDuration();
//                        mSeekBar.setMax((int) finalTime);
//
//                        startTime = MessagePlayerService.getCurrentPosition();
                    }
                    //paid for check
                    else if (purchased == 1)
                    {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Stream")
                                .setMessage("Do you want to stream or download from the internet?")
                                .setPositiveButton("Stream", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
//                                        MessagePlayerService.startMessage();
                                        if (getMediaPlayer()  == null)
                                        {
                                            Toast.makeText(getContext(), "Streaming", Toast.LENGTH_LONG).show();

                                            messagePlayerServiceIntent = new Intent(getContext(),MessagePlayerService.class);
                                            messagePlayerServiceIntent.putExtra("receiver", new MyResultReceiver(new Handler()));
                                            messagePlayerServiceIntent.putExtra("message_title", message_title);
                                            messagePlayerServiceIntent.putExtra("preacher_name", preacher_name);
                                            messagePlayerServiceIntent.putExtra("preacher_id", preacher_id);
                                            messagePlayerServiceIntent.putExtra("message_id", message_id);
                                            messagePlayerServiceIntent.putExtra("purchased", purchased);
                                            messagePlayerServiceIntent.putExtra("downloaded", downloaded);
                                            getActivity().startService(messagePlayerServiceIntent);
                                        }
                                        else
                                        {
                                            //                           MessagePlayerService.resumeMessage();
                                            getMediaPlayer().start();
                                            mPauseButton.setVisibility(View.VISIBLE);
                                            mPlayButton.setVisibility(View.GONE);
                                        }
                                    }
                                })
                                .setNegativeButton("Download", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(getContext(), mDownloadUrl, Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(getContext(), DownloadActivity.class);
                                        intent.putExtra(DownloadService.URL, mDownloadUrl);
                                        intent.putExtra(DownloadService.DIR, mDownloadDir);
                                        intent.putExtra("title", message_title);
                                        intent.putExtra("preacher", preacher_name);
                                        intent.putExtra("date", date);
                                        intent.putExtra("cost", price);
                                        intent.putExtra("message_id", message_id);
                                        intent.putExtra(DownloadService.FILENAME, mDownloadFile);
                                        startActivity(intent);
                                        // do nothing
                                        //onDestroy();
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .create()
                                .show();
                    }
                    else
                    {
                        AlertDialog.Builder bld = new AlertDialog.Builder(getContext());
                        bld.setMessage("Do you want to buy the message:\n"+message_title+"\nby:\n"+preacher_name+"?");
                        bld.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(getContext(), PurchaseActivity.class);
                                intent.putExtra("TITLE", message_title);
                                intent.putExtra("PREACHER", preacher_name);
                                intent.putExtra("ID", message_id);
                                intent.putExtra("DATE", date);
                                intent.putExtra("PRICE", price);
                                startActivity(intent);
                            }
                        });
                        bld.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                        bld.create().show();
                    }
                }
            });

            mPauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    messagePlayerServiceIntent = new Intent(MessagePlayerService.ACTION_PAUSE);
//                    messagePlayerServiceIntent.putExtra("message_title", message_title);
//                    messagePlayerServiceIntent.putExtra("preacher_name", preacher_name);
//                    messagePlayerServiceIntent.putExtra("preacher_id", preacher_id);
//                    messagePlayerServiceIntent.putExtra("message_id", message_id);
//                    messagePlayerServiceIntent.putExtra("purchased", purchased);
//                    messagePlayerServiceIntent.putExtra("downloaded", downloaded);
//                    getActivity().startService(messagePlayerServiceIntent);

                    MessagePlayerService.pauseMessage();
                    mPauseButton.setVisibility(View.GONE);
                    mPlayButton.setVisibility(View.VISIBLE);
                }
            });

            mForwardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int temp = (int) startTime;

                    if ((temp + forwardTime) <= finalTime && getMediaPlayer() != null) {
                        startTime = startTime + forwardTime;
                        MessagePlayerService.seekMessageTo((int) startTime);
                    } else {
                        Toast.makeText(getActivity(), "Cannot jump forward 5 seconds", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mReverseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int temp = (int) startTime;

                    if ((temp - backwardTime) > 0 && getMediaPlayer() != null) {
                        startTime = startTime - backwardTime;
                        MessagePlayerService.seekMessageTo((int) startTime);
                    } else {
                        Toast.makeText(getActivity(), "Cannot jump backward 5 seconds", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                int startSeek, stopSeek, increment;

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (getMediaPlayer() != null)
                        startSeek = MessagePlayerService.getCurrentPosition();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    startSeek = (int) startTime;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    stopSeek = seekBar.getProgress();
                    increment = stopSeek - startSeek;
                    startTime = startTime + increment;
                    if (getMediaPlayer() != null)
                        MessagePlayerService.seekMessageTo((int) startTime);
                }
            });

            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }
}