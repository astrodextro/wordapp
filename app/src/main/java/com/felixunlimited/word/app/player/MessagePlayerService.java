package com.felixunlimited.word.app.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.ResultReceiver;
import android.widget.Toast;

import com.felixunlimited.word.app.MessageDetailActivity;
import com.felixunlimited.word.app.R;
import com.felixunlimited.word.app.UserMessagesListActivity;
import com.felixunlimited.word.app.Utility;

import org.json.JSONException;

import java.io.IOException;

import static android.media.MediaPlayer.OnBufferingUpdateListener;
import static android.media.MediaPlayer.OnCompletionListener;
import static android.media.MediaPlayer.OnErrorListener;
import static android.media.MediaPlayer.OnInfoListener;
import static android.media.MediaPlayer.OnPreparedListener;
import static com.felixunlimited.word.app.Utility.log;
import static com.felixunlimited.word.app.Utility.updateDB;

public class MessagePlayerService extends Service
        implements OnPreparedListener,
        OnErrorListener,
        OnBufferingUpdateListener,
        OnInfoListener,
        MusicFocusable,
        OnCompletionListener {

    // The tag we put on debug messages
    final static String TAG = "RandomMusicPlayer";
    public static final String PLAYCOMPLETE = "playComplete";
    public static int RESULT_CODE = 1001;
    private static String mUrl;
    private static MessagePlayerService mInstance = null;
    public static final String NOTIFICATION = "com.felixunlimited.word.app.service.receiver";

//    private static MediaPlayer mMediaPlayer = null;    // The Media Player
    private int mBufferPosition;
//    private static String mMessageTitle;
    private static String mPreacherName;
    private static String mMessagePicUrl;
    private static int mDownloaded;
    private static int mPurchased;
    private static int mMessageID;
    private static int mPreacherID;
    private static Context mContext;

    NotificationManager mNotificationManager;
    Notification mNotification = null;
    final static int NOTIFICATION_ID = 1;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public static final String ACTION_TOGGLE_PLAYBACK =
            "com.felixunlimited.word.app.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.felixunlimited.word.app.action.PLAY";
    public static final String ACTION_PAUSE = "com.felixunlimited.word.app.action.PAUSE";
    public static final String ACTION_STOP = "com.felixunlimited.word.app.action.STOP";
    public static final String ACTION_SKIP = "com.felixunlimited.word.app.action.SKIP";
    public static final String ACTION_REWIND = "com.felixunlimited.word.app.action.REWIND";
    public static final String ACTION_URL = "com.felixunlimited.word.app.action.URL";
    public static final String ACTION_PLAY_DOWNLOADED = "com.felixunlimited.word.app.action.PLAY_DOWNLOADED";
    public static final String ACTION_STREAM = "com.felixunlimited.word.app.action.STREAM";

    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;

    // our media player
    static MediaPlayer mPlayer = null;

    public static MediaPlayer getMediaPlayer()
    {
        return mPlayer;
    }

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;
//    AudioManager audioManager = null;

    public MessagePlayerService() {}

    public static void pauseMessage() {
        getMediaPlayer().pause();
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Bundle resultData;
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                resultData = new Bundle();
                resultData.putInt("buffering", 1);
                mReceiver.send(RESULT_CODE, resultData);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                resultData = new Bundle();
                resultData.putInt("buffering", 0);
                mReceiver.send(RESULT_CODE, resultData);
                break;
        }
        return false;
    }

    // indicates the state our service:
    enum State {
        Retrieving, // the MediaRetriever is retrieving message
        Stopped, // media player is stopped and not prepared to play
        Preparing, // media player is preparing...
        Playing, // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused
        // playback paused (media player ready!)
    };

    static State mState = State.Retrieving;

    // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    boolean mStartPlayingAfterRetrieve = false;

    // if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
    // start playing when we are ready. If null, we should play a random song from the device
    Uri mWhatToPlayAfterRetrieve = null;

    enum PauseReason {
        UserRequest,  // paused by user request
        FocusLoss,    // paused because of audio focus loss
    };

    // why did we pause? (only relevant if mState == State.Paused)
    PauseReason mPauseReason = PauseReason.UserRequest;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // title of the song we are currently playing
    String mMessageTitle = "";

    // whether the song we are playing is streaming from the network
    boolean mIsStreaming = false;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClientCompat mRemoteControlClientCompat;

    // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiManager.WifiLock mWifiLock;

    // Dummy album art we will pass to the remote control (if the APIs are available).
    Bitmap mDummyAlbumArt;

    // Our instance of our MusicRetriever, which handles scanning for media and
    // providing titles and URIs as we need.
    MusicRetriever mRetriever;

    // The component name of MusicIntentReceiver, for use with media button and remote control
    // APIs
    ComponentName mMediaButtonReceiverComponent;

    AudioManager mAudioManager;

    NotificationCompat.Builder mNotificationBuilder = null;

    ResultReceiver mReceiver;

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        }
        else
            mPlayer.reset();
    }

    @Override
    public void onCreate() {
        createMediaPlayerIfNeeded();
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)

        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else
            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

        mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);
        mContext = getApplicationContext();
        mInstance = this;
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createMediaPlayerIfNeeded();
        mReceiver = (ResultReceiver) intent.getParcelableExtra("receiver");
        mMessageTitle = intent.getStringExtra("message_title");
        mPreacherName = intent.getStringExtra("preacher_name");
        mDownloaded = intent.getIntExtra("downloaded", 0);
        mPurchased = intent.getIntExtra("purchased", 0);
        mMessageID = intent.getIntExtra("message_id", 2);
        mPreacherID = intent.getIntExtra("preacher_id", 2);
//        String action = intent.getAction();
//        if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
//        else if (action.equals(ACTION_PLAY)) processPlayRequest();
//        else if (action.equals(ACTION_PAUSE)) processPauseRequest();
//        else if (action.equals(ACTION_SKIP)) processSkipRequest();
//        else if (action.equals(ACTION_STOP)) processStopRequest();
//        else if (action.equals(ACTION_REWIND)) processRewindRequest();
//        else if (action.equals(ACTION_URL)) processAddRequest(intent);
//        else if (action.equals(ACTION_PLAY_DOWNLOADED)) processPlayDownloadedRequest();
//        else if (action.equals(ACTION_STREAM)) processStreamRequest();
        if (mDownloaded == 1) processPlayDownloadedRequest();
        else processStreamRequest();
        return START_NOT_STICKY; // Means we started the service, but don't want it to
        // restart in case it's killed.
    }

    void processStreamRequest() {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer

        tryToGetAudioFocus();

        // actually play the song

        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            String url = "http://felixunlimited.com/messages/"+mMessageID+".mp3"; // your URL here
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mPlayer.setDataSource(url);
                mPlayer.prepareAsync();
                Bundle resultData = new Bundle();
                resultData.putInt("prepared", 1);
                mReceiver.send(RESULT_CODE, resultData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            try {
                updateDB(getBaseContext(), "streams", 0, 0, 0, System.currentTimeMillis());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            setUpAsForeground(mMessageTitle + " (playing)");
            configAndStartMediaPlayer();
        }

        // Tell any remote controls that our playback state is 'playing'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
    }

    void processPlayDownloadedRequest() {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer
        tryToGetAudioFocus();

        // actually play the song

        if (mState == State.Stopped) {
            Uri myUri = Utility.getDownloadedMessageUri(mMessageID, mContext); // initialize Uri here
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mPlayer.setDataSource(mContext, myUri);
                mPlayer.prepareAsync();
                Bundle resultData = new Bundle();
                resultData.putInt("prepared", 1);
                mReceiver.send(RESULT_CODE, resultData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mMessageTitle + " (playing)");
            configAndStartMediaPlayer();
        }

        // Tell any remote controls that our playback state is 'playing'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }

    }

    void processPlayDownloadedDeclRequest() {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer
        tryToGetAudioFocus();

        // actually play the song

        if (mState == State.Stopped) {
            Uri myUri = Utility.getDownloadedDeclUri(mMessageID, mContext); // initialize Uri here
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mPlayer.setDataSource(mContext, myUri);
                mPlayer.prepareAsync();
                Bundle resultData = new Bundle();
                resultData.putInt("prepared", 1);
                mReceiver.send(RESULT_CODE, resultData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mMessageTitle + " (playing)");
            configAndStartMediaPlayer();
        }

        // Tell any remote controls that our playback state is 'playing'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }

    }

    void processTogglePlaybackRequest() {
        if (mState == State.Paused || mState == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }

    void processPlayRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, just set the flag to start playing when we're
            // ready
            mWhatToPlayAfterRetrieve = null; // play a random song
            mStartPlayingAfterRetrieve = true;
            return;
        }

        tryToGetAudioFocus();

        // actually play the song

        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextMessage(null);
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mMessageTitle + " (playing)");
            configAndStartMediaPlayer();
        }

        // Tell any remote controls that our playback state is 'playing'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
    }

    void processPauseRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, clear the flag that indicates we should start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }

        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mPlayer.pause();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
        }

        // Tell any remote controls that our playback state is 'paused'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }

    void processRewindRequest() {
        if (mState == State.Playing || mState == State.Paused)
            mPlayer.seekTo(0);
    }

    void processSkipRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            playNextMessage(null);
        }
    }

    void processStopRequest() {
        processStopRequest(false);
    }

    void processStopRequest(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // Tell any remote controls that our playback state is 'paused'.
            if (mRemoteControlClientCompat != null) {
                mRemoteControlClientCompat
                        .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mPlayer.isPlaying()) mPlayer.pause();
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);    // we'll be relatively quiet
        else
            mPlayer.setVolume(1.0f, 1.0f); // we can be loud

        if (!mPlayer.isPlaying()) mPlayer.start();
    }

    public static void resumeMessage()
    {
//        configAndStartMediaPlayer();
    }

    void processAddRequest(Intent intent) {
        // user wants to play a song directly by URL or path. The URL or path comes in the "data"
        // part of the Intent. This Intent is sent by {@link MainActivity} after the user
        // specifies the URL/path via an alert box.
        if (mState == State.Retrieving) {
            // we'll play the requested URL right after we finish retrieving
            mWhatToPlayAfterRetrieve = intent.getData();
            mStartPlayingAfterRetrieve = true;
        }
        else if (mState == State.Playing || mState == State.Paused || mState == State.Stopped) {
            log(TAG, "Playing from URL/path: " + intent.getData().toString(), Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'i', null, mContext);
            tryToGetAudioFocus();
            playNextMessage(intent.getData().toString());
        }
    }

    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    void playNextMessage(String manualUrl) {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer

        try {
            MusicRetriever.Item playingItem = null;
            if (manualUrl != null) {
                // set the source of the media player to a manual URL or path
                createMediaPlayerIfNeeded();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(manualUrl);
                mIsStreaming = manualUrl.startsWith("http:") || manualUrl.startsWith("https:");

                playingItem = new MusicRetriever.Item(0, null, manualUrl, null, 0);
            }
            else {
                mIsStreaming = false; // playing a locally available song

                playingItem = mRetriever.getRandomItem();
                if (playingItem == null) {
                    Toast.makeText(this,
                            "No available music to play. Place some music on your external storage "
                                    + "device (e.g. your SD card) and try again.",
                            Toast.LENGTH_LONG).show();
                    processStopRequest(true); // stop everything!
                    return;
                }

                // set the source of the media player a a content URI
                createMediaPlayerIfNeeded();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(getApplicationContext(), playingItem.getURI());
            }

            mMessageTitle = playingItem.getTitle();

            mState = State.Preparing;
            setUpAsForeground(mMessageTitle + " (loading)");

            // Use the media button APIs (if available) to register ourselves for media button
            // events

            MediaButtonHelper.registerMediaButtonEventReceiverCompat(
                    mAudioManager, mMediaButtonReceiverComponent);

            // Use the remote control APIs (if available) to set the playback state

            if (mRemoteControlClientCompat == null) {
                Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                intent.setComponent(mMediaButtonReceiverComponent);
                mRemoteControlClientCompat = new RemoteControlClientCompat(
                        PendingIntent.getBroadcast(this /*context*/,
                                0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
                RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                        mRemoteControlClientCompat);
            }

            mRemoteControlClientCompat.setPlaybackState(
                    RemoteControlClient.PLAYSTATE_PLAYING);

            mRemoteControlClientCompat.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                            RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP);

            // Update the remote controls
            mRemoteControlClientCompat.editMetadata(true)
                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, playingItem.getArtist())
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, playingItem.getAlbum())
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, playingItem.getTitle())
                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                            playingItem.getDuration())
                    // TODO: fetch real item artwork
                    .putBitmap(
                            RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                            mDummyAlbumArt)
                    .apply();

            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            mPlayer.prepareAsync();

            // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
            // we are *not* streaming, we want to release the lock if we were holding it before.
            if (mIsStreaming) mWifiLock.acquire();
            else if (mWifiLock.isHeld()) mWifiLock.release();
        }
        catch (IOException ex) {
            log(TAG, "IOException playing next song: " + ex.getMessage(), Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'e', null, mContext);
            ex.printStackTrace();
        }
    }

    private static void initMediaPlayer() {

        mState = State.Preparing;
    }

    public boolean isPlaying() {
        if (mState.equals(State.Playing)) {
            return true;
        }
        return false;
    }

    public void restartMessage() {
//        mMediaPlayer.reset();
        seekMessageTo(0);
        // Restart message
    }

    protected void setBufferPosition(int progress) {
        mBufferPosition = progress;
        Bundle resultData = new Bundle();
        resultData.putInt("buffering", mBufferPosition);
        mReceiver.send(RESULT_CODE, resultData);

    }

    /** Called when MediaPlayer is ready */
    @Override
    public void onPrepared(MediaPlayer player) {

        mState = State.Playing;
        setUpAsForeground(mMessageTitle + " (playing)");
        updateNotification(mMessageTitle + " (playing)");
        // publishing the progress....
        Bundle resultData = new Bundle();
        resultData.putInt("duration" ,(int) mPlayer.getDuration());
        mReceiver.send(RESULT_CODE, resultData);
        configAndStartMediaPlayer();
        // Begin playing message
//        startMessage();
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }

    public static int getMessageDuration() {
        // Return current message duration
        return mPlayer.getDuration();
    }

    public static int getCurrentPosition() {
        // Return current position
        return mPlayer.getCurrentPosition();
    }

    public int getBufferPercentage() {
        return mBufferPosition;
    }

    public static void seekMessageTo(int pos) {
        // Seek message to pos
        mPlayer.seekTo(pos);
    }

    public static MessagePlayerService getInstance() {
        return mInstance;
    }

    public void setMessage(String url, String title, String songPicUrl) {
        mUrl = url;
        mMessageTitle = title;
        mMessagePicUrl = songPicUrl;
    }

    public String getMessageTitle() {
        return mMessageTitle;
    }

    public String getMessagePicUrl() {
        return mMessagePicUrl;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        setBufferPosition(percent * getMessageDuration() / 100);
    }

    /** Updates the notification. */
    void updateNotification(String text) {
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
//        stackBuilder.addNextIntent(resultIntent);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MessageDetailActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.setContentText(text)
                    .setContentIntent(pi);

            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing something the user is
     * actively aware of (such as playing message), and must appear to the user as a notification. That's why we create
     * the notification here.
     */
    void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), UserMessagesListActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Build the notification object.
        mNotificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Utility.preacherPictureBitmap(mPreacherID, mContext))
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle("WordAppPlayer")
                .setContentText(text)
                .setContentIntent(pi)
                .setOngoing(true);

        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), "Media player error! Resetting.",
                Toast.LENGTH_SHORT).show();
        log(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra), Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'e', null, mContext);

        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        return true; // true indicates we handled the error
    }

    public void onGainedAudioFocus() {
        Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" :
                "no duck"), Toast.LENGTH_SHORT).show();
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mPlayer != null && mPlayer.isPlaying())
            configAndStartMediaPlayer();
    }

    public void onCompletion(MediaPlayer _mediaPlayer) {
        log(TAG, "Playing complete for message:  " + mMessageTitle + ", by: " + mPreacherName, Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'i', null, mContext);
//        playNextMessage(null);
        // onDestroy();
        restartMessage();
        broadcastCompletion(true);
        Bundle resultData = new Bundle();
        resultData.putInt("complete", 1);
        mReceiver.send(RESULT_CODE, resultData);
//        stopSelf();
        onDestroy();

        //_mediaPlayer.stop();
        //stopSelf();
    }

    private void broadcastCompletion(Boolean isComplete) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(PLAYCOMPLETE, isComplete);
        sendBroadcast(intent);
    }
}