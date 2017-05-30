package com.felixunlimited.word.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.felixunlimited.word.app.data.MessageContract;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DeclarationsActivity extends AppCompatActivity
        implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {

    private static final String[] DECLARATIONS_PROJECTION = new String[] {
            MessageContract.DeclarationsEntry._ID,
            MessageContract.DeclarationsEntry.COLUMN_TITLE,
            MessageContract.DeclarationsEntry.COLUMN_DATE,
            MessageContract.DeclarationsEntry.COLUMN_CATEGORY,
            MessageContract.DeclarationsEntry.COLUMN_PREACHER_KEY,
            MessageContract.DeclarationsEntry.COLUMN_DOWNLOADED,
            MessageContract.DeclarationsEntry.COLUMN_TIMESTAMP
    };

    private static final int DECL_ID = 0;
    private static final int DECL_TITLE = 1;
    private static final int DECL_DATE = 2;
    private static final int DECL_CATEGORY = 3;
    private static final int DECL_PREACHER_KEY = 4;
    private static final int DECL_DOWNLOADED = 5;
    private static final int DECL_TIMESTAMP = 6;
    private static final String CURRENT_SCROLL_POSITION = "decl_scroll_position";
    private static final String PLAY_TIME = "decl_play_time";

    ScrollView declScrollView;
    Button bNext, bPrev;
    TextView declTxt, paginationTxt;
    LinearLayout declControl;
    public Cursor declCursor;
    public MediaPlayer declPlayer;
    PowerManager.WakeLock wakeLock;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private double startScrollPosition = 0;
    private double finalScrollPosition = 0;
    private double startTime = 0;
    private double finalTime = 0;
    private Handler mHandler = new Handler();
    private int decl_id = 1;
    boolean toContinue = false;
    // indicates the state our service:
    enum State {
        Idle,
        Initialized,
        Preparing,
        Prepared,
        Started,
        Paused,
        Stopped,
        PlaybackCompleted,
        Error,
        End
    }

    static State mState = State.Idle;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MyMediaPlayer");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        declPlayer = new  MediaPlayer();
        declPlayer.setOnPreparedListener(this);
        declPlayer.setOnCompletionListener(this);
        declPlayer.setOnErrorListener(this);
        mState = State.Idle;

        context = this;
        setContentView(R.layout.activity_declarations);

        declScrollView = (ScrollView) findViewById(R.id.decl_scroll_view);
        bNext = (Button) findViewById(R.id.buttonNext);
        bPrev = (Button) findViewById(R.id.buttonPrev);
        declTxt = (TextView) findViewById(R.id.decl_text);
        paginationTxt = (TextView) findViewById(R.id.pagination_tv);
        declControl = (LinearLayout) findViewById(R.id.decl_control);

        Uri declUri = MessageContract.DeclarationsEntry.CONTENT_URI;

        // we'll query our contentProvider, as always
        declCursor = getContentResolver().query(declUri, DECLARATIONS_PROJECTION, null, null, MessageContract.DeclarationsEntry._ID+" DESC");
        assert declCursor != null;
        declCursor.moveToFirst();
        if (declCursor.getCount() > 0 && (new File(Utility.choosePreferredDir(context).getAbsolutePath()+"/d"+decl_id+".txt")).exists()) {
            paginationTxt.setText((declCursor.getPosition()+1)+" of "+declCursor.getCount());
            setDeclarationsText(declCursor.getInt(DECL_ID));
            bNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    change(true);
                }
            });
            bPrev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    change(false);
                }
            });
        }
        else
            declControl.setVisibility(View.GONE);

//        declScrollView.postDelayed(new Runnable() {
//            public void run() {
//                declScrollView.fullScroll(View.SCROLL_AXIS_VERTICAL);
//            }
//        }, 100L);

//        final Handler handler = new Handler();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {Thread.sleep(100);} catch (InterruptedException e) {}
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        declScrollView.fullScroll(View.FOCUS_DOWN);
//                    }
//                });
//            }
//        }).start();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0f);
        }

        if (savedInstanceState != null) {
            startScrollPosition = savedInstanceState.getInt(CURRENT_SCROLL_POSITION, 0);
            declScrollView.smoothScrollTo(0, (int) startScrollPosition);
            decl_id = savedInstanceState.getInt("decl_id", 1);

            startTime = savedInstanceState.getInt(PLAY_TIME, 0);
            if (mState == State.Idle)
            {
                playDecl();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        onSaveInstanceState();
        declCursor.close();
        mHandler.removeCallbacks(updateScroll);
        if (declPlayer != null)
            declPlayer.release();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_SCROLL_POSITION, declScrollView.getVerticalScrollbarPosition());
        outState.putInt("decl_id", decl_id);
        if (declPlayer != null && mState != State.Error)
        {
            outState.putInt(PLAY_TIME, declPlayer.getCurrentPosition());
            outState.putBoolean("to_continue", true);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        startScrollPosition = savedInstanceState.getInt(CURRENT_SCROLL_POSITION, 0);
        declScrollView.smoothScrollTo(0, (int) startScrollPosition);

        startTime = savedInstanceState.getInt(PLAY_TIME, 0);
        if (mState == State.Idle)
        {
            playDecl();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.decl_menu, menu);
        if (declCursor.getCount() <= 0 || !(new File(Utility.choosePreferredDir(context).getAbsolutePath()+"/d"+decl_id+".mp3")).exists()) {
            menu.clear();
        }
        else {
            if (mState == State.Started)
                menu.findItem(R.id.play_decl).setIcon(android.R.drawable.ic_media_pause);
            else
                menu.findItem(R.id.play_decl).setIcon(android.R.drawable.ic_media_play);

            menu.findItem(R.id.auto_play).setChecked(sharedPreferences.getBoolean("auto_play", false));
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.play_decl) {
            switch(mState){
                case Error:
                    break;
                case Idle:
                    playDecl();
                    item.setIcon(android.R.drawable.ic_media_pause);
                    mState = State.Started;
                    break;
                case Started:
                    declPlayer.pause();
                    item.setIcon(android.R.drawable.ic_media_play);
                    mState = State.Paused;
                    break;
                case Paused:
                    declPlayer.start();
                    item.setIcon(android.R.drawable.ic_media_pause);
                    mState = State.Started;
                    break;
            }
//            invalidateOptionsMenu();
            return true;
        }
        if (id == R.id.auto_play) {
            editor = sharedPreferences.edit();
            if (sharedPreferences.getBoolean("auto_play", false)) {
                editor.putBoolean("auto_play", false);
                item.setChecked(false);
            }
            else {
                editor.putBoolean("auto_play", true);
                item.setChecked(true);
            }
            editor.apply();
            declScrollView.smoothScrollTo(0,0);
            //playDecl();
        }
        return super.onOptionsItemSelected(item);
    }

    public void setDeclarationsText (int id) {
        if (!(new File(Utility.choosePreferredDir(context).getAbsolutePath()+"/d"+decl_id+".txt")).exists())
            return;

        File path = Utility.choosePreferredDir(context);

        //Get the text file
        File file = new File(path.getAbsolutePath(), "d"+id+".txt");

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }

        //Set the text
        declTxt.setText(text.toString());

//        declScrollView.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                declScrollView.fullScroll(View.FOCUS_DOWN);
//            }
//        }, 10000);
//        final Handler handler = new Handler();
//        Timer timer = new Timer();
//        TimerTask doAsynchronousTask = new TimerTask() {
//            @Override
//            public void run() {
//                handler.post(new Runnable() {
//                    public void run() {
//                        declScrollView.smoothScrollTo(0, declScrollView.getChildAt( 0 ).getBottom());
//                    }
//                });
//            }
//        };
//        timer.schedule(doAsynchronousTask, 0, 100000);
    }

    private void change(boolean isNext) {
        declPlayer.reset();
        mState = State.Idle;
        invalidateOptionsMenu();

        if (isNext){
            if (declCursor.isLast())
                declCursor.moveToFirst();
            else
                declCursor.moveToNext();
        }
        else {
            if (declCursor.isFirst())
                declCursor.moveToLast();
            else
                declCursor.moveToPrevious();
        }

        paginationTxt.setText((declCursor.getPosition()+1)+" of "+declCursor.getCount());
        declScrollView.smoothScrollTo(0,0);
        decl_id = declCursor.getInt(DECL_ID);
        setDeclarationsText(decl_id);
        finalScrollPosition = declScrollView.getChildAt(0).getHeight();

        if (sharedPreferences.getBoolean("auto_play", false)) {
            if (!(new File(Utility.choosePreferredDir(context).getAbsolutePath()+"/d"+decl_id+".mp3")).exists())
                change(true);
            else
                playDecl();
        }
    }

    public void playDecl () {
        declScrollView.smoothScrollTo(0,0);
        initDeclPlayer();
    }

    private Runnable updateScroll = new Runnable() {
        public void run() {
            if (mState == State.Started) {

                startTime = declPlayer.getCurrentPosition();
                startScrollPosition = (startTime / finalTime) * finalScrollPosition;
                declScrollView.smoothScrollTo(0, (int) startScrollPosition);
                mHandler.postDelayed(this, 100);
            }
        }
    };

    private void initDeclPlayer()
    {
        String PATH_TO_FILE = Utility.choosePreferredDir(context).getAbsolutePath() + "/d"+decl_id+".mp3";
        if (!(new File(PATH_TO_FILE)).exists()) {
            Toast.makeText(this, "No Such file", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            declPlayer.setDataSource(PATH_TO_FILE);
            mState = State.Initialized;
            declPlayer.prepareAsync();
            mState = State.Preparing;
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            mState = State.Error;
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mState = State.Prepared;
        declPlayer.seekTo((int) startTime);
        declPlayer.start();
        mState = State.Started;
        invalidateOptionsMenu();

        finalTime = declPlayer.getDuration();
        finalScrollPosition = declScrollView.getChildAt(0).getHeight();
        startScrollPosition = (startTime / finalTime) * finalScrollPosition;
        declScrollView.smoothScrollTo(0, (int) startScrollPosition);
        mHandler.postDelayed(updateScroll, 100);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mState = State.PlaybackCompleted;
        mediaPlayer.reset();
        mState = State.Idle;

        if (sharedPreferences.getBoolean("auto_play", false))
            change(true);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        declPlayer.reset();
        mState = State.Idle;
        return false;
    }
}