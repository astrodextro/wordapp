package com.felixunlimited.word.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.felixunlimited.word.app.data.MessageContract;
import com.felixunlimited.word.app.sync.JSONParser;
import com.felixunlimited.word.app.sync.WordAppSyncAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Helen on 21/03/2016.
 */
public class Utility {

    public static int THEME_BLUE = 1;
    public static int THEME_ORANGE = 2;
    public static int THEME_DARK = 3;

    /**
     * Simple String transformation by XOR-ing all characters by value.
     */
    static String stringTransform(String s, int i) {
        char[] chars = s.toCharArray();
        for(int j = 0; j<chars.length; j++)
            chars[j] = (char)(chars[j] ^ i);
        return String.valueOf(chars);
    }

    public static String appKey = stringTransform("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnjkk450JrjSPvDeoi3gtHGTIdQqfpx2bhiQsoz0gyYpFbohHyB/hSiShn6zG/Ad/hzsSft6+WaSu8b3D9pdjVw2aHML908RP8uSz1UpHa2OXER4tYMDot+kc2Xg9CqUmSVi65sEKryW0BxIE+/doGN4RYeNYNL4G08yIEUgU/pUec7VwXcNLVumvJK6LRS55iwZDjJOaYT8dL3H9G64ZLz0oLhij+YJTcrpzuFx2I3KR0GaaOJZ5qjWedXlqI2nleJ72r+XfXkaYsGm2ec9jbKhNLOdHlCy+osl7uIPMsW3Gy5fs1aIxAXOk7p398CfMURjWjdSjlSCzAMN7gS8oUwIDAQAB",0xCD);

    public static void log(String tag,  String event, String userID, long timeStamp, char mode, Throwable exception, Context context)
    {
        switch (mode)
        {
            case 'd':
            {
                Log.d(tag, event, exception);
                break;
            }
            case 'v':
            {
                Log.v(tag, event, exception);
                break;
            }
            case 'e':
            {
                Log.e(tag, event, exception);
                break;
            }
            case 'w':
            {
                Log.w(tag, event, exception);
                break;
            }
            default:
            {
                Log.i(tag, event, exception);
                break;
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:SS");
        Date resultdate = new Date(timeStamp);
        String datetime = sdf.format(resultdate);

        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageContract.LogEntry.COLUMN_USER_ID, Utility.getUserID(context));
        contentValues.put(MessageContract.LogEntry.COLUMN_TIMESTAMP, datetime);
        contentValues.put(MessageContract.LogEntry.COLUMN_EVENT, event);

        context.getContentResolver().insert(MessageContract.LogEntry.CONTENT_URI, contentValues);
    }

    public static void chooseTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//        int theme = prefs.getInt(context.getString(R.string.pref_themes_label), 1));
        String theme = prefs.getString(context.getString(R.string.pref_themes_key), context.getString(R.string.pref_themes_blue));

        if (theme.equals(context.getString(R.string.pref_themes_blue)))
        {
            context.setTheme(R.style.pref_themes_blue);
        }
        else if (theme.equals(context.getString(R.string.pref_themes_orange)))
        {
            context.setTheme(R.style.pref_themes_orange);
        }
        else if (theme.equals(context.getString(R.string.pref_themes_dark)))
        {
            context.setTheme(R.style.pref_themes_dark);
        }
        else {
            context.setTheme(R.style.AppTheme);
        }
//        switch (theme)
//        {
//            case context.getString(R.string.pref_themes_blue):
//                context.setTheme(R.style.pref_themes_blue);
//                break;
//            case THEME_ORANGE:
//                context.setTheme(R.style.pref_themes_orange);
//                break;
//            case THEME_DARK:
//                context.setTheme(R.style.pref_themes_dark);
//                break;
//            default:
//                context.setTheme(R.style.AppTheme);
//                break;
//        }
    }

    static String formatDate(long dateInMilliseconds) {
        Date date = new Date(dateInMilliseconds);
        return DateFormat.getDateInstance().format(date);
    }

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return a user-friendly representation of the date.
     */
//    public static String getFriendlyDayString(Context context, long dateInMillis) {
//        // The day string for message uses the following logic:
//        // For today: "Today, June 8"
//        // For tomorrow:  "Tomorrow"
//        // For the next 5 days: "Wednesday" (just the day name)
//        // For all days after that: "Mon Jun 8"
//
//        Time time = new Time();
//        time.setToNow();
//        long currentTime = System.currentTimeMillis();
//        int julianDay = Time.getJulianDay(dateInMillis, time.gmtoff);
//        int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);
//
//        // If the date we're building the String for is today's date, the format
//        // is "Today, June 24"
//        if (julianDay == currentJulianDay) {
//            String today = context.getString(R.string.today);
//            int formatId = R.string.format_full_friendly_date;
//            return String.format(context.getString(
//                    formatId,
//                    today,
//                    getFormattedMonthDay(context, dateInMillis)));
//        } else if ( julianDay < currentJulianDay + 7 ) {
//            // If the input date is less than a week in the future, just return the day name.
//            return getDayName(context, dateInMillis);
//        } else {
//            // Otherwise, use the form "Mon Jun 3"
//            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
//            return shortenedDateFormat.format(dateInMillis);
//        }
//    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return
     */
    public static String getDayName(Context context, long dateInMillis) {
        // If the date is today, return the localized version of "Today" instead of the actual
        // day name.

        Time t = new Time();
        t.setToNow();
        int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        } else if ( julianDay == currentJulianDay +1 ) {
            return context.getString(R.string.tomorrow);
        } else {
            Time time = new Time();
            time.setToNow();
            // Otherwise, the format is just the day of the week (e.g "Wednesday".
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateInMillis The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, long dateInMillis ) {
        Time time = new Time();
        time.setToNow();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
        String monthDayString = monthDayFormat.format(dateInMillis);
        return monthDayString;
    }

    public static boolean isPaidFor() {
        return false;
    }

    public static boolean isDownloaded(String audio_path) {
        //if ()
        return false;
    }

    public static boolean confirm (Context context, String message) {
//        if (sharedPref.getBoolean("com.felixiosystems.wordapp.isChecked", true))
//            return true;

        final boolean[] confirm = {false};
        AlertDialog.Builder bld = new AlertDialog.Builder(context);
        bld.setMessage(message);
        bld.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                confirm[0] = true;
            }
        });
        bld.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        bld.create().show();

//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.putBoolean("com.felixiosystems.wordapp.isChecked", confirm[0]);
//        editor.apply();

        return confirm[0];
    }

    public static Bitmap preacherPictureBitmap(int preacher_id, Context context)
    {
        File f=new File(choosePreferredDir (context), preacher_id+".png");
        return BitmapFactory.decodeFile(f.getAbsolutePath());
    }

    public static Uri getDownloadedMessageUri(int message_id, Context context)
    {
        Uri.Builder uriBuilder = new Uri.Builder();
        return uriBuilder.path(choosePreferredDir (context).getAbsolutePath())
                .appendPath(message_id+".mp3")
                .build();
//        File preacher_pic_file = new File(Environment.getDataDirectory()+"/preachers/",""+preacher_id);
//        return uriBuilder.path(preacher_pic_file.toString()).build();
    }

    public static Uri getDownloadedDeclUri(int decl_id, Context context)
    {
        Uri.Builder uriBuilder = new Uri.Builder();
        return uriBuilder.path(choosePreferredDir (context).getAbsolutePath())
                .appendPath("d"+decl_id+".mp3")
                .build();
//        File preacher_pic_file = new File(Environment.getDataDirectory()+"/preachers/",""+preacher_id);
//        return uriBuilder.path(preacher_pic_file.toString()).build();
    }

    /**
     * Return pseudo unique ID
     * @return ID
     */
    public static String getUniquePsuedoID() {
        // If all else fails, if the user does have lower than API 9 (lower
        // than Gingerbread), has reset their device or 'Secure.ANDROID_ID'
        // returns 'null', then simply the ID returned will be solely based
        // off their Android device information. This is where the collisions
        // can happen.
        // Thanks http://www.pocketmagic.net/?p=1662!
        // Try not to use DISPLAY, HOST or ID - these items could change.
        // If there are collisions, there will be overlapping data
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

        // Thanks to @Roman SL!
        // http://stackoverflow.com/a/4789483/950427
        // Only devices with API >= 9 have android.os.Build.SERIAL
        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        // If a user upgrades software or roots their device, there will be a duplicate entry
        String serial = null;
        try {
            serial = android.os.Build.class.getField("SERIAL").get(null).toString();

            // Go ahead and return the serial for api => 9
            String code = new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
            code = code.replace('-','_');
            return code;
        } catch (Exception exception) {
            // String needs to be initialized
            serial = "serial"; // some value
        }
        String code = new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        code = code.replace('-','_');

        // Thanks @Joe!
        // http://stackoverflow.com/a/2853253/950427
        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return code;
    }

    public static String getUserID(Context context) {
        String uniqueID = null;
        final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    BackupAgent.PREFS, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = getUniquePsuedoID();
//                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();

                //backup the changes
                BackupManager mBackupManager = new BackupManager(context);
                mBackupManager.dataChanged();
            }
        }

        return uniqueID;
    }

    public static void setImage(String imgPath, String fileName, ImageView img)
    {
        File f=new File(imgPath, fileName);
        Uri.Builder uriBuilder = new Uri.Builder();
        Uri uri = uriBuilder.path(f.getPath()).build();
//            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
        Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
        if (isExternalStorageReadable())
            if (f.exists()) {
                img.setImageURI(uri);
            } else {
                img.setImageResource(R.drawable.p0);
            }
        else {
            img.setImageResource(R.drawable.p0);
        }

    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) &&
                !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static boolean isConnected(Context context) {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    /**
     * Gets the state of Airplane Mode.
     *
     * @param context
     * @return true if enabled.
     */
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    public static String getEmail(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = getAccount(accountManager);

        if (account == null) {
            return null;
        } else {
            return account.name;
        }
    }

    private static Account getAccount(AccountManager accountManager) {
        Account[] accounts = accountManager.getAccountsByType("com.google");
        Account account;
        if (accounts.length > 0) {
            account = accounts[0];
        } else {
            account = null;
        }
        return account;
    }

    private static boolean isPhone(Context context){
        TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if(manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE){
            return true;
        }
            return false;

    }

    public static boolean isSimSupport(Context context)
    {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);  //gets the current TelephonyManager
        return !(tm.getSimState() == TelephonyManager.SIM_STATE_ABSENT);
    }

    public static class UseNetwork extends AsyncTask<String, Void, String> {

        Context sContext;
        String messageID;

        public UseNetwork(Context context) {
            super();
            sContext = context;
        }

        @Override
        protected String doInBackground(String... params) {
            JSONParser jsonParser = new JSONParser();
            messageID = params[2];
            String response = "no";
            JSONObject jsonObject;
            try {
                jsonObject = jsonParser.makeHttpRequest(WordAppSyncAdapter.WEBSERVICE_URL,"POST",params[0],params[1]);
                if (jsonObject != null)
                    response = jsonObject.getString("response");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            if (response.equals("ok"))
            {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MessageContract.MessageEntry.COLUMN_UPDATED, 1);
                sContext.getContentResolver().update(MessageContract.MessageEntry.CONTENT_URI, contentValues,
                        MessageContract.UserMessagesEntry._ID+" = ?", new String[]{messageID});
            }
        }
    }

    public static void updateDB(Context context, String table, int amtPaid, int msgID, int rating, long timeStamp) throws JSONException {
        Uri messageUri = MessageContract.MessageEntry.CONTENT_URI;
        // we'll query our contentProvider, as always
        int streams = 1;
        Cursor cursor;

        if (!table.equals("sync")) {
            cursor = context.getContentResolver().query(messageUri, UserMessagesListFragment.MESSAGES_COLUMNS, "paid > ?", new String[]{"0"}, "timestamp DESC LIMIT 1");
            if (cursor.moveToFirst()) {
                streams += cursor.getInt(UserMessagesListFragment.COL_MESSAGE_STREAMS);
            }

            ContentValues contentValues = new ContentValues();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            final SharedPreferences.Editor editor = sharedPreferences.edit();

            contentValues.put(MessageContract.MessageEntry.COLUMN_PAID, amtPaid);
            contentValues.put(MessageContract.MessageEntry.COLUMN_TIMESTAMP, timeStamp);
            contentValues.put(MessageContract.MessageEntry.COLUMN_UPDATED, 0);

            switch (table) {
                case "complete":
                    contentValues.put(MessageContract.MessageEntry.COLUMN_PURCHASED, 1);
                    editor.putInt(String.valueOf(msgID), amtPaid);
                    editor.apply();
                    break;
                case "streams":
                    contentValues.clear();
                    contentValues.put(MessageContract.MessageEntry.COLUMN_STREAMS, streams);
                    editor.putInt(String.valueOf(msgID), amtPaid);
                    editor.apply();
                    break;
                case "downloaded":
                    contentValues.clear();
                    contentValues.put(MessageContract.MessageEntry.COLUMN_DOWNLOADED, 1);
                    break;
                case "rating":
                    contentValues.clear();
                    contentValues.put(MessageContract.MessageEntry.COLUMN_RATING, rating);
                    break;
            }

            context.getContentResolver().update(MessageContract.MessageEntry.CONTENT_URI, contentValues,
                    MessageContract.UserMessagesEntry._ID+" = ?", new String[]{String.valueOf(msgID)});
        }
        else
            cursor = context.getContentResolver().query(messageUri, UserMessagesListFragment.MESSAGES_COLUMNS, "paid > ?", new String[]{"0"}, null);

        if (cursor.moveToFirst()) {

            JSONArray jsonArray = new JSONArray();
            do {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:SS");
                Date resultDate = new Date(Long.parseLong(cursor.getString(UserMessagesListFragment.COL_TIMESTAMP)));
                String datetime = sdf.format(resultDate);
                JSONObject jsonObject = new JSONObject();

                jsonObject.put("msg_id", cursor.getInt(UserMessagesListFragment.COL_MESSAGE_ID));
                jsonObject.put("preacher_id", cursor.getInt(UserMessagesListFragment.COL_MESSAGE_PREACHER_KEY));
                jsonObject.put("user_id", Utility.getUniquePsuedoID());
                jsonObject.put("paid", cursor.getString(UserMessagesListFragment.COL_PAID));
                jsonObject.put("price", cursor.getString(UserMessagesListFragment.COL_PRICE));
                jsonObject.put("downloaded", cursor.getString(UserMessagesListFragment.COL_MESSAGE_DOWNLOADED));
                jsonObject.put("streams", cursor.getString(UserMessagesListFragment.COL_MESSAGE_STREAMS));
                jsonObject.put("rating", cursor.getString(UserMessagesListFragment.COL_MESSAGE_RATING));
                jsonObject.put("timestamp", datetime);

                jsonArray.put(cursor.getPosition(), jsonObject);
            }
            while (cursor.moveToNext());

            new Utility.UseNetwork(context).execute(jsonArray.toString(), table, String.valueOf(msgID));
        }
    }

    /**
     * Returns all available external SD-Card roots in the system.
     *
     * @return paths to all available external SD-Card roots in the system.
     */
    public static String[] getStorageDirectories(Context applicationContext) {
        String [] storageDirectories;
        String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            List<String> results = new ArrayList<String>();
            File[] externalDirs = applicationContext.getExternalFilesDirs(null);
            for (File file : externalDirs) {
                String path = file.getPath().split("/Android")[0];
                if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Environment.isExternalStorageRemovable(file))
                        || rawSecondaryStoragesStr != null && rawSecondaryStoragesStr.contains(path)){
                    results.add(path);
                }
            }
            storageDirectories = results.toArray(new String[0]);
        } else {
            final Set<String> rv = new HashSet<String>();

            if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
                final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
                Collections.addAll(rv, rawSecondaryStorages);
            }
            storageDirectories = rv.toArray(new String[rv.size()]);
        }
        return storageDirectories;
    }

    public static File choosePreferredDir (Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String internal = context.getString(R.string.pref_dir_chooser_internal);
        String external = context.getString(R.string.pref_dir_chooser_external);

        if (internal.equals(sharedPreferences.getString(context.getString(R.string.pref_dir_chooser_key), null))) {
            if (hasSpace(context.getFilesDir()))
                return context.getFilesDir();
            else
                Toast.makeText(context, "Insufficient memory in internal storage", Toast.LENGTH_LONG).show();
        }
        else if (external.equals(sharedPreferences.getString(context.getString(R.string.pref_dir_chooser_key), null))) {
            if (isExternalStorageWritable()) {
                if (!hasSpace(context.getExternalFilesDir(null)))
                    Toast.makeText(context, "Insufficient memory in external memory", Toast.LENGTH_LONG).show();
                return context.getExternalFilesDir(null);
//                if (hasSpace(context.getExternalFilesDir(null)))
//                    return context.getExternalFilesDir(null);
//                else
//                    Toast.makeText(context, "Insufficient memory in external memory", Toast.LENGTH_LONG).show();
            }
            else
                Toast.makeText(context, "External memory not readable", Toast.LENGTH_LONG).show();
        }

        return null;
    }

    public static File chooseFile (Context context, String fileName) {
        return new File(choosePreferredDir(context).getAbsolutePath()+"/"+fileName);
    }

    public static long getFolderSize(File f) {
        long size = 0;
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                size += getFolderSize(file);
            }
        } else {
            size=f.length();
        }
        return size;
    }

    public static boolean hasSpace (File dir) {
        return dir.getFreeSpace() > 10000000;
    }

    // Reference: http://stackoverflow.com/a/5599842

    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static boolean moveFiles(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < sourceLocation.listFiles().length; i++) {

                moveFiles(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);

            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            in.close();
            out.close();

            // delete the original file
            sourceLocation.delete();

            if (sourceLocation.getFreeSpace() == 0)
                return true;
        }
        return false;
    }

    public static class MoveFilesTask extends AsyncTask<File, Void, Boolean> {

        Context sContext;

        public MoveFilesTask(Context context) {
            super();
            sContext = context;
        }

        @Override
        protected Boolean doInBackground(File... params) {
            try {
                if (moveFiles(params[0], params[1]))
                    return true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean response) {
            super.onPostExecute(response);
            if (response)
                Toast.makeText(sContext, "Files successfully moved", Toast.LENGTH_SHORT).show();
        }
    }
}