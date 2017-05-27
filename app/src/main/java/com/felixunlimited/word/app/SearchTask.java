//package com.felixunlimited.word.app;
//
//import android.content.ContentValues;
//import android.os.AsyncTask;
//
//import com.felixunlimited.word.app.data.MessageContract;
//import com.felixunlimited.word.app.sync.JSONParser;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.util.Vector;
//
///**
// * Created by Helen on 05/06/2016.
// */
//public class SearchTask extends AsyncTask<String, Void, String> {
//
//    private MessagesSearchActivity messagesSearchActivity;
//
//    public SearchTask(MessagesSearchActivity messagesSearchActivity) {
//        this.messagesSearchActivity = messagesSearchActivity;
//    }
//
//    @Override
//    protected void onPreExecute() {
//        super.onPreExecute();
//    }
//
//    @Override
//    protected String doInBackground(String... params) {
//        final String WEBSERVICE_URL =
//                "http://www.felixunlimited.com/webservice.php";
//        JSONParser jsonParser = new JSONParser();
//        try {
//            getMessageDataFromJson(jsonParser.makeHttpRequest(WEBSERVICE_URL, "POST", params[0], "messages").toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    @Override
//    protected void onPostExecute(String strings) {
//        UserMessagesListFragment userMessagesListFragment = ((UserMessagesListFragment) messagesSearchActivity.getSupportFragmentManager()
//                .findFragmentById(R.id.messages_list));
////        userMessagesListFragment.setUseTodayLayout(!messagesSearchActivity.mTwoPane);
//        super.onPostExecute(strings);
//    }
//
//    private void getMessageDataFromJson(String messageJsonStr)
//            throws JSONException {
//
//        // Now we have a String representing the complete message in JSON Format.
//        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
//        // into an Object hierarchy for us.
//
//        // These are the names of the JSON objects that need to be extracted.
//
//
//        // Message information.  Each day's message info is an element of the "list" array.
//        final String RESPONSE = "response";
//        final String COUNT = "count";
//        final String END = "end";
//        final String OWM_MESSAGE_ID = "id";
//        final String OWM_TITLE = "title";
//        final String OWM_PREACHER = "preacher";
//        final String OWM_PREACHER_KEY = "preacher_key";
//        final String OWM_CATEGORY = "category";
//        final String OWM_OVERVIEW = "overview";
//        final String OWM_DATE = "date";
//        final String OWM_NO_OF_DOWNLOADS = "no_of_downloads";
//        final String OWM_PURCHASED = "purchased";
//        final String OWM_DOWNLOADED = "downloaded";
//        final String OWM_PRICE = "price";
//
//        try {
//            JSONObject messageJson = new JSONObject(messageJsonStr);
//            JSONArray messageArray = messageJson.getJSONArray(RESPONSE);
//            messagesSearchActivity.mCount = messageJson.getInt(COUNT);
//            messagesSearchActivity.mEnd = messageJson.getInt(END);
//
//            // Insert the new message information into the database
//            Vector<ContentValues> cVVector = new Vector<ContentValues>(messageArray.length());
//
//            for (int i = 0; i < messageArray.length(); i++) {
//                // These are the values that will be collected.
//                String title;
//                String preacher;
//                String category;
//                String overview;
//
//                String no_of_downloads;
//                int preacher_key;
//                int messageId;
//
//                // Get the JSON object representing the day
//                JSONObject message = messageArray.getJSONObject(i);
//
//                title = message.getString(OWM_TITLE);
//                preacher = message.getString(OWM_PREACHER);
//                category = message.getString(OWM_CATEGORY);
//                overview = message.getString(OWM_OVERVIEW);
//                no_of_downloads = message.getString(OWM_NO_OF_DOWNLOADS);
//                messageId = message.getInt(OWM_MESSAGE_ID);
//                preacher_key = message.getInt(OWM_PREACHER_KEY);
//                String date = message.getString(OWM_DATE);
//                int purchased = message.getInt(OWM_PURCHASED);
//                int downloaded = message.getInt(OWM_DOWNLOADED);
//                int price = message.getInt(OWM_PRICE);
//
//                ContentValues messageValues = new ContentValues();
//
//                messageValues.put(MessageContract.MessageEntry._ID, messageId);
//                messageValues.put(MessageContract.MessageEntry.COLUMN_DATE, date);
//                messageValues.put(MessageContract.MessageEntry.COLUMN_PREACHER_KEY, preacher_key);
//                messageValues.put(MessageContract.MessageEntry.COLUMN_PREACHER, preacher);
//                messageValues.put(MessageContract.MessageEntry.COLUMN_TITLE, title);
//                messageValues.put(MessageContract.MessageEntry.COLUMN_CATEGORY, category);
//                messageValues.put(MessageContract.MessageEntry.COLUMN_OVERVIEW, overview);
//                messageValues.put(MessageContract.MessageEntry.COLUMN_DOWNLOADS, no_of_downloads);
//                messageValues.put(MessageContract.MessageEntry.COLUMN_STREAMS, downloaded);
//                messageValues.put(MessageContract.MessageEntry.COLUMN_PURCHASES, purchased);
//                messageValues.put(MessageContract.MessageEntry.COLUMN_PRICE, price);
//
//                cVVector.add(messageValues);
//            }
//
//            if (cVVector.size() > 0) {
//                ContentValues[] cvArray = new ContentValues[cVVector.size()];
//                cVVector.toArray(cvArray);
//                messagesSearchActivity.getContentResolver().delete(MessageContract.MessageSearchEntry.CONTENT_URI, null, null);
//                messagesSearchActivity.getContentResolver().bulkInsert(MessageContract.MessageEntry.CONTENT_URI, cvArray);
//            }
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//}
