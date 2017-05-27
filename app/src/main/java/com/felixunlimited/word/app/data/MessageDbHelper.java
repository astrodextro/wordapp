package com.felixunlimited.word.app.data;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.felixunlimited.word.app.data.MessageContract.MessageSearchEntry;

import java.util.ArrayList;

import static com.felixunlimited.word.app.data.MessageContract.*;
import static com.felixunlimited.word.app.data.MessageContract.LogEntry;
import static com.felixunlimited.word.app.data.MessageContract.MessageEntry;
import static com.felixunlimited.word.app.data.MessageContract.PreachersEntry;
import static com.felixunlimited.word.app.data.MessageContract.UserEntry;

class MessageDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
//    yjasmine7@gmail.com, min.preciouscoza@yahoo.com, zipporahangyu@rcsi.ie, justinankiru@yahoo.com, tega.isiorho@gmail.com, ttblossoms@gmail.com, aino_marvelous@yahoo.com, sogadav01@yahoo.com, topeajayi2014@gmail.com, justinankiru@yahoo.com, soloji5000@yahoo.com, op4rill2006@gmail.com, anyankpeletimi@gmail.com
//    zipporahangyu@yahoo.com, hisaac101@gmail.com, contactdrbunmi@gmail.com, faith4chioma@outlook.com, yinkaobasun@gmail.com, wkasifa@yahoo.com, ureukefi@yahoo.co.uk, joeikp@yahoo.com, busayomoriyonu@gmail.com, stephensuotonye@gmail.com, astrodextro@gmail.com, angyepagella@gmail.com

//justinankiru@yahoo.com, aino_marvelous@yahoo.com, sogadav01@yahoo.com, justinankiru@yahoo.com, soloji5000@yahoo.com, hisaac101@yahoo.com, faith4chioma@outlook.com, wkasifa@yahoo.com, ureukefi@yahoo.co.uk and angyepagella@gmail.com.
    private static final String DATABASE_NAME = "Messages.db";

    MessageDbHelper(Context context)
    {
        super(context, DATABASE_NAME , null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String SQL_CREATE_MESSAGE_TABLE = "create table " + MessageEntry.TABLE_NAME +
                "( " +
                MessageEntry._ID + " integer primary key not null," +
                MessageEntry.COLUMN_DATE + " text not null," +
                MessageEntry.COLUMN_TITLE + " text not null," +
                MessageEntry.COLUMN_PREACHER + " text not null," +
                MessageEntry.COLUMN_PREACHER_KEY + " integer not null," +
                MessageEntry.COLUMN_CATEGORY + " text not null," +
                MessageEntry.COLUMN_OVERVIEW + " text not null," +
                MessageEntry.COLUMN_DOWNLOADS + " integer not null," +
                MessageEntry.COLUMN_PURCHASES + " integer not null," +
                MessageEntry.COLUMN_STREAMS + " integer not null," +
                MessageEntry.COLUMN_RATING + " integer not null," +
                MessageEntry.COLUMN_PRICE + " integer not null," +
                MessageEntry.COLUMN_DOWNLOADED + " integer," +
                MessageEntry.COLUMN_PURCHASED + " integer," +
                MessageEntry.COLUMN_PAID + " integer," +
                MessageEntry.COLUMN_TIMESTAMP + " text," +
                MessageEntry.COLUMN_UPDATED + " integer," +

                // Set up the location column as a foreign key to location table.
                " FOREIGN KEY (" + MessageEntry.COLUMN_PREACHER_KEY + ") REFERENCES " +
                PreachersEntry.TABLE_NAME + " (" + PreachersEntry._ID + "), " +

                // To assure the application have just one message entry per day
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + MessageEntry._ID + ") ON CONFLICT REPLACE" +
                ");";

        final String SQL_CREATE_USER_MESSAGES_TABLE = "create table " + UserMessagesEntry.TABLE_NAME +
                "( " +
                UserMessagesEntry._ID + " integer primary key not null," +
                UserMessagesEntry.COLUMN_PRICE + " integer not null," +
                UserMessagesEntry.COLUMN_PAID + " integer not null," +
                UserMessagesEntry.COLUMN_DATE + " text not null," +
                UserMessagesEntry.COLUMN_TITLE + " text not null," +
                UserMessagesEntry.COLUMN_PREACHER + " text not null," +
                UserMessagesEntry.COLUMN_PREACHER_KEY + " integer not null," +
                UserMessagesEntry.COLUMN_CATEGORY + " text not null," +
                UserMessagesEntry.COLUMN_OVERVIEW + " text not null," +
                UserMessagesEntry.COLUMN_DOWNLOADS + " integer not null," +
                UserMessagesEntry.COLUMN_DOWNLOADED + " integer not null," +
                UserMessagesEntry.COLUMN_PURCHASES + " integer not null," +
                UserMessagesEntry.COLUMN_STREAMS + " integer not null," +
                UserMessagesEntry.COLUMN_RATING + " integer not null," +
                UserMessagesEntry.COLUMN_PURCHASED + " integer not null," +
                UserMessagesEntry.COLUMN_TIMESTAMP + " text not null," +

                // Set up the location column as a foreign key to location table.
                " FOREIGN KEY (" + UserMessagesEntry.COLUMN_PREACHER_KEY + ") REFERENCES " +
                PreachersEntry.TABLE_NAME + " (" + PreachersEntry._ID + "), " +

                // To assure the application have just one message entry per day
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + UserMessagesEntry._ID + ") ON CONFLICT REPLACE" +
                ");";

        final String SQL_CREATE_MESSAGE_SEARCH_TABLE = "create table " + MessageSearchEntry.TABLE_NAME +
                "( " +
                MessageSearchEntry._ID + " integer primary key not null," +
                MessageSearchEntry.COLUMN_DATE + " text not null," +
                MessageSearchEntry.COLUMN_TITLE + " text not null," +
                MessageSearchEntry.COLUMN_PREACHER + " text not null," +
                MessageSearchEntry.COLUMN_PREACHER_KEY + " integer not null," +
                MessageSearchEntry.COLUMN_CATEGORY + " text not null," +
                MessageSearchEntry.COLUMN_OVERVIEW + " text not null," +
                MessageSearchEntry.COLUMN_DOWNLOADS + " integer not null," +
                MessageSearchEntry.COLUMN_PURCHASES + " integer not null," +
                MessageSearchEntry.COLUMN_STREAMS + " integer not null," +
                MessageSearchEntry.COLUMN_RATING + " integer not null," +
                MessageSearchEntry.COLUMN_DOWNLOADED + " integer not null," +
                MessageSearchEntry.COLUMN_PURCHASED + " integer not null," +
                MessageSearchEntry.COLUMN_PRICE + " integer not null," +

                // Set up the location column as a foreign key to location table.
                " FOREIGN KEY (" + MessageSearchEntry.COLUMN_PREACHER_KEY + ") REFERENCES " +
                PreachersEntry.TABLE_NAME + " (" + PreachersEntry._ID + "), " +

                // To assure the application have just one message entry per day
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + MessageSearchEntry._ID + ") ON CONFLICT REPLACE" +
                ");";

        final String SQL_CREATE_PREACHERS_TABLE = "create table " + PreachersEntry.TABLE_NAME +
                "( " +
                PreachersEntry._ID + " integer primary key not null," +
  //              PreachersEntry.COLUMN_PREACHER_ID + " integer not null," +
                PreachersEntry.COLUMN_NAME + " text not null," +
                PreachersEntry.COLUMN_MINISTRY + " text not null," +
                PreachersEntry.COLUMN_BIRTHDAY + " text not null," +
                PreachersEntry.COLUMN_NO_OF_MESSAGES + " integer not null," +
                PreachersEntry.COLUMN_STREAMS + " integer not null," +
                PreachersEntry.COLUMN_DOWNLOADS + " integer not null," +
                PreachersEntry.COLUMN_PURCHASES + " integer not null," +
                PreachersEntry.COLUMN_INTERNAL_EXTERNAL + " text not null" +
                ");";

        final String SQL_CREATE_USER_TABLE = "create table " + UserEntry.TABLE_NAME +
                "( " +
                UserEntry._ID + " integer primary key not null," +
                UserEntry.COLUMN_USER_EMAIL + " text not null," +
                UserEntry.COLUMN_DOWNLOADS + " integer not null," +
                UserEntry.COLUMN_PURCHASES + " integer not null," +
                UserEntry.COLUMN_STREAMS + " integer not null," +
                UserEntry.COLUMN_DECLARATIONS_SUBSCRIPTION + " integer not null," +
                UserEntry.COLUMN_PRAYERS_SUBSCRIPTION + " integer not null" +
                ");";

        final String SQL_CREATE_LOG_TABLE = "create table " + LogEntry.TABLE_NAME +
                "( " +
                LogEntry._ID + " integer primary key autoincrement not null," +
                LogEntry.COLUMN_USER_ID + " text not null," +
                LogEntry.COLUMN_EVENT + " text not null," +
                LogEntry.COLUMN_TIMESTAMP + " text not null" +
                ");";

        final String SQL_CREATE_EVENTS_TABLE = "create table " + EventsEntry.TABLE_NAME +
                "( " +
                EventsEntry._ID + " integer primary key not null," +
                EventsEntry.COLUMN_DATE + " text not null," +
                EventsEntry.COLUMN_TIME + " text not null," +
                EventsEntry.COLUMN_TITLE + " text not null," +
                EventsEntry.COLUMN_VENUE + " text not null," +
                EventsEntry.COLUMN_DESCRIPTION + " text not null," +
                EventsEntry.COLUMN_IS_VALID + " int not null," +
                EventsEntry.COLUMN_PRIORITY + " int not null," +
                EventsEntry.COLUMN_TIMESTAMP + " text not null," +
//                EventsEntry.COLUMN_IMAGE_FILE + " text not null," +

                " UNIQUE (" + EventsEntry._ID + ") ON CONFLICT REPLACE" +
                ");";

        final String SQL_CREATE_DECLARATIONS_TABLE = "create table " + DeclarationsEntry.TABLE_NAME +
                "( " +
                DeclarationsEntry._ID + " integer primary key not null," +
                DeclarationsEntry.COLUMN_TITLE + " text not null," +
                DeclarationsEntry.COLUMN_CATEGORY + " text not null," +
//                DeclarationsEntry.COLUMN_AUDIO_FILE + " text not null," +
                DeclarationsEntry.COLUMN_DATE + " text not null," +
                DeclarationsEntry.COLUMN_DOWNLOADED + " int not null default 0," +
  //              DeclarationsEntry.COLUMN_TEXT_FILE + " text not null," +
                DeclarationsEntry.COLUMN_PREACHER_KEY + " int not null default 0," +
                DeclarationsEntry.COLUMN_TIMESTAMP + " text not null," +
                // Set up the location column as a foreign key to location table.
                " FOREIGN KEY (" + DeclarationsEntry.COLUMN_PREACHER_KEY + ") REFERENCES " +
                PreachersEntry.TABLE_NAME + " (" + PreachersEntry._ID + "), " +

                // To assure the application have just one message entry per day
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + DeclarationsEntry._ID + ") ON CONFLICT REPLACE" +
                ");";

        db.execSQL(SQL_CREATE_MESSAGE_TABLE);
        db.execSQL(SQL_CREATE_MESSAGE_SEARCH_TABLE);
        db.execSQL(SQL_CREATE_USER_TABLE);
        db.execSQL(SQL_CREATE_PREACHERS_TABLE);
        db.execSQL(SQL_CREATE_LOG_TABLE);
        db.execSQL(SQL_CREATE_EVENTS_TABLE);
        db.execSQL(SQL_CREATE_DECLARATIONS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS " + MessageEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UserMessagesEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PreachersEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LogEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + EventsEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DeclarationsEntry.TABLE_NAME);
        onCreate(db);
    }

    ArrayList<Cursor> getData(String Query){
        //get writable database
        SQLiteDatabase sqlDB = this.getWritableDatabase();
        String[] columns = new String[] { "mesage" };
        //an array list of cursor to save two cursors one has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
        MatrixCursor Cursor2= new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);


        try{
            String maxQuery = Query ;
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(maxQuery, null);


            //add value to cursor2
            Cursor2.addRow(new Object[] { "Success" });

            alc.set(1,Cursor2);
            if (null != c && c.getCount() > 0) {


                alc.set(0,c);
                c.moveToFirst();

                return alc ;
            }
            return alc;
        } catch(SQLException sqlEx){
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+sqlEx.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        } catch(Exception ex){

            Log.d("printing exception", ex.getMessage());

            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+ex.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        }


    }
}