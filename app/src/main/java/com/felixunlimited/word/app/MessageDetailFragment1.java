//package com.felixunlimited.word.app;
//
//import android.content.BroadcastReceiver;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.ServiceConnection;
//import android.content.SharedPreferences;
//import android.database.Cursor;
//import android.media.AudioManager;
//import android.net.Uri;
//import android.net.wifi.WifiManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.IBinder;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.LoaderManager;
//import android.support.v4.content.CursorLoader;
//import android.support.v4.content.Loader;
//import android.support.v4.view.MenuItemCompat;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.widget.ShareActionProvider;
//import android.view.LayoutInflater;
//import android.view.Menu;
//import android.view.MenuInflater;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.animation.Animation;
//import android.view.animation.AnimationUtils;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.SeekBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.felixunlimited.word.app.data.MessageContract;
//import com.felixunlimited.word.app.player.MusicService;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//
///**
// * A fragment representing a single Message detail screen.
// * This fragment is either contained in a {@link UserMessagesListActivity}
// * in two-pane mode (on tablets) or a {@link MessageDetailActivity}
// * on handsets.
// */
//public class MessageDetailFragment1
//        extends Fragment
//        implements LoaderManager.LoaderCallbacks<Cursor>
//        /*MessageDialogFragment.MessageDialogListener*/ {
//
//    private ImageButton mForwardButton;
//    private ImageButton mPlayButton;
//    private ImageButton mPauseButton;
//    private ImageButton mReverseButton;
//    private ImageView mAlbumArt;
//    private double startTime = 0;
//    private double finalTime = 0;
//    private Handler mHandler = new Handler();;
//    private int forwardTime = 5000;
//    private int backwardTime = 5000;
//    private SeekBar mSeekBar;
//    private TextView mStartTime;
//    private TextView mFinalTime;
//    private TextView mMsgTitle;
//    private TextView mDate;
//    private TextView mPreacher;
//    private TextView mPrice;
//    public static int oneTimeOnly = 0;
//    Boolean isGompleted = false;
//    //private Context getContext() = getContext();
//
//    private static final int MESSAGE_DETAIL_LOADER = 0;
//    static final String MESSAGE_DETAIL_URI = "URI";
//    private static final String MESSAGE_SHARE_HASHTAG = " #WordApp";
//    private ShareActionProvider mShareActionProvider;
//    private String mShareText;
//    private Uri mUri;
//    private String mDownloadUrl;
//    private String mDownloadDir;
//    private String mDownloadFile;
//    int mDownloaded = 0;
//    Intent musicServiceIntent;
//    WifiManager.WifiLock wifiLock;
//    //service
//    private MusicService musicSrv;
//    private Intent playIntent;
//    //binding
//    private boolean musicBound=false;
//
//    private static final String[] DETAIL_COLUMNS = {
//            MessageContract.MessageEntry.TABLE_NAME + "." + MessageContract.MessageEntry._ID,
//            MessageContract.MessageEntry.COLUMN_DATE,
//            MessageContract.MessageEntry.COLUMN_TITLE,
//            MessageContract.MessageEntry.COLUMN_PREACHER_KEY,
//            MessageContract.MessageEntry.COLUMN_CATEGORY,
//            MessageContract.MessageEntry.COLUMN_OVERVIEW,
//            MessageContract.MessageEntry.COLUMN_DOWNLOADS,
//            MessageContract.MessageEntry.COLUMN_STREAMS,
//            MessageContract.MessageEntry.COLUMN_PURCHASES,
//            MessageContract.MessageEntry.COLUMN_PREACHER,
//            MessageContract.MessageEntry.COLUMN_PRICE
//    };
//
//    // These indices are tied to DETAIL_COLUMNS.  If DETAIL_COLUMNS changes, these
//    // must change.
//    public static final int COL_MESSAGE_ID =                0;
//    public static final int COL_MESSAGE_DATE =              1;
//    public static final int COL_MESSAGE_TITLE =             2;
//    public static final int COL_MESSAGE_PREACHER_KEY =      3;
//    public static final int COL_MESSAGE_CATEGORY =          4;
//    public static final int COL_MESSAGE_OVERVIEW =          5;
//    public static final int COL_MESSAGE_NO_OF_DOWNLOADS =   6;
//    public static final int COL_MESSAGE_DOWNLOADED =        7;
//    public static final int COL_MESSAGE_PURCHASED =         8;
//    public static final int COL_PREACHER_NAME =             9;
//    public static final int COL_PRICE =                    10;
//
//    /**
//     * Mandatory empty constructor for the fragment manager to instantiate the
//     * fragment (e.g. upon screen orientation changes).
//     */
//    public MessageDetailFragment1() { setHasOptionsMenu(true);
//    }
//    static SharedPreferences sharedPref;
//
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
//
//
//    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Bundle bundle = intent.getExtras();
//            if (bundle != null) {
//                isGompleted = bundle.getBoolean(MusicService.PLAYCOMPLETE);
//                Toast.makeText(getContext(), isGompleted.toString() + " received", Toast.LENGTH_LONG).show();
//            }
//        }
//    };
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(MusicService.NOTIFICATION));
//
//        sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.putBoolean("com.felixiosystems.wordapp.isChecked", false);
//        editor.apply();
//
////        if (musicSrv.player != null) {
////            musicSrv.player.reset();
////            musicSrv.player.release();
//////            if (musicSrv.player.isPlaying())
//////                musicSrv.player.stop();
//////            else
//////                musicSrv.player.release();
////        }
//
//        Bundle arguments = getArguments();
//        if (arguments != null) {
//            mUri = arguments.getParcelable(MessageDetailFragment1.MESSAGE_DETAIL_URI);
//        }
//
//        View rootView = inflater.inflate(R.layout.fragment_message_detail, container, false);
//        mForwardButton = (ImageButton) rootView.findViewById(R.id.forward_button);
//        mPlayButton = (ImageButton) rootView.findViewById(R.id.play_button);
//        mPauseButton = (ImageButton) rootView.findViewById(R.id.pause_button);
//        mReverseButton = (ImageButton) rootView.findViewById(R.id.reverse_button);
//        mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);
//        mStartTime = (TextView) rootView.findViewById(R.id.start_time_view);
//        mFinalTime = (TextView) rootView.findViewById(R.id.final_time_view);
//        mMsgTitle =(TextView) rootView.findViewById(R.id.title_view);
////        mOverview = (TextView) rootView.findViewById(R.id.overview_view);
//        mAlbumArt = (ImageView) rootView.findViewById(R.id.album_art);
//        mSeekBar = (SeekBar) rootView.findViewById(R.id.seek_bar);
//        mDate = (TextView) rootView.findViewById(R.id.date_view);
//        mPreacher = (TextView) rootView.findViewById(R.id.preacher_view);
//        return rootView;
//    }
//
//    //connect to the service
//    private ServiceConnection musicConnection = new ServiceConnection(){
//
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
//            //get service
//            //musicSrv = binder.getService();
//            //pass list
//            musicBound = true;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            musicBound = false;
//        }
//    };
//
//    //start and bind the service when the activity starts
//    @Override
//    public void onStart() {
//        super.onStart();
//        if(playIntent==null){
//            playIntent = new Intent(getContext(), MusicService.class);
//            getContext().bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
//            getContext().startService(playIntent);
//        }
//    }
//
//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        inflater.inflate(R.menu.detailfragment, menu);
//
//        if (mDownloaded == 1)
//        {
//            menu.findItem(R.id.action_download).setVisible(false);
//        }
//
//        // Retrieve the share menu item
//        MenuItem menuItem = menu.findItem(R.id.action_share);
//
//        // Get the provider and hold onto it to set/change the share intent.
//        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
//
//        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
//        if (mShareText != null) {
//            mShareActionProvider.setShareIntent(createShareForecastIntent());
//        }
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_download) {
//            Toast.makeText(getContext(), mDownloadUrl, Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(getContext(), DownloadActivity.class);
//            intent.putExtra(DownloadService.URL, mDownloadUrl);
//            intent.putExtra(DownloadService.DIR, mDownloadDir);
//            intent.putExtra(DownloadService.FILENAME, mDownloadFile);
//            startActivity(intent);
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    private Intent createShareForecastIntent() {
//        Intent shareIntent = new Intent(Intent.ACTION_SEND);
//        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//        shareIntent.setType("text/plain");
//        shareIntent.putExtra(Intent.EXTRA_TEXT, mShareText + MESSAGE_SHARE_HASHTAG);
//        return shareIntent;
//    }
//
//    @Override
//    public void onActivityCreated(Bundle savedInstanceState) {
//        getLoaderManager().initLoader(MESSAGE_DETAIL_LOADER, null, this);
//        super.onActivityCreated(savedInstanceState);
//    }
//
//    private Runnable UpdateSongTime = new Runnable() {
//        public void run() {
//            if (musicSrv.player != null) {
//                if (isGompleted) {
//                    mPauseButton.setVisibility(View.GONE);
//                    mPlayButton.setVisibility(View.VISIBLE);
//                    isGompleted = false;
//                }
//
//                mStartTime.setText(String.format("%d:%d",
//
//                                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
//                                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
//                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
//                                                toMinutes((long) startTime)))
//                );
//                mSeekBar.setProgress((int) startTime);
//                mHandler.postDelayed(this, 100);
//            }
//        }
//    };
//
//    @Override
//    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//        if ( null != mUri ) {
//            // Now create and return a CursorLoader that will take care of
//            // creating a Cursor for the data being displayed.
//            return new CursorLoader(
//                    getActivity(),
//                    mUri,
//                    DETAIL_COLUMNS,
//                    null,
//                    null,
//                    null
//            );
//        }
//        return null;
//    }
//
//    @Override
//    public void onDestroy() {
//        //MusicService.pauseMessage();
//        getActivity().unregisterReceiver(broadcastReceiver);
//        //getContext().stopService(musicServiceIntent);
//        super.onDestroy();
//    }
//
//    @Override
//    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        if (data != null && data.moveToFirst()) {
//
//            final int message_id = data.getInt(COL_MESSAGE_ID);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                getActivity().invalidateOptionsMenu();
//            }
//
//            String date = data.getString(COL_MESSAGE_DATE);
//            final String message_title = data.getString(COL_MESSAGE_TITLE);
//            int preacher_id = data.getInt(COL_MESSAGE_PREACHER_KEY);
//            String downloaded = data.getString(COL_MESSAGE_DOWNLOADED);
//            String category = data.getString(COL_MESSAGE_CATEGORY);
//            String overview = data.getString(COL_MESSAGE_OVERVIEW);
//            String no_of_downloads = data.getString(COL_MESSAGE_NO_OF_DOWNLOADS);
//            final int purchased = data.getInt(COL_MESSAGE_PURCHASED);
//            final String preacher_name = data.getString(COL_PREACHER_NAME);
//            final String price = data.getString(COL_PRICE);
//
//            mShareText = "I'm listening to "+preacher_name+" share a word titled: "+message_title+" on ";
//
//            musicServiceIntent = new Intent(getActivity(), MusicService.class);
//            musicServiceIntent.putExtra("message_title", message_title);
//            musicServiceIntent.putExtra("preacher_name", preacher_name);
//            musicServiceIntent.putExtra("preacher_id", preacher_id);
//            musicServiceIntent.putExtra("message_id", message_id);
//            musicServiceIntent.putExtra("purchased", purchased);
//            musicServiceIntent.putExtra("downloaded", downloaded);
//            getActivity().startService(musicServiceIntent);
//
//            getActivity().setTitle(message_title);
//
//            mMsgTitle.setText(message_title);
//            mMsgTitle.setSelected(true);
//            mDate.setText(date);
//            mPreacher.setText(preacher_name);
//
//            mDownloadUrl = "http://www.felixunlimited.com/messages/"+String.valueOf(message_id)+".mp3";
//            final File dir = getContext().getExternalFilesDir(null);
//            mDownloadDir = dir.getAbsolutePath();
//            mDownloadFile = message_id+".mp3";
//            mDownloaded = data.getInt(COL_MESSAGE_DOWNLOADED);
//            String fileName = "p"+preacher_id+".png";
//            File preacherFile = new File(dir, fileName);
//
//            Utility.setImage(mDownloadDir, fileName, mAlbumArt);
//
//            Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.hyperspace_jump);
//            if (mAlbumArt != null) {
//                mAlbumArt.startAnimation(hyperspaceJumpAnimation);
//            }
////          mAlbumArt.setImageURI(Utility.getPreacherPicUri(preacher_id, getContext()));
//
//            mPlayButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                    if (UpdateSongTime == null)
//                        mHandler.removeCallbacks(UpdateSongTime);
//                    musicSrv.player.reset();
//
//                    File msgFile = new File(dir, message_id+".mp3");
//
//                    if (msgFile.exists())
//                    {
//                        AlertDialog.Builder bld = new AlertDialog.Builder(getContext());
//                        bld.setMessage("You are playing a downloaded file");
//                        bld.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                Toast.makeText(getContext(), "Playing from downloaded file", Toast.LENGTH_LONG).show();
//                                Uri myUri = Utility.getDownloadedMessageUri(message_id, getContext()); // initialize Uri here
//                                musicSrv.player.setAudioStreamType(AudioManager.STREAM_MUSIC);
//                                try {
//                                    musicSrv.player.setDataSource(getContext(), myUri);
//                                    musicSrv.player.prepare();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//
//                                finalTime = musicSrv.getDuration();
//                                mSeekBar.setMax((int) finalTime);
//
//                                startTime = musicSrv.getCurrentPosition();
//
//                                mFinalTime.setText(String.format("%d:%d",
//                                        TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
//                                        TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
//                                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)))
//                                );
//
//                                mStartTime.setText(String.format("%d:%d",
//                                        TimeUnit.MILLISECONDS.toMinutes((long) startTime),
//                                        TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
//                                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) startTime)))
//                                );
//
//                                mSeekBar.setProgress((int) startTime);
//                                mHandler.postDelayed(UpdateSongTime, 100);
//                                mPlayButton.setVisibility(View.GONE);
//                                mPauseButton.setVisibility(View.VISIBLE);
//
//                                wifiLock = ((WifiManager) getContext().getSystemService(Context.WIFI_SERVICE))
//                                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
//
//                                wifiLock.acquire();
//
//                            }
//                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                            }
//                        });
//                        bld.create().show();
//                    }
//                    //paid for check
//                    else if (purchased == 1)
//                    {
//                        new AlertDialog.Builder(getContext())
//                                .setTitle("Stream")
//                                .setMessage("Do you want to stream or download from the internet?")
//                                .setPositiveButton("Stream", new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        String url = "http://felixunlimited.com/messages/"+message_id+".mp3"; // your URL here
//                                        musicSrv.player.setAudioStreamType(AudioManager.STREAM_MUSIC);
//                                        try {
//                                            musicSrv.player.setDataSource(url);
//                                            musicSrv.player.prepare(); // might take long! (for buffering, etc)
//                                        } catch (IOException e) {
//                                            e.printStackTrace();
//                                        }
//                                        Toast.makeText(getContext(), "Streaming", Toast.LENGTH_LONG).show();
//                                        finalTime = musicSrv.getDuration();
//                                        mSeekBar.setMax((int) finalTime);
//
//                                        startTime = musicSrv.getCurrentPosition();
//
//                                        mFinalTime.setText(String.format("%d:%d",
//                                                TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
//                                                TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
//                                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)))
//                                        );
//
//                                        mStartTime.setText(String.format("%d:%d",
//                                                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
//                                                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
//                                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) startTime)))
//                                        );
//
//                                        mSeekBar.setProgress((int) startTime);
//                                        mHandler.postDelayed(UpdateSongTime, 100);
//                                        mPlayButton.setVisibility(View.GONE);
//                                        mPauseButton.setVisibility(View.VISIBLE);
//
//                                        wifiLock = ((WifiManager) getContext().getSystemService(Context.WIFI_SERVICE))
//                                                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
//
//                                        wifiLock.acquire();
//
//                                    }
//                                })
//                                .setNegativeButton("Download", new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        Toast.makeText(getContext(), mDownloadUrl, Toast.LENGTH_SHORT).show();
//                                        Intent intent = new Intent(getContext(), DownloadActivity.class);
//                                        intent.putExtra(DownloadService.URL, mDownloadUrl);
//                                        intent.putExtra(DownloadService.DIR, mDownloadDir);
//                                        intent.putExtra(DownloadService.FILENAME, mDownloadFile);
//                                        startActivity(intent);
//                                        // do nothing
//                                        //onDestroy();
//                                    }
//                                })
//                                .setIcon(android.R.drawable.ic_dialog_alert)
//                                .create()
//                                .show();
//                    }
//                    else
//                    {
//                        AlertDialog.Builder bld = new AlertDialog.Builder(getContext());
//                        bld.setMessage("Do you want to buy the message:\n"+message_title+"\nby:\n"+preacher_name+"?");
//                        bld.setPositiveButton("YES", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                Intent intent = new Intent(getContext(), PurchaseActivity.class);
//                                intent.putExtra("TITLE", message_title);
//                                intent.putExtra("PRICE", price);
//                                startActivity(intent);
//                            }
//                        });
//                        bld.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                            }
//                        });
//                        bld.create().show();
//                    }
//
//                }
//            });
//
//            mPauseButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    musicSrv.pauseSong();
//                    mPauseButton.setVisibility(View.GONE);
//                    mPlayButton.setVisibility(View.VISIBLE);
//                    wifiLock.release();
//                }
//            });
//
//            mForwardButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    int temp = (int) startTime;
//
//                    if ((temp + forwardTime) <= finalTime) {
//                        startTime = startTime + forwardTime;
//                        musicSrv.seekTo((int) startTime);
//                    } else {
//                        Toast.makeText(getActivity(), "Cannot jump forward 5 seconds", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            });
//
//            mReverseButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    int temp = (int) startTime;
//
//                    if ((temp - backwardTime) > 0) {
//                        startTime = startTime - backwardTime;
//                        musicSrv.seekTo((int) startTime);
//                    } else {
//                        Toast.makeText(getActivity(), "Cannot jump backward 5 seconds", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            });
//
//            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//
//                int startSeek, stopSeek, increment;
//
//                @Override
//                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                    startSeek = musicSrv.getCurrentPosition();
//                }
//
//                @Override
//                public void onStartTrackingTouch(SeekBar seekBar) {
//                    startSeek = (int) startTime;
//                }
//
//                @Override
//                public void onStopTrackingTouch(SeekBar seekBar) {
//                    stopSeek = seekBar.getProgress();
//                    increment = stopSeek - startSeek;
//                    startTime = startTime + increment;
//                    musicSrv.seekTo((int) startTime);
//                }
//            });
//
////            mShareText = String.format("%s - %s - %s", message_title, preacher_name, date);
//
//            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
//            if (mShareActionProvider != null) {
//                mShareActionProvider.setShareIntent(createShareForecastIntent());
//            }
//        }
//    }
//
//    @Override
//    public void onLoaderReset(Loader<Cursor> loader) { }
//}