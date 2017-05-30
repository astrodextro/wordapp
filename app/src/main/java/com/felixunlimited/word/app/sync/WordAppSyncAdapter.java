package com.felixunlimited.word.app.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.felixunlimited.word.app.DeclarationsActivity;
import com.felixunlimited.word.app.EventsActivity;
import com.felixunlimited.word.app.R;
import com.felixunlimited.word.app.Utility;
import com.felixunlimited.word.app.data.MessageContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Vector;

import static com.felixunlimited.word.app.Utility.log;
import static com.felixunlimited.word.app.Utility.updateDB;

public class WordAppSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = WordAppSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the message, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180 * 60;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int EVENTS_NOTIFICATION_ID = 3004;
    private static final int DECL_NOTIFICATION_ID = 4004;

    private static final String[] NOTIFY_EVENTS_PROJECTION = new String[] {
            MessageContract.EventsEntry._ID,
            MessageContract.EventsEntry.COLUMN_DATE,
            MessageContract.EventsEntry.COLUMN_TIME,
            MessageContract.EventsEntry.COLUMN_TITLE,
            MessageContract.EventsEntry.COLUMN_VENUE
    };
    // these indices must match the projection
    private static final int EVENT_ID = 0;
    private static final int EVENT_DATE = 1;
    private static final int EVENT_TIME = 2;
    private static final int EVENT_TITLE = 3;
    private static final int EVENT_VENUE = 4;

    private static final String[] NOTIFY_DECLARATIONS_PROJECTION = new String[] {
            MessageContract.DeclarationsEntry._ID,
            MessageContract.DeclarationsEntry.COLUMN_DATE,
            MessageContract.DeclarationsEntry.COLUMN_TITLE
    };
    // these indices must match the projection
    private static final int DECLARATIONS_ID = 0;
    private static final int DECLARATIONS_DATE = 1;
    private static final int DECLARATIONS_TITLE = 2;

    private static final String[] LOG_PROJECTION = new String[] {
            MessageContract.LogEntry._ID,
            MessageContract.LogEntry.COLUMN_TIMESTAMP,
            MessageContract.LogEntry.COLUMN_USER_ID,
            MessageContract.LogEntry.COLUMN_EVENT
    };
    // these indices must match the projection
    private static final int LOG_ID = 0;
    private static final int LOG_TIMESTAMP = 1;
    private static final int LOG_USER_ID = 2;
    private static final int LOG_EVENT = 3;

    public static final String WEBSERVICE_URL =
            "http://www.felixunlimited.com/webservice.php";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public WordAppSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        context = getContext();
        log(LOG_TAG, "Starting sync", Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, context);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        JSONParser jsonParser = new JSONParser();
        if (Utility.isConnected(context)) {
            //notifyEvent();
            try {
                if (!sharedPreferences.getBoolean("userCreated", false)) {
                    createUser();
                }
                JSONObject msgJsonObject, prchrJsonObject, eventsJsonObject, declJsonObject;
                
                msgJsonObject = jsonParser.makeHttpRequest(WEBSERVICE_URL, "POST", Utility.getUniquePsuedoID(), "messages");
                if(msgJsonObject != null)
                    getMessageDataFromJson(msgJsonObject.toString());
                else
                    onSyncCanceled();

                prchrJsonObject = jsonParser.makeHttpRequest(WEBSERVICE_URL, "POST", "o", "preachers");
                if (prchrJsonObject != null)
                    getPreacherDataFromJson(prchrJsonObject.toString());
                else
                    onSyncCanceled();

                eventsJsonObject = jsonParser.makeHttpRequest(WEBSERVICE_URL, "POST", "o", "events");
                if (eventsJsonObject != null)
                    getEventsDataFromJson(eventsJsonObject.toString());
                else
                    onSyncCanceled();

                declJsonObject = jsonParser.makeHttpRequest(WEBSERVICE_URL, "POST", "o", "declarations");
                if (declJsonObject != null)
                    getDeclarationsDataFromJson(declJsonObject.toString());
                else
                    onSyncCanceled();

                clearLog();
                updateAll();
//                notifyEvent();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else
            onSyncCanceled();

    }

    private void updateAll() throws JSONException {
        updateDB(context, "sync", 0, 0, 0, System.currentTimeMillis());
    }

    private void createUser(){
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = new JSONObject();
        String response = "no";
        try {
            jsonObject.put("user_id", Utility.getUserID(context));
            jsonObject.put("email", Utility.getEmail(context));
            JSONObject resp = jsonParser.makeHttpRequest(WEBSERVICE_URL, "POST", jsonObject.toString(), "create");
            if (resp == null)
                onSyncCanceled();
            else
                response = resp.getString("response");
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        if (response.equals("ok")) {
            log("WordAppSyncAdapter", "User created", Utility.getUserID(context), System.currentTimeMillis(), 'i', null, context);
            editor = sharedPreferences.edit();
            editor.putBoolean("userCreated", true);
            editor.apply();
        }
        else
            log("WordAppSyncAdapter", "User not created", Utility.getUserID(context), System.currentTimeMillis(), 'i', null, context);
    }

    /**
     * Take the String representing the complete message in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getMessageDataFromJson(String messageJsonStr)
            throws JSONException {

        // Now we have a String representing the complete message in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.


        // Message information.  Each day's message info is an element of the "list" array.
        final String RESPONSE = "response";
        final String WA_MESSAGE_ID = "id";
        final String WA_TITLE = "title";
        final String WA_PREACHER = "preacher";
        final String WA_PREACHER_KEY = "preacher_id";
        final String WA_CATEGORY = "category";
        final String WA_OVERVIEW = "overview";
        final String WA_DATE = "date";
        final String WA_DOWNLOADS = "downloads";
        final String WA_PURCHASES = "purchases";
        final String WA_STREAMS = "ustreams";
        final String WA_RATING = "urating";
        final String WA_PURCHASED = "purchased";
        final String WA_DOWNLOADED = "downloaded";
        final String WA_PRICE = "price";

        try {
            JSONObject messageJson = new JSONObject(messageJsonStr);
            JSONArray messageArray = messageJson.getJSONArray(RESPONSE);

            // Insert the new message information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(messageArray.length());

            for(int i = 0; i < messageArray.length(); i++) {
                // These are the values that will be collected.
                String title;
                String preacher;
                String category;
                String overview;

                String no_of_downloads;
                int preacher_key;
                int messageId;

                // Get the JSON object representing the day
                JSONObject message = messageArray.getJSONObject(i);

                title = message.getString(WA_TITLE);
                preacher = message.getString(WA_PREACHER);
                category = message.getString(WA_CATEGORY);
                overview = message.getString(WA_OVERVIEW);
                no_of_downloads = message.getString(WA_DOWNLOADS);
                messageId = message.getInt(WA_MESSAGE_ID);
                preacher_key = message.getInt(WA_PREACHER_KEY);
                String date = message.getString(WA_DATE);
                int price = message.getInt(WA_PRICE);
                int purchases = message.getInt(WA_PURCHASES);
//                int purchased = message.getInt(WA_PURCHASED);
                int paid = 0;
                int purchased = 0;
                int downloaded = 0;
                int streams = 0;
                int rating = 0;

                if (message.get(WA_STREAMS) == null) {
                    streams = message.getInt(WA_STREAMS);
                    rating = message.getInt(WA_RATING);
                    paid = message.getInt("paid");
                    downloaded = message.getInt(WA_DOWNLOADED);
                    if (paid == price)
                        purchased = 1;
                }

                ContentValues messageValues = new ContentValues();

                messageValues.put(MessageContract.MessageEntry._ID, messageId);
                messageValues.put(MessageContract.MessageEntry.COLUMN_DATE, date);
                messageValues.put(MessageContract.MessageEntry.COLUMN_PREACHER_KEY, preacher_key);
                messageValues.put(MessageContract.MessageEntry.COLUMN_PREACHER, preacher);
                messageValues.put(MessageContract.MessageEntry.COLUMN_TITLE, title);
                messageValues.put(MessageContract.MessageEntry.COLUMN_CATEGORY, category);
                messageValues.put(MessageContract.MessageEntry.COLUMN_OVERVIEW, overview);
                messageValues.put(MessageContract.MessageEntry.COLUMN_DOWNLOADS, no_of_downloads);
                messageValues.put(MessageContract.MessageEntry.COLUMN_DOWNLOADED, downloaded);
                messageValues.put(MessageContract.MessageEntry.COLUMN_PURCHASED, purchased);
                messageValues.put(MessageContract.MessageEntry.COLUMN_PRICE, price);
                messageValues.put(MessageContract.MessageEntry.COLUMN_PAID, paid);
                messageValues.put(MessageContract.MessageEntry.COLUMN_PURCHASES, purchases);
                messageValues.put(MessageContract.MessageEntry.COLUMN_STREAMS, streams);
                messageValues.put(MessageContract.MessageEntry.COLUMN_RATING, rating);

                cVVector.add(messageValues);
            }

            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                context.getContentResolver().bulkInsert(MessageContract.MessageEntry.CONTENT_URI, cvArray);

                //notifyEvent();
            }

            log(LOG_TAG, "Messages Sync Complete. " + cVVector.size() + " Inserted", Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, context);

        } catch (JSONException e) {
            log(LOG_TAG, e.getMessage(), Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'e', e, context);
            e.printStackTrace();
        }
    }

    private void getPreacherDataFromJson(String preacherJsonStr)
            throws JSONException
    {
        final String RESPONSE = "response";
        final String WA_ID = "id";
        final String WA_NAME = "name";
        final String WA_BIRTHDAY = "birthday";
        final String WA_MINISTRY = "ministry";
        final String WA_NO_OF_MESSAGES = "no_of_messages";
        final String WA_PURCHASES = "purchases";
        final String WA_STREAMS = "streams";
        final String WA_DOWNLOADS = "downloads";
        final String WA_INT_EXT = "int_ext";

        try {
            JSONObject preacherJson = new JSONObject(preacherJsonStr);
            JSONArray preacherArray = preacherJson.getJSONArray(RESPONSE);

            // Insert the new message information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(preacherArray.length());

            int j = 0;
            for(int i = 0; i < preacherArray.length(); i++) {
                // These are the values that will be collected.
                int preacher_id;
                String birthday;
                String name;
                String ministry;
                String int_ext;
                int no_of_messages;
                int no_of_purchased;
                int no_of_downloaded;
                int no_of_streamed;

                // Get the JSON object representing the day
                JSONObject preacher = preacherArray.getJSONObject(i);

                preacher_id = preacher.getInt(WA_ID);
                birthday = preacher.getString(WA_BIRTHDAY);
                name = preacher.getString(WA_NAME);
                ministry = preacher.getString(WA_MINISTRY);
                int_ext = preacher.getString(WA_INT_EXT);
                no_of_messages = preacher.getInt(WA_NO_OF_MESSAGES);
                no_of_purchased = preacher.getInt(WA_PURCHASES);
                no_of_streamed = preacher.getInt(WA_STREAMS);
                no_of_downloaded = preacher.getInt(WA_DOWNLOADS);

                ContentValues preacherValues = new ContentValues();

                preacherValues.put(MessageContract.PreachersEntry._ID, preacher_id);
                preacherValues.put(MessageContract.PreachersEntry.COLUMN_NAME, name);
                preacherValues.put(MessageContract.PreachersEntry.COLUMN_BIRTHDAY, birthday);
                preacherValues.put(MessageContract.PreachersEntry.COLUMN_MINISTRY, ministry);
                preacherValues.put(MessageContract.PreachersEntry.COLUMN_INTERNAL_EXTERNAL, int_ext);
                preacherValues.put(MessageContract.PreachersEntry.COLUMN_NO_OF_MESSAGES, no_of_messages);
                preacherValues.put(MessageContract.PreachersEntry.COLUMN_PURCHASES, no_of_purchased);
                preacherValues.put(MessageContract.PreachersEntry.COLUMN_STREAMS, no_of_streamed);
                preacherValues.put(MessageContract.PreachersEntry.COLUMN_DOWNLOADS, no_of_downloaded);

                if (context.getContentResolver().insert(MessageContract.PreachersEntry.CONTENT_URI, preacherValues) != null)
                {
                    File dir = Utility.choosePreferredDir(context);
                    if (!dir.exists())
                        dir.mkdirs();

                    String preachersDir = dir.getAbsolutePath();
                    String fileName = "p"+preacher_id+".png";

                    File preacherFile = new File(preachersDir, fileName);
                    if (!preacherFile.exists())
                    {
                        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        Uri uri = Uri.parse("http://www.felixunlimited.com/preachers/"+fileName);
                        DownloadManager.Request req=new DownloadManager.Request(uri);

                        req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                                | DownloadManager.Request.NETWORK_MOBILE)
                                .setAllowedOverRoaming(false)
                                .setVisibleInDownloadsUi(false)
//                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                                .setTitle("WordApp Syncing")
                                .setDescription("This will just take a while")
//                                .setDestinationUri(Uri.fromFile(Utility.chooseFile(context, fileName)));
                                .setDestinationInExternalFilesDir(context, null, fileName);
                        //Enqueue a new download and same the referenceId
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
                        }
                        long downloadReference = downloadManager.enqueue(req);

                    }
                    //notifyEvent();
                    j++;
                }
            }

            log(LOG_TAG, "Preacher Sync Complete. " + j +  " Inserted", Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, context);

        } catch (JSONException e) {
            log(LOG_TAG, e.getMessage(), Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'e', null, context);
            e.printStackTrace();
        }
    }

    private void getEventsDataFromJson(String eventsJsonStr)
            throws JSONException
    {
        final String RESPONSE = "response";

        try {
            JSONObject eventsJson = new JSONObject(eventsJsonStr);
            JSONArray eventsArray = eventsJson.getJSONArray(RESPONSE);

            // Insert the new message information into the database
            int j = 0;
            for(int i = 0; i < eventsArray.length(); i++) {
                // These are the values that will be collected.
                // Get the JSON object representing the day
                JSONObject event = eventsArray.getJSONObject(i);

                ContentValues eventValues = new ContentValues();

                eventValues.put(MessageContract.EventsEntry._ID, event.getInt("id"));
                eventValues.put(MessageContract.EventsEntry.COLUMN_DATE, event.getString(MessageContract.EventsEntry.COLUMN_DATE));
                eventValues.put(MessageContract.EventsEntry.COLUMN_TIME, event.getString(MessageContract.EventsEntry.COLUMN_TIME));
                eventValues.put(MessageContract.EventsEntry.COLUMN_TITLE, event.getString(MessageContract.EventsEntry.COLUMN_TITLE));
                eventValues.put(MessageContract.EventsEntry.COLUMN_VENUE, event.getString(MessageContract.EventsEntry.COLUMN_VENUE));
                eventValues.put(MessageContract.EventsEntry.COLUMN_DESCRIPTION, event.getString(MessageContract.EventsEntry.COLUMN_DESCRIPTION));
                eventValues.put(MessageContract.EventsEntry.COLUMN_IS_VALID, event.getInt(MessageContract.EventsEntry.COLUMN_IS_VALID));
                eventValues.put(MessageContract.EventsEntry.COLUMN_PRIORITY, event.getInt(MessageContract.EventsEntry.COLUMN_PRIORITY));
                eventValues.put(MessageContract.EventsEntry.COLUMN_TIMESTAMP, event.getString(MessageContract.EventsEntry.COLUMN_TIMESTAMP));

                if (context.getContentResolver().insert(MessageContract.EventsEntry.CONTENT_URI, eventValues) != null)
                {
                    File dir = Utility.choosePreferredDir(context);
                    assert dir != null;
                    if (!dir.exists())
                        dir.mkdirs();

                    String eventsDir = dir.getAbsolutePath();
                    String fileName = "e"+event.getInt("id")+".jpg";

                    File eventFile = new File(eventsDir, fileName);
                    if (!eventFile.exists())
                    {
                        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        Uri uri = Uri.parse("http://www.felixunlimited.com/events/"+fileName);
                        DownloadManager.Request req=new DownloadManager.Request(uri);

                        req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                                | DownloadManager.Request.NETWORK_MOBILE)
                                .setAllowedOverRoaming(false)
                                .setVisibleInDownloadsUi(false)
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                                .setTitle("WordApp Syncing")
                                .setDescription("This will just take a while")
//                                .setDestinationUri(Uri.fromFile(Utility.chooseFile(context, fileName)));
                                .setDestinationInExternalFilesDir(context, null, fileName);
// Enqueue a new download and same the referenceId
                        long downloadReference = downloadManager.enqueue(req);
                    }
                    j++;
                }
            }

            notifyEvent();
            log(LOG_TAG, "Events Sync Complete. " + j +  " Inserted", Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, context);

        } catch (JSONException e) {
            log(LOG_TAG, e.getMessage(), Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'e', null, context);
            e.printStackTrace();
        }
    }

    private void getDeclarationsDataFromJson(String declarationsJsonStr)
            throws JSONException
    {
        final String RESPONSE = "response";

        try {
            JSONObject declarationsJson = new JSONObject(declarationsJsonStr);
            JSONArray declarationsArray = declarationsJson.getJSONArray(RESPONSE);

            // Insert the new message information into the database
            int j = 0;
            for(int i = 0; i < declarationsArray.length(); i++) {
                // These are the values that will be collected.
                // Get the JSON object representing the day
                JSONObject declaration = declarationsArray.getJSONObject(i);

                ContentValues declarationValues = new ContentValues();

                declarationValues.put(MessageContract.DeclarationsEntry._ID, declaration.getInt("id"));
                declarationValues.put(MessageContract.DeclarationsEntry.COLUMN_DATE, declaration.getString(MessageContract.DeclarationsEntry.COLUMN_DATE));
                declarationValues.put(MessageContract.DeclarationsEntry.COLUMN_TITLE, declaration.getString(MessageContract.DeclarationsEntry.COLUMN_TITLE));
                declarationValues.put(MessageContract.DeclarationsEntry.COLUMN_CATEGORY, declaration.getString(MessageContract.DeclarationsEntry.COLUMN_CATEGORY));
                declarationValues.put(MessageContract.DeclarationsEntry.COLUMN_PREACHER_KEY, declaration.getInt(MessageContract.DeclarationsEntry.COLUMN_PREACHER_KEY));
                declarationValues.put(MessageContract.DeclarationsEntry.COLUMN_TIMESTAMP, declaration.getString(MessageContract.DeclarationsEntry.COLUMN_TIMESTAMP));

                if (context.getContentResolver().insert(MessageContract.DeclarationsEntry.CONTENT_URI, declarationValues) != null)
                {
                    File dir = Utility.choosePreferredDir(context);
                    assert dir != null;
                    if (!dir.exists())
                        dir.mkdirs();

                    String declarationsDir = dir.getAbsolutePath();

                    String fileNameT = "d"+declaration.getInt("id")+".txt";
                    File declarationFileT = new File(declarationsDir, fileNameT);
                    if (!declarationFileT.exists())
                    {
                        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        Uri uri = Uri.parse("http://www.felixunlimited.com/declarations/"+fileNameT);
                        DownloadManager.Request req=new DownloadManager.Request(uri);

                        req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                                | DownloadManager.Request.NETWORK_MOBILE)
                                .setAllowedOverRoaming(false)
                                .setVisibleInDownloadsUi(false)
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                                .setTitle("WordApp Syncing")
                                .setDescription("This will just take a while")
 //                               .setDestinationUri(Uri.fromFile(Utility.chooseFile(context, fileNameT)));
                                .setDestinationInExternalFilesDir(context, null, fileNameT);
                        //Enqueue a new download and same the referenceId
                        long downloadReference = downloadManager.enqueue(req);
                    }

                    String fileNameA = "d"+declaration.getInt("id")+".mp3";
                    File declarationFileA = new File(declarationsDir, fileNameA);
                    if (!declarationFileA.exists())
                    {
                        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        Uri uri = Uri.parse("http://www.felixunlimited.com/declarations/"+fileNameA);
                        DownloadManager.Request req=new DownloadManager.Request(uri);

                        req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                                | DownloadManager.Request.NETWORK_MOBILE)
                                .setAllowedOverRoaming(false)
                                .setVisibleInDownloadsUi(false)
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                                .setTitle("WordApp Syncing")
                                .setDescription("This will just take a while")
//                                .setDestinationUri(Uri.fromFile(Utility.chooseFile(context, fileNameT)));
                                .setDestinationInExternalFilesDir(context, null, fileNameA);
                        //Enqueue a new download and same the referenceId
                        long downloadReference = downloadManager.enqueue(req);
                    }
                    j++;
                }
            }
            notifyDecl();

            log(LOG_TAG, "declarations Sync Complete. " + j +  " Inserted", Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, context);

        } catch (JSONException e) {
            log(LOG_TAG, e.getMessage(), Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'e', null, context);
            e.printStackTrace();
        }
    }

    private void clearLog() throws JSONException {
        Uri logUri = MessageContract.LogEntry.CONTENT_URI;

        // we'll query our contentProvider, as always
        Cursor cursor = context.getContentResolver().query(logUri, LOG_PROJECTION, null, null, null);

        if (cursor.moveToFirst()) {
            JSONArray jsonArray = new JSONArray();
            do {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id",cursor.getInt(LOG_ID));
                jsonObject.put("user_id",cursor.getString(LOG_USER_ID));
                jsonObject.put("event",cursor.getString(LOG_EVENT));
                jsonObject.put("timestamp",cursor.getString(LOG_TIMESTAMP));

                jsonArray.put(cursor.getPosition(), jsonObject);
            }
            while (cursor.moveToNext());

            JSONParser jsonParser = new JSONParser();
            String string = jsonArray.toString();
            JSONObject jsonObject;
            String response = "no";
            jsonObject = jsonParser.makeHttpRequest(WEBSERVICE_URL,"POST",jsonArray.toString(),"log");
            if (jsonObject != null)
                response = jsonObject.getString("response");
            else
                onSyncCanceled();

            if (response.equals("ok"))
            {
                context.getContentResolver().delete(logUri, null, null);
            }
        }
    }

    private void notifyEvent() {
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if (displayNotifications) {

            String lastEventNotificationKey = context.getString(R.string.pref_last_event_notification);
            long lastEventSync = prefs.getLong(lastEventNotificationKey, 0);

            if (System.currentTimeMillis() - lastEventSync >= DAY_IN_MILLIS) {
                // Last sync was more than 1 day ago, let's send a notification with the message.

                Uri eventsUri = MessageContract.EventsEntry.CONTENT_URI;

                // we'll query our contentProvider, as always
                Cursor eventsCursor = context.getContentResolver().query(eventsUri, NOTIFY_EVENTS_PROJECTION, null, null, null);

                if (eventsCursor != null) {
                    if (eventsCursor.getCount() <= 0) {
                        eventsCursor.close();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong(lastEventNotificationKey, 0);
                        editor.apply();
                        return;
                    }

                    eventsCursor.moveToFirst();
                    int iconId = R.drawable.p0;
                    Resources resources = context.getResources();
                    Bitmap largeIcon = BitmapFactory.decodeResource(resources,
                            R.drawable.p0);
                    String title = "Upcoming Events";

                    // Define the text of the message.
                    String contentText;
                    if (eventsCursor.getCount() == 1)
                        contentText = "There is " + eventsCursor.getCount() + " upcoming event";
                    else
                        contentText = "There are " + eventsCursor.getCount() + " upcoming events";

                    // NotificationCompatBuilder is a very convenient way to build backward-compatible
                    // notifications.  Just throw in some data.
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setColor(resources.getColor(R.color.wordapp_light_orange))
                                    .setSmallIcon(iconId)
                                    .setSound(Uri.parse("android.resource://com.felixunlimited.word.app/" + R.raw.e))
                                    .setLargeIcon(largeIcon)
                                    .setContentTitle(title)
                                    .setContentText(contentText);

                     /* Add Big View Specific Configuration */
                    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

                    // Sets a title for the Inbox style big view
                    inboxStyle.setBigContentTitle("COZA Events");
                    while (!eventsCursor.isAfterLast()) {
                        // Moves events into the big view
                        inboxStyle.addLine(eventsCursor.getString(EVENT_TITLE)+" - "+eventsCursor.getString(EVENT_DATE));
                        eventsCursor.moveToNext();
                    }

                    mBuilder.setStyle(inboxStyle);

                    // Make something interesting happen when the user clicks on the notification.
                    // In this case, opening the app is sufficient.
                    Intent resultIntent = new Intent(context, EventsActivity.class);

                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
                    mNotificationManager.notify(EVENTS_NOTIFICATION_ID, mBuilder.build());

                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastEventNotificationKey, System.currentTimeMillis());
                    editor.apply();
                    eventsCursor.close();
                }
            }
        }
    }

    private void notifyDecl() {
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if (displayNotifications) {

            String lastDeclNotificationKey = context.getString(R.string.pref_last_decl_notification);
            long lastDeclSync = prefs.getLong(lastDeclNotificationKey, 0);

            if (System.currentTimeMillis() - lastDeclSync >= (1000*60*60*(prefs.getInt(context.getString(R.string.pref_decl_interval_key), 3)))) {
                // Last sync was more than 1 day ago, let's send a notification with the message.

                Uri declarationsUri = MessageContract.DeclarationsEntry.CONTENT_URI;
                Cursor declarationsCursor = context.getContentResolver().query(declarationsUri, NOTIFY_DECLARATIONS_PROJECTION, null, null, null);

                if (declarationsCursor != null) {
                    declarationsCursor.moveToFirst();
                    if (declarationsCursor.getCount() <= 0) {
                        declarationsCursor.close();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong(lastDeclNotificationKey, 0);
                        editor.apply();
                        return;
                    }

                    int iconId = R.drawable.p0;
                    Resources resources = context.getResources();
                    Bitmap largeIcon = BitmapFactory.decodeResource(resources,
                            R.drawable.p0);
                    String title = "iDeclare";

                    // Define the text of the message.
                    String contentText;
                    if (declarationsCursor.getCount() == 1)
                        contentText = "There is "+declarationsCursor.getCount()+" declaration";
                    else
                        contentText = "There are "+declarationsCursor.getCount()+" declarations";

                    String path = Utility.choosePreferredDir(context).getAbsolutePath()+"d"+declarationsCursor.getInt(DECLARATIONS_ID)+".mo3";
                    Uri.Builder uriBuilder = new Uri.Builder();
                    Uri uri = uriBuilder.path(path).build();
                    // NotificationCompatBuilder is a very convenient way to build backward-compatible
                    // notifications.  Just throw in some data.
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setColor(resources.getColor(R.color.wordapp_light_orange))
                                    .setSmallIcon(iconId)
                                    .setLargeIcon(largeIcon)
                                    .setContentTitle(title)
                                    .setSound(uri)
                                    .setSound(Uri.parse("android.resource://com.felixunlimited.word.app/" + R.raw.e))
                                    .setContentText(contentText);

                     /* Add Big View Specific Configuration */
                    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

                    // Sets a title for the Inbox style big view
                    inboxStyle.setBigContentTitle("iDeclare");
                    while(!declarationsCursor.isAfterLast()) {

                        // Moves declarations into the big view
                        inboxStyle.addLine(declarationsCursor.getString(DECLARATIONS_TITLE));
                        declarationsCursor.moveToNext();
                    }

                    mBuilder.setStyle(inboxStyle);

                    // Make something interesting happen when the user clicks on the notification.
                    // In this case, opening the app is sufficient.
                    Intent resultIntent = new Intent(context, DeclarationsActivity.class);

                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
                    mNotificationManager.notify(DECL_NOTIFICATION_ID, mBuilder.build());

                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastDeclNotificationKey, System.currentTimeMillis());
                    editor.apply();
                    declarationsCursor.close();
                }
            }
        }
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        WordAppSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}