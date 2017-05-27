/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.felixunlimited.word.app.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

public class MessageProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private MessageDbHelper mOpenHelper;

    static final int MESSAGE = 100;
    static final int MESSAGE_WITH_DATE = 101;
    static final int MESSAGE_WITH_ID = 102;
    static final int MESSAGE_SEARCH = 150;
    static final int USER = 200;
    static final int LOG = 300;
    static final int PREACHER = 400;
    static final int USER_MESSAGES = 500;
    static final int EVENTS = 600;
    static final int DECLARATIONS = 700;

    private static final SQLiteQueryBuilder sMessageQueryBuilder;

    static{
        sMessageQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //weather INNER JOIN message ON weather.message_id = message._id
        sMessageQueryBuilder.setTables(
                MessageContract.MessageEntry.TABLE_NAME);
//                MessageContract.MessageEntry.TABLE_NAME + " INNER JOIN " +
//                        MessageContract.PreachersEntry.TABLE_NAME +
//                        " ON " + MessageContract.MessageEntry.TABLE_NAME +
//                        "." + MessageContract.MessageEntry.COLUMN_PREACHER_KEY +
//                        " = " + MessageContract.PreachersEntry.TABLE_NAME +
//                        "." + MessageContract.PreachersEntry.COLUMN_PREACHER_ID
//        );
    }

    //message.date = ?
    private static final String sDateSelection =
            MessageContract.MessageEntry.TABLE_NAME+
                    "." + MessageContract.MessageEntry.COLUMN_DATE + " = ? ";

//    //message.message_setting = ? AND date >= ?
//    private static final String sMessageSettingWithStartDateSelection =
//            MessageContract.MessageEntry.TABLE_NAME+
//                    "." + MessageContract.MessageEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
//                    MessageContract.MessageEntry.COLUMN_DATE + " >= ? ";

//    //message.message_setting = ? AND date = ?
//    private static final String sMessageSettingAndDaySelection =
//            MessageContract.MessageEntry.TABLE_NAME +
//                    "." + MessageContract.MessageEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
//                    MessageContract.MessageEntry.COLUMN_DATE + " = ? ";

    private Cursor getMessageByDate(Uri uri, String[] projection, String sortOrder) {
        //long startDate = MessageContract.MessageEntry.getStartDateFromUri(uri);
        String date = MessageContract.MessageEntry.getDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        selection = sDateSelection;
        selectionArgs = new String[]{date};

        return sMessageQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /*
        Students: Here is where you need to create the UriMatcher. This UriMatcher will
        match each URI to the MESSAGE, MESSAGE_WITH_LOCATION, MESSAGE_WITH_LOCATION_AND_DATE,
        and LOCATION integer constants defined above.  You can test this by uncommenting the
        testUriMatcher test within TestUriMatcher.
     */
    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MessageContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, MessageContract.PATH_MESSAGES, MESSAGE);
        matcher.addURI(authority, MessageContract.PATH_MESSAGES_SEARCH, MESSAGE_SEARCH);
        matcher.addURI(authority, MessageContract.PATH_PREACHERS, PREACHER);
        matcher.addURI(authority, MessageContract.PATH_LOG, LOG);
        matcher.addURI(authority, MessageContract.PATH_USER, USER);
        matcher.addURI(authority, MessageContract.PATH_USER_MESSAGES, USER_MESSAGES);
        matcher.addURI(authority, MessageContract.PATH_MESSAGES + "/*", MESSAGE_WITH_DATE);
        matcher.addURI(authority, MessageContract.PATH_MESSAGES + "/#", MESSAGE_WITH_ID);
        matcher.addURI(authority, MessageContract.PATH_EVENTS, EVENTS);
        matcher.addURI(authority, MessageContract.PATH_DECLARATIONS, DECLARATIONS);
        return matcher;
    }

    /*
        Students: We've coded this for you.  We just create a new MessageDbHelper for later use
        here.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new MessageDbHelper(getContext());
        return true;
    }

    /*
        Students: Here's where you'll code the getType function that uses the UriMatcher.  You can
        test this by uncommenting testGetType in TestProvider.

     */
    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case MESSAGE:
                return MessageContract.MessageEntry.CONTENT_TYPE;
            case MESSAGE_SEARCH:
                return MessageContract.MessageSearchEntry.CONTENT_TYPE;
            case PREACHER:
                return MessageContract.PreachersEntry.CONTENT_TYPE;
            case LOG:
                return MessageContract.LogEntry.CONTENT_TYPE;
            case USER:
                return MessageContract.UserEntry.CONTENT_TYPE;
            case USER_MESSAGES:
                return MessageContract.UserMessagesEntry.CONTENT_TYPE;
            case MESSAGE_WITH_DATE:
                return MessageContract.MessageEntry.CONTENT_ITEM_TYPE;
            case MESSAGE_WITH_ID:
                return MessageContract.MessageEntry.CONTENT_ITEM_TYPE;
            case EVENTS:
                return MessageContract.EventsEntry.CONTENT_TYPE;
            case DECLARATIONS:
                return MessageContract.DeclarationsEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder)        {

        String innerJoinStr = MessageContract.MessageEntry.TABLE_NAME + " INNER JOIN " +
                MessageContract.PreachersEntry.TABLE_NAME +
                " ON " + MessageContract.MessageEntry.TABLE_NAME +
                "." + MessageContract.MessageEntry.COLUMN_PREACHER_KEY +
                " = " + MessageContract.PreachersEntry.TABLE_NAME +
                "." + MessageContract.PreachersEntry._ID;

        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        int i = sUriMatcher.match(uri);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(MessageContract.MessageEntry.TABLE_NAME);
        switch (i) {
            case MESSAGE:
                retCursor = mOpenHelper.getReadableDatabase().query(
//                        innerJoinStr,
                        MessageContract.MessageEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case USER_MESSAGES:
                retCursor = mOpenHelper.getReadableDatabase().query(
//                        innerJoinStr,
                        MessageContract.UserMessagesEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case MESSAGE_SEARCH:
                retCursor = mOpenHelper.getReadableDatabase().query(
//                        innerJoinStr,
                        MessageContract.MessageSearchEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case MESSAGE_WITH_DATE: {
                retCursor = getMessageByDate(uri, projection, sortOrder);
                break;
            }
            case PREACHER:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MessageContract.PreachersEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case USER:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MessageContract.UserEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case LOG:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MessageContract.LogEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case EVENTS:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MessageContract.EventsEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case DECLARATIONS:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MessageContract.DeclarationsEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
//            case MESSAGE_WITH_DATE:
//                qb.appendWhere( MessageContract.MessageEntry.COLUMN_DATE + "="
//                        + uri.getPathSegments().get(1));
//                break;
            // "weather/*"
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    /*
        Student: Add the ability to insert Messages to the implementation of this function.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri = null;

        switch (match) {
            case MESSAGE: {
                //normalizeDate(values);
                long _id = db.insert(MessageContract.MessageEntry.TABLE_NAME, null, values);
                String[] args = {values.get(MessageContract.MessageEntry._ID).toString()};
                if (_id > 0)
                    returnUri = MessageContract.MessageEntry.buildMessageUri(_id);
                else {
                    values.remove(MessageContract.MessageEntry._ID);
                    _id = db.update(MessageContract.MessageEntry.TABLE_NAME, values, "_id = ?", args);
                    returnUri = MessageContract.MessageEntry.buildMessageUri(_id);
                }
                break;
            }
            case USER_MESSAGES: {
                //normalizeDate(values);
                long _id = db.insert(MessageContract.UserMessagesEntry.TABLE_NAME, null, values);
                String[] args = {values.get(MessageContract.UserMessagesEntry._ID).toString()};
                if (_id > 0)
                    returnUri = MessageContract.UserMessagesEntry.buildUserMessagesUri(_id);
                else {
                    values.remove(MessageContract.UserMessagesEntry._ID);
                    _id = db.update(MessageContract.UserMessagesEntry.TABLE_NAME, values, "_id = ?", args);
                    returnUri = MessageContract.UserMessagesEntry.buildUserMessagesUri(_id);
                }
                break;
            }
            case USER: {
                //normalizeDate(values);
                long _id = db.insert(MessageContract.UserEntry.TABLE_NAME, null, values);
                String[] args = {values.get(MessageContract.UserEntry._ID).toString()};
                if (_id > 0)
                    returnUri = MessageContract.UserEntry.buildUserUri(_id);
                else {
                    values.remove(MessageContract.UserEntry._ID);
                    _id = db.update(MessageContract.UserEntry.TABLE_NAME, values, "_id = ?", args);
                    returnUri = MessageContract.UserEntry.buildUserUri(_id);
                }
                break;
            }
            case PREACHER: {
                //normalizeDate(values);
                String[] args = {values.get(MessageContract.PreachersEntry._ID).toString()};
                long _id = db.insert(MessageContract.PreachersEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = MessageContract.PreachersEntry.buildPreacherUri(_id);
                else {
                    values.remove(MessageContract.PreachersEntry._ID);
                    _id = db.update(MessageContract.PreachersEntry.TABLE_NAME, values, "_id = ?", args);
                    returnUri = MessageContract.PreachersEntry.buildPreacherUri(_id);
                }
                break;
            }
            case LOG: {
                //normalizeDate(values);
                long _id = db.insert(MessageContract.LogEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = MessageContract.LogEntry.buildLogUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case EVENTS: {
                //normalizeDate(values);
                long _id = db.insert(MessageContract.EventsEntry.TABLE_NAME, null, values);
                String[] args = {values.get(MessageContract.EventsEntry._ID).toString()};
                if (_id > 0)
                    returnUri = MessageContract.EventsEntry.buildEventsUri(_id);
                else {
                    values.remove(MessageContract.EventsEntry._ID);
                    _id = db.update(MessageContract.EventsEntry.TABLE_NAME, values, "_id = ?", args);
                    returnUri = MessageContract.EventsEntry.buildEventsUri(_id);
                }
                break;
            }
            case DECLARATIONS: {
                //normalizeDate(values);
                long _id = db.insert(MessageContract.DeclarationsEntry.TABLE_NAME, null, values);
                String[] args = {values.get(MessageContract.DeclarationsEntry._ID).toString()};
                if (_id > 0)
                    returnUri = MessageContract.DeclarationsEntry.buildDeclarationsUri(_id);
                else {
                    values.remove(MessageContract.DeclarationsEntry._ID);
                    _id = db.update(MessageContract.DeclarationsEntry.TABLE_NAME, values, "_id = ?", args);
                    returnUri = MessageContract.DeclarationsEntry.buildDeclarationsUri(_id);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case MESSAGE:
                rowsDeleted = db.delete(
                        MessageContract.MessageEntry.TABLE_NAME, selection, selectionArgs);

                break;
            case PREACHER:
                rowsDeleted = db.delete(
                        MessageContract.PreachersEntry.TABLE_NAME, selection, selectionArgs);

                break;
            case LOG:
                rowsDeleted = db.delete(
                        MessageContract.LogEntry.TABLE_NAME, selection, selectionArgs);

                break;
            case USER:
                rowsDeleted = db.delete(
                        MessageContract.UserEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case USER_MESSAGES:
                rowsDeleted = db.delete(
                        MessageContract.UserMessagesEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case EVENTS:
                rowsDeleted = db.delete(
                        MessageContract.EventsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case DECLARATIONS:
                rowsDeleted = db.delete(
                        MessageContract.DeclarationsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

//    private void normalizeDate(ContentValues values) {
//        // normalize the date value
//        if (values.containsKey(MessageContract.MessageEntry.COLUMN_DATE)) {
//            long dateValue = values.getAsLong(MessageContract.MessageEntry.COLUMN_DATE);
//            values.put(MessageContract.MessageEntry.COLUMN_DATE, MessageContract.normalizeDate(dateValue));
//        }
//    }

    @Override
    public int update(
            @NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case MESSAGE:
                //normalizeDate(values);
                rowsUpdated = db.update(MessageContract.MessageEntry.TABLE_NAME, values, selection,
                        selectionArgs);

                break;
            case USER_MESSAGES:
                //normalizeDate(values);
                rowsUpdated = db.update(MessageContract.UserMessagesEntry.TABLE_NAME, values, selection,
                        selectionArgs);

                break;
            case EVENTS:
                //normalizeDate(values);
                rowsUpdated = db.update(MessageContract.EventsEntry.TABLE_NAME, values, selection,
                        selectionArgs);

                break;
            case DECLARATIONS:
                //normalizeDate(values);
                rowsUpdated = db.update(MessageContract.DeclarationsEntry.TABLE_NAME, values, selection,
                        selectionArgs);

                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int returnCount;
        switch (match) {
            case MESSAGE:
                db.beginTransaction();
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
//                        normalizeDate(value);
                        long _id = db.insert(MessageContract.MessageEntry.TABLE_NAME, null, value);
                        String[] args = {value.get(MessageContract.MessageEntry._ID).toString()};
                        if (_id != -1) {
                            returnCount++;
                        }
                        else {
                            value.remove(MessageContract.MessageEntry._ID);
                            _id = db.update(MessageContract.MessageEntry.TABLE_NAME, value, "_id = ?", args);
                        }
//                        if (_id <= 0)
//                            throw new android.database.SQLException("Failed to insert row into " + uri);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            case USER_MESSAGES:
                db.beginTransaction();
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
//                        normalizeDate(value);
                        long _id = db.insert(MessageContract.UserMessagesEntry.TABLE_NAME, null, value);
                        String[] args = {value.get(MessageContract.UserMessagesEntry._ID).toString()};
                        if (_id != -1) {
                            returnCount++;
                        }
                        else {
                            value.remove(MessageContract.UserMessagesEntry._ID);
                            _id = db.update(MessageContract.UserMessagesEntry.TABLE_NAME, value, "_id = ?", args);
                        }
//                        if (_id <= 0)
//                            throw new android.database.SQLException("Failed to insert row into " + uri);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            case PREACHER:
                db.beginTransaction();
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
//                        normalizeDate(value);
                        String[] args = {value.get(MessageContract.PreachersEntry._ID).toString()};
                        long _id = db.insert(MessageContract.PreachersEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                        else {
                            value.remove(MessageContract.PreachersEntry._ID);
                            _id = db.update(MessageContract.PreachersEntry.TABLE_NAME, value, "_id = ?", args);
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            case USER:
                db.beginTransaction();
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
//                        normalizeDate(value);
                        long _id = db.insert(MessageContract.UserEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            case EVENTS:
                db.beginTransaction();
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        //normalizeDate(values);
                        long _id = db.insert(MessageContract.EventsEntry.TABLE_NAME, null, value);
                        String[] args = {value.get(MessageContract.EventsEntry._ID).toString()};
                        if (_id != -1) {
                            returnCount++;
                        }
                        else {
                            value.remove(MessageContract.EventsEntry._ID);
                            _id = db.update(MessageContract.EventsEntry.TABLE_NAME, value, "_id = ?", args);
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            case DECLARATIONS:
                db.beginTransaction();
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
//                        normalizeDate(value);
                        long _id = db.insert(MessageContract.DeclarationsEntry.TABLE_NAME, null, value);
                        String[] args = {value.get(MessageContract.DeclarationsEntry._ID).toString()};
                        if (_id != -1) {
                            returnCount++;
                        }
                        else {
                            value.remove(MessageContract.DeclarationsEntry._ID);
                            _id = db.update(MessageContract.DeclarationsEntry.TABLE_NAME, value, "_id = ?", args);
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}