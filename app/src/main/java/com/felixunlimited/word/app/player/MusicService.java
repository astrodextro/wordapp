package com.felixunlimited.word.app.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;

import java.io.IOException;

/*
 * This is demo code to accompany the Mobiletuts+ series:
 * Android SDK: Creating a Music Player
 * 
 * Sue Smith - February 2014
 */

public class MusicService extends Service
		implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener,
		MediaPlayer.OnBufferingUpdateListener,
		MediaPlayer.OnCompletionListener {

	//media player
	public MediaPlayer player;
	//current position
	private int songPosn;
	//binder
	private final IBinder musicBind = new MusicBinder();

	public static final String PLAYCOMPLETE = "playComplete";
	private static String mUrl;
	private static MessagePlayerService mInstance = null;
	public static final String NOTIFICATION = "com.felixunlimited.word.app.service.receiver";

	private static MediaPlayer mMediaPlayer = null;    // The Media Player
	private int mBufferPosition;
	private static String mMessageTitle;
	private static String mPreacherName;
	private static String mSongPicUrl;
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
//	public MusicService() {}

	public MediaPlayer getMediaPlayer() {
		return player;
	}

	public void setMediaPlayer(MediaPlayer mMediaPlayer) {
		player = mMediaPlayer;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mMessageTitle = intent.getStringExtra("message_title");
		mPreacherName = intent.getStringExtra("preacher_name");
		mDownloaded = intent.getIntExtra("downloaded", 0);
		mPurchased = intent.getIntExtra("purchased", 0);
		mMessageID = intent.getIntExtra("message_id", 2);
		mPreacherID = intent.getIntExtra("preacher_id", 2);
		return super.onStartCommand(intent, flags, startId);
	}

	public void onCreate(){
		//create the service
		super.onCreate();
		//initialize position
		songPosn=0;
		//create player
		player = new MediaPlayer();
		//initialize
		initMusicPlayer();
	}

	public void initMusicPlayer(){
		//set player properties
		player.setWakeMode(getApplicationContext(), 
				PowerManager.PARTIAL_WAKE_LOCK);
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		//set listeners
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {

	}

	//binder
	public class MusicBinder extends Binder {
		MusicService getService() { 
			return MusicService.this;
		}
	}

	//activity will bind to service
	@Override
	public IBinder onBind(Intent intent) {
		return musicBind;
	}

	//release resources when unbind
	@Override
	public boolean onUnbind(Intent intent){
		player.stop();
		player.release();
		return false;
	}

	//play a song
	public void playSong(String src){
		//play
		player.reset();

		if (src.startsWith("http"))
		{
			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			try {
				player.setDataSource(src);
				player.prepareAsync(); // might take long! (for buffering, etc)
			} catch (IOException e) {
				e.printStackTrace();
			}
			Toast.makeText(mContext, "Streaming", Toast.LENGTH_LONG).show();
		}

		else
		{
			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			try {
				player.setDataSource(getBaseContext(), Uri.parse(src));
				player.prepare();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		player.prepareAsync();
	}

	//set the song
	public void setSong(int songIndex){
		songPosn=songIndex;	
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		//start playback
		mp.start();
	}

	public int getCurrentPosition()
	{
		return player.getCurrentPosition();
	}

	public int getDuration()
	{
		return player.getDuration();
	}

	public void pauseSong()
	{
		player.pause();
	}

	public void seekTo(int to)
	{
		player.seekTo(to);
	}
}
