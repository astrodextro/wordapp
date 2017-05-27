package com.felixunlimited.word.app.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

import com.felixunlimited.word.app.Utility;

/**
 * Created by Helen on 28/03/2016.
 */
public class MessageContract {

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.felixunlimited.word.app";
    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_MESSAGES = "messages";
    public static final String PATH_MESSAGES_SEARCH = "search_result";
    public static final String PATH_PREACHERS = "preachers";
    public static final String PATH_LOG = "log";
    public static final String PATH_USER = "user";
    public static final String PATH_EVENTS = "events";
    public static final String PATH_DECLARATIONS = "declarations";
    public static final String PATH_USER_MESSAGES = Utility.getUniquePsuedoID();

    // To make it easy to query for the exact date, we normalize all dates that go into
    // the database to the start of the the Julian day at UTC.
    public static long normalizeDate(long startDate) {
        // normalize the start date to the beginning of the (UTC) day
        Time time = new Time();
        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }

    public static final class MessageEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MESSAGES).build();
        // Possible paths (appended to base content URI for possible URI's)
        // For instance, content://com.felixunlimited.word.app/message/ is a valid path for
        // looking at message data. content://com.felixunlimited.word.app/givemeroot/ will fail,
        // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
        // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MESSAGES;
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MESSAGES;

        public static final String TABLE_NAME = "messages";

        //        public static final String COLUMN_MESSAGE_ID = "message_id";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_PREACHER = "preacher_name";
        public static final String COLUMN_PREACHER_KEY = "preacher_key";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_OVERVIEW = "overview";
        public static final String COLUMN_DOWNLOADS = "downloads";
        public static final String COLUMN_PURCHASES = "purchases";
        public static final String COLUMN_STREAMS = "streams";
        public static final String COLUMN_RATING = "rating";
        public static final String COLUMN_DOWNLOADED = "downloaded";
        public static final String COLUMN_PURCHASED = "purchased";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_PAID = "paid";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_UPDATED = "updated";

        public static Uri buildMessageUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildMessageUriWithDate(String date) {
            return CONTENT_URI.buildUpon()
                    .appendPath(date).build();
        }

        public static String getDateFromUri(Uri uri) {
            return (uri.getPathSegments().get(1));
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (null != dateString && dateString.length() > 0)
                return Long.parseLong(dateString);
            else
                return 0;
        }
    }
    public static final class MessageSearchEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MESSAGES_SEARCH).build();
        // Possible paths (appended to base content URI for possible URI's)
        // For instance, content://com.felixunlimited.word.app/message/ is a valid path for
        // looking at message data. content://com.felixunlimited.word.app/givemeroot/ will fail,
        // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
        // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MESSAGES_SEARCH;
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MESSAGES_SEARCH;

        public static final String TABLE_NAME = "search_result";

        //        public static final String COLUMN_MESSAGE_ID = "message_id";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_PREACHER = "preacher_name";
        public static final String COLUMN_PREACHER_KEY = "preacher_key";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_OVERVIEW = "overview";
        public static final String COLUMN_DOWNLOADS = "downloads";
        public static final String COLUMN_PURCHASES = "purchases";
        public static final String COLUMN_STREAMS = "streams";
        public static final String COLUMN_RATING = "rating";
        public static final String COLUMN_DOWNLOADED = "downloaded";
        public static final String COLUMN_PURCHASED = "purchased";
        public static final String COLUMN_PRICE = "price";

        public static Uri buildMessageUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildMessageUriWithDate(String date) {
            return CONTENT_URI.buildUpon()
                    .appendPath(date).build();
        }

        public static String getDateFromUri(Uri uri) {
            return (uri.getPathSegments().get(1));
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (null != dateString && dateString.length() > 0)
                return Long.parseLong(dateString);
            else
                return 0;
        }
    }

    public static final class UserEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_USER).build();
        // Possible paths (appended to base content URI for possible URI's)
        // For instance, content://com.felixunlimited.word.app/message/ is a valid path for
        // looking at message data. content://com.felixunlimited.word.app/givemeroot/ will fail,
        // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
        // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_USER;
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_USER;

        public static final String TABLE_NAME = "user";

        //        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_USER_EMAIL = "email";
        public static final String COLUMN_DOWNLOADS = "downloads";
        public static final String COLUMN_PURCHASES = "purchases";
        public static final String COLUMN_STREAMS = "streams";
        public static final String COLUMN_RATING = "rating";
        public static final String COLUMN_DECLARATIONS_SUBSCRIPTION = "declarations";
        public static final String COLUMN_PRAYERS_SUBSCRIPTION = "prayers";

        public static Uri buildUserUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class UserMessagesEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_USER_MESSAGES).build();
        // Possible paths (appended to base content URI for possible URI's)
        // For instance, content://com.felixunlimited.word.app/message/ is a valid path for
        // looking at message data. content://com.felixunlimited.word.app/givemeroot/ will fail,
        // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
        // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_USER_MESSAGES;
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_USER_MESSAGES;

        public static final String TABLE_NAME = PATH_USER_MESSAGES;

        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_PAID = "paid";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_PREACHER = "preacher_name";
        public static final String COLUMN_PREACHER_KEY = "preacher_key";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_OVERVIEW = "overview";
        public static final String COLUMN_DOWNLOADS = "downloads";
        public static final String COLUMN_PURCHASES = "purchases";
        public static final String COLUMN_STREAMS = "streams";
        public static final String COLUMN_RATING = "rating";
        public static final String COLUMN_DOWNLOADED = "downloaded";
        public static final String COLUMN_PURCHASED = "purchased";
        public static final String COLUMN_TIMESTAMP = "timestamp";

        public static Uri buildUserMessagesUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class PreachersEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PREACHERS).build();
        // Possible paths (appended to base content URI for possible URI's)
        // For instance, content://com.felixunlimited.word.app/message/ is a valid path for
        // looking at message data. content://com.felixunlimited.word.app/givemeroot/ will fail,
        // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
        // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PREACHERS;
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PREACHERS;

        public static final String TABLE_NAME = "preachers";

//        public static final String COLUMN_PREACHER_ID = "preacher_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_MINISTRY = "ministry";
        public static final String COLUMN_BIRTHDAY = "birthday";
        public static final String COLUMN_NO_OF_MESSAGES = "messages";
        public static final String COLUMN_STREAMS = "streams";
        public static final String COLUMN_PURCHASES = "purchases";
        public static final String COLUMN_DOWNLOADS = "downloads";
        public static final String COLUMN_INTERNAL_EXTERNAL = "int_ext";
//        public static final String COLUMN_STREAMS = "downloaded";

        public static Uri buildPreacherUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class LogEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOG).build();
        // Possible paths (appended to base content URI for possible URI's)
        // For instance, content://com.felixunlimited.word.app/message/ is a valid path for
        // looking at message data. content://com.felixunlimited.word.app/givemeroot/ will fail,
        // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
        // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOG;
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOG;

        public static final String TABLE_NAME = "log";

        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_EVENT = "event";

        public static Uri buildLogUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class EventsEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_EVENTS).build();
        // Possible paths (appended to base content URI for possible URI's)
        // For instance, content://com.felixunlimited.word.app/message/ is a valid path for
        // looking at message data. content://com.felixunlimited.word.app/givemeroot/ will fail,
        // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
        // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_EVENTS;
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_EVENTS;

        public static final String TABLE_NAME = "events";

        //        public static final String COLUMN_MESSAGE_ID = "message_id";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_VENUE = "venue";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_IS_VALID = "is_valid";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_PRIORITY = "priority";
//        public static final String COLUMN_IMAGE_FILE = "image_file";

        public static Uri buildEventsUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildEventsUriWithDate(String date) {
            return CONTENT_URI.buildUpon()
                    .appendPath(date).build();
        }

        public static String getDateFromUri(Uri uri) {
            return (uri.getPathSegments().get(1));
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (null != dateString && dateString.length() > 0)
                return Long.parseLong(dateString);
            else
                return 0;
        }
    }

    public static final class DeclarationsEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_DECLARATIONS).build();
        // Possible paths (appended to base content URI for possible URI's)
        // For instance, content://com.felixunlimited.word.app/message/ is a valid path for
        // looking at message data. content://com.felixunlimited.word.app/givemeroot/ will fail,
        // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
        // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_DECLARATIONS;
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_DECLARATIONS;

        public static final String TABLE_NAME = "declarations";

        //        public static final String COLUMN_MESSAGE_ID = "message_id";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_PREACHER_KEY = "preacher_key";
        public static final String COLUMN_CATEGORY = "category";
//        public static final String COLUMN_TEXT_FILE = "text_file";
//        public static final String COLUMN_AUDIO_FILE = "audio_file";
        public static final String COLUMN_DOWNLOADED = "downloaded";
        public static final String COLUMN_TIMESTAMP = "timestamp";

        public static Uri buildDeclarationsUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildDeclarationsUriWithDate(String date) {
            return CONTENT_URI.buildUpon()
                    .appendPath(date).build();
        }

        public static String getDateFromUri(Uri uri) {
            return (uri.getPathSegments().get(1));
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (null != dateString && dateString.length() > 0)
                return Long.parseLong(dateString);
            else
                return 0;
        }
    }

}