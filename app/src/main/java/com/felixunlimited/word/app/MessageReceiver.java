//package com.felixunlimited.word.app;
//
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.media.AudioManager;
//import android.media.MediaPlayer;
//import android.os.IBinder;
//import android.os.PowerManager;
//import android.widget.Toast;
//
//public class MessageReceiver extends BroadcastReceiver {
//    public MessageReceiver() {
//    }
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        // TODO: This method is called when the BroadcastReceiver is receiving
//        // an Intent broadcast.
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
//}
//
//package com.felixunlimited.word.app;
//
//        import android.app.Notification;
//        import android.app.NotificationManager;
//        import android.app.PendingIntent;
//        import android.app.Service;
//        import android.content.Context;
//        import android.content.DialogInterface;
//        import android.content.Intent;
//        import android.media.AudioManager;
//        import android.media.MediaPlayer;
//        import android.net.Uri;
//        import android.os.IBinder;
//        import android.os.PowerManager;
//        import android.support.v7.app.AlertDialog;
//        import android.widget.Toast;
//
//        import java.io.IOException;
//
//public class MessagePlayerService extends Service
//        implements MediaPlayer.OnPreparedListener,
//        MediaPlayer.OnErrorListener,
//        MediaPlayer.OnBufferingUpdateListener,
//        MediaPlayer.OnCompletionListener {
//
//    public static final String PLAYCOMPLETE = "playComplete";
//    private static String mUrl;
//    private static MessagePlayerService mInstance = null;
//    public static final String NOTIFICATION = "com.felixunlimited.word.app.service.receiver";
//
//    private static MediaPlayer mMediaPlayer = null;    // The Media Player
//    private int mBufferPosition;
//    private static String mMessageTitle;
//    private static String mPreacherName;
//    private static String mSongPicUrl;
//    private static int mDownloaded;
//    private static int mPurchased;
//    private static int mMessageID;
//    private static int mPreacherID;
//    private static Context mContext;
//
//    NotificationManager mNotificationManager;
//    Notification mNotification = null;
//    final static int NOTIFICATION_ID = 1;
//
//    /**
//     * Creates an IntentService.  Invoked by your subclass's constructor.
//     *
//     */
//    public MessagePlayerService() {}
//
//    public static MediaPlayer getMediaPlayer() {
//        return mMediaPlayer;
//    }
//
//    public static void setMediaPlayer(MediaPlayer mMediaPlayer) {
//        MessagePlayerService.mMediaPlayer = mMediaPlayer;
//    }
//
//    // indicates the state our service:
//    enum State {
//        Retrieving, // the MediaRetriever is retrieving message
//        Stopped, // media player is stopped and not prepared to play
//        Preparing, // media player is preparing...
//        Playing, // playback active (media player ready!). (but the media player may actually be
//        // paused in this state if we don't have audio focus. But we stay in this state
//        // so that we know we have to resume playback once we get focus back)
//        Paused
//        // playback paused (media player ready!)
//    };
//
//    static State mState = State.Retrieving;
//
//    @Override
//    public void onCreate() {
//        mContext = getApplicationContext();
//        mInstance = this;
//        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        mMediaPlayer = new MediaPlayer(); // initialize it here
//        mMessageTitle = intent.getStringExtra("message_title");
//        mPreacherName = intent.getStringExtra("preacher_name");
//        mDownloaded = intent.getIntExtra("downloaded", 0);
//        mPurchased = intent.getIntExtra("purchased", 0);
//        mMessageID = intent.getIntExtra("message_id", 2);
//        mPreacherID = intent.getIntExtra("preacher_id", 2);
//        mMediaPlayer.setOnPreparedListener(this);
//        mMediaPlayer.setOnErrorListener(this);
//        mMediaPlayer.setOnBufferingUpdateListener(this);
//        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        mMediaPlayer.setWakeMode(getBaseContext(), PowerManager.PARTIAL_WAKE_LOCK);
//        mMediaPlayer.setOnCompletionListener(this);
//        initMediaPlayer();
//        return START_STICKY;
//    }
//
//    private static void initMediaPlayer() {
//
//        mState = State.Preparing;
//    }
//
//    public static void pauseMessage() {
//        if (mState.equals(State.Playing)) {
//            mMediaPlayer.pause();
//            mState = State.Paused;
//            updateNotification(mMessageTitle + "(paused)");
//        }
//    }
//
//    public static void startMessage() {
//
//        if (!mState.equals(State.Preparing) && !mState.equals(State.Retrieving)) {
//            mMediaPlayer.start();
//            mState = State.Playing;
//            updateNotification(mMessageTitle + "(playing)");
//        }
//
//        mMediaPlayer.start();
//        mState = State.Playing;
//    }
//
//    public boolean isPlaying() {
//        if (mState.equals(State.Playing)) {
//            return true;
//        }
//        return false;
//    }
//
//    public void restartMessage() {
////        mMediaPlayer.reset();
//        seekMessageTo(0);
//        // Restart message
//    }
//
//    protected void setBufferPosition(int progress) {
//        mBufferPosition = progress;
//    }
//
//    /** Called when MediaPlayer is ready */
//    @Override
//    public void onPrepared(MediaPlayer player) {
//        // Begin playing message
////        startMessage();
//    }
//
//    @Override
//    public boolean onError(MediaPlayer mp, int what, int extra) {
//        // TODO Auto-generated method stub
//        return false;
//    }
//
//    @Override
//    public void onDestroy() {
//        if (mMediaPlayer != null) {
//            mMediaPlayer.release();
//        }
//        mState = State.Retrieving;
//    }
//
//    public static int getMessageDuration() {
//        // Return current message duration
//        return getMediaPlayer().getDuration();
//    }
//
//    public static int getCurrentPosition() {
//        // Return current position
//        return mMediaPlayer.getCurrentPosition();
//    }
//
//    public int getBufferPercentage() {
//        return mBufferPosition;
//    }
//
//    public static void seekMessageTo(int pos) {
//        // Seek message to pos
//        mMediaPlayer.seekTo(pos);
//    }
//
//    public static MessagePlayerService getInstance() {
//        return mInstance;
//    }
//
//    public static void setSong(String url, String title, String songPicUrl) {
//        mUrl = url;
//        mMessageTitle = title;
//        mSongPicUrl = songPicUrl;
//    }
//
//    public String getSongTitle() {
//        return mMessageTitle;
//    }
//
//    public String getSongPicUrl() {
//        return mSongPicUrl;
//    }
//
//    @Override
//    public void onBufferingUpdate(MediaPlayer mp, int percent) {
//        setBufferPosition(percent * getMessageDuration() / 100);
//    }
//
//    /** Updates the notification. */
//    static void updateNotification(String context) {
////        // Notify NotificationManager of new intent
////        //checking the last update and notify if it' the first of the day
////        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
////        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
////        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
////                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));
////
////
////                    int iconId = R.drawable.p0;
////                    Resources resources = context.getResources();
////                    Bitmap largeIcon = BitmapFactory.decodeFile(Utility.getPreacherPicUri(mPreacherID, context).toString());
////                    String title = context.getString(R.string.app_name)+" Player";
////
////                    // Define the text of the message.
////                    String contentText = String.format(context.getString(R.string.format_notification),
////                            "Touch & Agree",
////                            "Mon, 16/06/2016",
////                            "COZA Auditorium");
////
////                    // NotificationCompatBuilder is a very convenient way to build backward-compatible
////                    // notifications.  Just throw in some data.
////                    NotificationCompat.Builder mBuilder =
////                            new NotificationCompat.Builder(context)
////                                    .setColor(resources.getColor(R.color.wordapp_light_orange))
////                                    .setLargeIcon(largeIcon)
////                                    .setContentTitle(mMessageTitle)
////                                    .setContentText(mPreacherName);
////
////
////                    // Make something interesting happen when the user clicks on the notification.
////                    // In this case, opening the app is sufficient.
////                    Intent resultIntent = new Intent(context, UserMessagesListActivity.class);
////
////                    // The stack builder object will contain an artificial back stack for the
////                    // started Activity.
////                    // This ensures that navigating backward from the Activity leads out of
////                    // your application to the Home screen.
////                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
////                    stackBuilder.addNextIntent(resultIntent);
////                    PendingIntent resultPendingIntent =
////                            stackBuilder.getPendingIntent(
////                                    0,
////                                    PendingIntent.FLAG_UPDATE_CURRENT
////                            );
////                    mBuilder.setContentIntent(resultPendingIntent);
////
////                    NotificationManager mNotificationManager =
////                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
////                    // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
////                    mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
////
////                    //refreshing last sync
////                }
////            }
////        }
//    }
//
//    /**
//     * Configures service as a foreground service. A foreground service is a service that's doing something the user is
//     * actively aware of (such as playing message), and must appear to the user as a notification. That's why we create
//     * the notification here.
//     */
//    void setUpAsForeground(String text) {
//        PendingIntent pi =
//                PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), UserMessagesListActivity.class),
//                        PendingIntent.FLAG_UPDATE_CURRENT);
//        mNotification = new Notification();
//        mNotification.tickerText = text;
//        mNotification.icon = R.drawable.ic_skip_white_36dp;
//        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
//        //mNotification.setLatestEventInfo(getApplicationContext(), getResources().getString(R.string.app_name), text, pi);
//        startForeground(NOTIFICATION_ID, mNotification);
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        return null;
//
//        //throw new UnsupportedOperationException("Not yet implemented");
//    }
//
//    public void onCompletion(MediaPlayer _mediaPlayer) {
//        Toast.makeText(getBaseContext(), "Selah!", Toast.LENGTH_LONG).show();
//        // onDestroy();
//        restartMessage();
//        broadcastCompletion(true);
//        //_mediaPlayer.stop();
//        //stopSelf();
//    }
//
//    private void broadcastCompletion(Boolean isComplete) {
//        Intent intent = new Intent(NOTIFICATION);
//        intent.putExtra(PLAYCOMPLETE, isComplete);
//        sendBroadcast(intent);
//    }
//}