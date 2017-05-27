/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.felixunlimited.word.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felixunlimited.word.app.data.MessageContract;
import com.felixunlimited.word.app.sync.JSONParser;
import com.felixunlimited.word.app.util.IabBroadcastReceiver;
import com.felixunlimited.word.app.util.IabBroadcastReceiver.IabBroadcastListener;
import com.felixunlimited.word.app.util.IabHelper;
import com.felixunlimited.word.app.util.IabHelper.IabAsyncInProgressException;
import com.felixunlimited.word.app.util.IabResult;
import com.felixunlimited.word.app.util.Inventory;
import com.felixunlimited.word.app.util.Purchase;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static com.felixunlimited.word.app.Utility.appKey;
import static com.felixunlimited.word.app.Utility.getUniquePsuedoID;
import static com.felixunlimited.word.app.Utility.isAirplaneModeOn;
import static com.felixunlimited.word.app.Utility.isConnected;
import static com.felixunlimited.word.app.Utility.isSimSupport;
import static com.felixunlimited.word.app.Utility.log;
import static com.felixunlimited.word.app.Utility.stringTransform;
import static com.felixunlimited.word.app.Utility.updateDB;

/**
 * Example game using in-app billing version 3.
 *
 * Before attempting to run this sample, please read the README file. It
 * contains important information on how to set up this project.
 *
 * All the game-specific logic is implemented here in PaystackActivity, while the
 * general-purpose boilerplate that can be reused in any app is provided in the
 * classes in the util/ subdirectory. When implementing your own application,
 * you can copy over util/*.java to make use of those utility classes.
 *
 * This game is a simple "driving" game where the player can buy message
 * and drive. The car has a tank which stores message. When the player purchases
 * message, the tank fills up (1/4 tank at a time). When the player drives, the message
 * in the tank diminishes (also 1/4 tank at a time).
 *
 * The user can also purchase a "premium upgrade" that gives them a red car
 * instead of the standard blue one (exciting!).
 *
 * The user can also purchase a subscription ("infinite message") that allows them
 * to drive without using up any message while that subscription is active.
 *
 * It's important to note the consumption mechanics for each item.
 *
 * PREMIUM: the item is purchased and NEVER consumed. So, after the original
 * purchase, the player will always own that item. The application knows to
 * display the red car instead of the blue one because it queries whether
 * the premium "item" is owned or not.
 *
 * INFINITE MESSAGE: this is a subscription, and subscriptions can't be consumed.
 *
 * MESSAGE: when message is purchased, the "message" item is then owned. We consume it
 * when we apply that item's effects to our app's world, which to us means
 * filling up 1/4 of the tank. This happens immediately after purchase!
 * It's at this point (and not when the user drives) that the "message"
 * item is CONSUMED. Consumption should always happen when your game
 * world was safely updated to apply the effect of the purchase. So,
 * in an example scenario:
 *
 * BEFORE:      tank at 1/2
 * ON PURCHASE: tank at 1/2, "message" item is owned
 * IMMEDIATELY: "message" is consumed, tank goes to 3/4
 * AFTER:       tank at 3/4, "message" item NOT owned any more
 *
 * Another important point to notice is that it may so happen that
 * the application crashed (or anything else happened) after the user
 * purchased the "message" item, but before it was consumed. That's why,
 * on startup, we check if we own the "message" item, and, if so,
 * we have to apply its effects to our world and consume it. This
 * is also very important!
 */
public class PurchaseActivity extends Activity implements IabBroadcastListener,
        OnClickListener, View.OnClickListener {

    // Debug tag, for logging
    static final String TAG = "Purchase Activity";

    // Does the user have the premium upgrade?
    boolean mIsPremium = false;

    // Does the user have an active subscription to the infinite message plan?
    boolean mSubscribedToInfiniteMessage = false;

    // Will the subscription auto-renew?
    boolean mAutoRenewEnabled = false;

    // Tracks the currently owned infinite message SKU, and the options in the Manage dialog
    String mInfiniteMessageSku = "";
    String mFirstChoiceSku = "";
    String mSecondChoiceSku = "";

    // Used to select between purchasing message on a monthly or yearly basis
    String mSelectedSubscriptionPeriod = "";

    // SKUs for our products: the premium upgrade (non-consumable) and message (consumable)
    static final String SKU_PREMIUM = "premium";
    private String SKU_MESSAGE = "android.test.canceled";
    static final String SKU_MSG = "msg";

    // SKU for our subscription (infinite message)
    static final String SKU_INFINITE_MESSAGE_MONTHLY = "infinite_message_monthly";
    static final String SKU_INFINITE_MESSAGE_YEARLY = "infinite_message_yearly";

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

    // How many units (1/4 tank is our unit) fill in the tank.
    static final int TANK_MAX = 4;

    // Current amount of message in tank, in units
    int mTank;

    boolean mSMSSent = false;
    boolean mSMSDelivered = false;

    // The helper object
    IabHelper mHelper;

    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;

    SharedPreferences sharedPreferences;
    TextView messageView;
    Button googlePlayButton, airTimeButton, paystackButton;
    PayLoadTask mPayLoadTask;
    SharedPreferences mSharedPreferences;
    int messageID;
    String title;
    String preacher;
    String date;
    int price;
    ProgressDialog mProgressDialog;
    Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_message_purchase);
//        if (android.os.Build.VERSION.SDK_INT > 9) {
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//        }

        mContext = PurchaseActivity.this;

        if (!isConnected(this)) {
            complain("Please check your internet connectivity and try again");
            super.onDestroy();
        }

        if (isAirplaneModeOn(this)){
            complain("Please turn off airplane mode and try again");
            super.onDestroy();
        }

        messageID = getIntent().getIntExtra("ID", 0);
        title = getIntent().getStringExtra("TITLE");
        preacher = getIntent().getStringExtra("PREACHER");
        date = getIntent().getStringExtra("DATE");
        price = getIntent().getIntExtra("PRICE", 200);
        SKU_MESSAGE = String.valueOf(messageID);
        messageView = (TextView) findViewById(R.id.message);
        messageView.setTextSize(20);
        googlePlayButton = (Button) findViewById(R.id.googleplay_button);
        airTimeButton = (Button) findViewById(R.id.airtime_button);
        paystackButton = (Button) findViewById(R.id.paystack_button);
        messageView.setText("You are buying the message:\n\n\nTitle: "+title.toUpperCase()+"\nTotal cost: N"
                +price+"\n\n\nPlease choose your payment method:");

        googlePlayButton.setOnClickListener(this);
        airTimeButton.setOnClickListener(this);
        paystackButton.setOnClickListener(this);

        mProgressDialog = new ProgressDialog(PurchaseActivity.this);
        mProgressDialog.setMessage("Please wait... Sending SMS");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);//        Utility.chooseTheme(this);
        mProgressDialog.setMax(100);

        if (!isSimSupport(this)){
            airTimeButton.setVisibility(View.GONE);
        }

        // load game data
        loadData();

        /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
        String base64EncodedPublicKey = stringTransform(appKey, 0xCD);

        // Create the helper, passing it our context and the public key to verify signatures with
        log(TAG, "Creating IAB helper.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        log(TAG, "Starting setup.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    googlePlayButton.setEnabled(false);
                    airTimeButton.setEnabled(false);
                    paystackButton.setEnabled(false);
                    messageView.setText("Please check your network connection and try again.");
                    int SPLASH_TIME_OUT = 7500;
                    new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

                        @Override
                        public void run() {
                            // This method will be executed once the timer is over
                            // Start your app main activity
                            finish();
                        }
                    }, SPLASH_TIME_OUT);
                    return;
                }


                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(PurchaseActivity.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                log(TAG, "Setup successful. Querying inventory.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(PurchaseActivity.this);
        if (sharedPreferences.getInt(String.valueOf(messageID), 0) == price) {
            try {
                updateDB(PurchaseActivity.this, "complete", price, messageID, 0, System.currentTimeMillis());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Test if device can send SMS
     * @param context
     * @return
     */
    public static boolean canSendSMS(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    //---sends an SMS message to another device---//
    private void sendSMS(String phoneNumber, String message) throws JSONException {
        if (!canSendSMS(getApplicationContext())) {
            log(TAG + ":SMS", "can send sms", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
//            Toast.makeText(context, context.getString(R.string.cannot_send_sms), Toast.LENGTH_LONG).show();
            return;
        }

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                final int paid = sharedPreferences.getInt(String.valueOf(messageID), 0);
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        final SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(String.valueOf(messageID), paid + 100);
                        editor.apply();
                        if (price > paid + 100){
                            try {
                                sendSMS("131","help");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        else if (paid + 100 == price){
                            try {
                                updateDB(PurchaseActivity.this, "complete", price, messageID, 0, System.currentTimeMillis());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mProgressDialog.dismiss();
                        }
                        log(TAG + ":SMS", "SMS sent", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        if (paid > 0) {
                            try {
                                updateDB(PurchaseActivity.this, "complete", price, messageID, 0, System.currentTimeMillis());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        log(TAG + ":SMS", "Generic failure", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                        complain("SMS not sent. Generic Failure");
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        if (paid > 0) {
                            try {
                                updateDB(PurchaseActivity.this, "complete", price, messageID, 0, System.currentTimeMillis());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        log(TAG + ":SMS", "No service", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                        complain("SMS not sent. No service");
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        if (paid > 0) {
                            try {
                                updateDB(PurchaseActivity.this, "complete", price, messageID, 0, System.currentTimeMillis());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        log(TAG + ":SMS", "Null PDU", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                        complain("SMS not sent. Null PDU");
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        if (paid > 0) {
                            try {
                                updateDB(PurchaseActivity.this, "complete", price, messageID, 0, System.currentTimeMillis());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        log(TAG + ":SMS", "Radio off", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                        complain("SMS not sent. Radio off");
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        log(TAG + ":SMS", "SMS delivered", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        log(TAG + ":SMS", "SMS not delivered", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                        complain("SMS not delivered");
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        int paid = sharedPreferences.getInt(String.valueOf(messageID), 0);
        if (paid < price)
        {
            if (Utility.isConnected(mContext)){
                sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
                log(TAG + ":SMS", "Sending SMS", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
            }
            else{
                complain("Not connected, check internet connection");
                super.onDestroy();
            }
        }
        else
            mProgressDialog.dismiss();
    }

    @Override
    public void onClick(View v) {
        log(TAG, "Buy message button clicked.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

        if (mSubscribedToInfiniteMessage) {
            complain("No need! You're subscribed to infinite message. Isn't that awesome?");
            return;
        }

        if (mTank >= TANK_MAX) {
            complain("Your tank is full. Drive around a bit!");
            return;
        }

        // launch the message purchase UI flow.
        // We will be notified of completion via mPurchaseFinishedListener/
        setWaitScreen(true);
        log(TAG, "Launching purchase flow for message.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = stringTransform(stringTransform(String.valueOf(messageID), 0xCD)+preacher, 0xDC);

        if (v == googlePlayButton)
        {
            try {
                mHelper.launchPurchaseFlow(this, SKU_MESSAGE, RC_REQUEST,
                        mPurchaseFinishedListener, payload);
            } catch (IabAsyncInProgressException e) {
                complain("Error launching purchase flow. Another async operation in progress.");
                setWaitScreen(false);
            }
        }
        else if (v == paystackButton)
        {
            if (!Utility.isConnected(mContext))
            {
                complain("Sorry, you are not connected to a network");
                super.onDestroy();
            }

            Intent intent = new Intent(PurchaseActivity.this, PaystackActivity.class);
            intent.putExtra("TITLE", title);
            intent.putExtra("PREACHER", preacher);
            intent.putExtra("ID", messageID);
            intent.putExtra("DATE", date);
            intent.putExtra("PRICE", price);
            startActivity(intent);
            super.onDestroy();

            int SPLASH_TIME_OUT = 5000;
            new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

                @Override
                public void run() {
                    // This method will be executed once the timer is over
                    finish();
                }
            }, SPLASH_TIME_OUT);
        }
        else if (v == airTimeButton)
        {
            if (!Utility.isConnected(mContext))
            {
                complain("Sorry, you are not connected to a network");
                super.onDestroy();
            }

            if (sharedPreferences.getInt(String.valueOf(messageID), 0) == price) {
                try {
                    updateDB(PurchaseActivity.this, "complete", price, messageID, 0, System.currentTimeMillis());
                    super.onDestroy();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            mProgressDialog.show();
            try {
                sendSMS("131", "help");
            } catch (JSONException e) {
                e.printStackTrace();
            }
///**
//            try {
//                int paid = sharedPreferences.getInt(String.valueOf(messageID), 0);
//
//                int n = (price - paid) / 100;
//                for (int i = 0; i < n; i++)
//                {
//                    log(TAG + ":SMS", "SMS iteration: "+i, getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
//                    if(sendSMS("131", "help"))
//                    {
//                        paid += 100;
//                    }
//                    else {
//                        updateDB(PurchaseActivity.this, "partial", paid, messageID, 0, System.currentTimeMillis());
//                        mProgressDialog.dismiss();
//                        complain("SMS not sent");
//                        break;
//                    }
//                }
//
//                if (paid == price)
//                {
//                    updateDB(PurchaseActivity.this, "complete", price, messageID, 0, System.currentTimeMillis());
//                    mProgressDialog.dismiss();
//                }
//                else
//                    mProgressDialog.dismiss();
//
//            } catch (Exception e) {
//                mProgressDialog.dismiss();
//                complain("SMS not sent // " + e);
//                setWaitScreen(false);
//            }
//*/
////            try {
////                mHelper.launchPurchaseFlow(this, "android.test.purchased", RC_REQUEST,
////                        mPurchaseFinishedListener, payload);
////            } catch (IabAsyncInProgressException e) {
////                complain("Error launching purchase flow. Another async operation in progress.");
////                setWaitScreen(false);
////            }
        }
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            log(TAG, "Query inventory finished.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {

//                Purchase purchase;
//                try {
//                    purchase = new Purchase("inapp", "{\"com.felixunlimited.word.app\":\"com.felixunlimited.word.app\","+
//                            "\"orderId\":\"transactionId.android.test.purchased\","+
//                            "\"productId\":\"android.test.purchased\",\"developerPayload\":\"\",\"purchaseTime\":0,"+
//                            "\"purchaseState\":0,\"purchaseToken\":\"inapp:com.felixunlimited.word.app :android.test.purchased\"}",
//                            "");
//                    mHelper.consumeAsync(purchase, null);
//                } catch (JSONException | IabAsyncInProgressException e) {
//                    e.printStackTrace();
//                }
                complain("Failed to query inventory: " + result);
                return;
            }

            log(TAG, "Query inventory was successful.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            log(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"), getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

            // First find out which subscription is auto renewing
            Purchase messageMonthly = inventory.getPurchase(SKU_INFINITE_MESSAGE_MONTHLY);
            Purchase messageYearly = inventory.getPurchase(SKU_INFINITE_MESSAGE_YEARLY);
            if (messageMonthly != null && messageMonthly.isAutoRenewing()) {
                mInfiniteMessageSku = SKU_INFINITE_MESSAGE_MONTHLY;
                mAutoRenewEnabled = true;
            } else if (messageYearly != null && messageYearly.isAutoRenewing()) {
                mInfiniteMessageSku = SKU_INFINITE_MESSAGE_YEARLY;
                mAutoRenewEnabled = true;
            } else {
                mInfiniteMessageSku = "";
                mAutoRenewEnabled = false;
            }

            // The user is subscribed if either subscription exists, even if neither is auto
            // renewing
            mSubscribedToInfiniteMessage = (messageMonthly != null && verifyDeveloperPayload(messageMonthly))
                    || (messageYearly != null && verifyDeveloperPayload(messageYearly));
            log(TAG, "User " + (mSubscribedToInfiniteMessage ? "HAS" : "DOES NOT HAVE")
                    + " infinite message subscription.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
            if (mSubscribedToInfiniteMessage) mTank = TANK_MAX;

            // Check for message delivery -- if we own message, we should fill up the tank immediately
            Purchase messagePurchase = inventory.getPurchase(SKU_MESSAGE);
            if (messagePurchase != null && verifyDeveloperPayload(messagePurchase)) {
                Toast.makeText(PurchaseActivity.this, "You already own this message", Toast.LENGTH_LONG).show();
                log(TAG, "We have message. Consuming it.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                try {
                    mHelper.consumeAsync(inventory.getPurchase(SKU_MESSAGE), mConsumeFinishedListener);
                } catch (IabAsyncInProgressException e) {
                    complain("Error consuming message. Another async operation in progress.");
                }
                return;
            }

            // Own another message
            if (messagePurchase != null && !verifyDeveloperPayload(messagePurchase)) {
                Toast.makeText(PurchaseActivity.this, "You already own this message", Toast.LENGTH_LONG).show();
                log(TAG, "We have message. Consuming it.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                try {
                    mHelper.consumeAsync(inventory.getPurchase(SKU_MESSAGE), mConsumeFinishedListener);
                } catch (IabAsyncInProgressException e) {
                    complain("Error consuming message. Another async operation in progress.");
                }
                return;
            }

            updateUi();
            setWaitScreen(false);
            log(TAG, "Initial inventory query finished; enabling main UI.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        }
    };

    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        log(TAG, "Received broadcast notification. Querying inventory.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
    }

    // User clicked the "Buy Message" button
    public void onBuyMessageButtonClicked(View arg0) {
        log(TAG, "Buy message button clicked.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

        if (mSubscribedToInfiniteMessage) {
            complain("No need! You're subscribed to infinite message. Isn't that awesome?");
            return;
        }

        if (mTank >= TANK_MAX) {
            complain("Your tank is full. Drive around a bit!");
            return;
        }

        // launch the message purchase UI flow.
        // We will be notified of completion via mPurchaseFinishedListener
        setWaitScreen(true);
        log(TAG, "Launching purchase flow for message.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = stringTransform(stringTransform(String.valueOf(messageID), 0xCD)+preacher, 0xDC);

        try {
            mHelper.launchPurchaseFlow(this, SKU_MESSAGE, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        } catch (IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
            setWaitScreen(false);
        }
    }

    // User clicked the "Upgrade to Premium" button.
    public void onUpgradeAppButtonClicked(View arg0) {
        log(TAG, "Upgrade button clicked; launching purchase flow for upgrade.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        setWaitScreen(true);

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";

        try {
            mHelper.launchPurchaseFlow(this, SKU_PREMIUM, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        } catch (IabAsyncInProgressException e) {
            complain("Error launching purchase flow. Another async operation in progress.");
            setWaitScreen(false);
        }
    }

    // "Subscribe to infinite message" button clicked. Explain to user, then start purchase
    // flow for subscription.
    public void onInfiniteMessageButtonClicked(View arg0) {
        if (!mHelper.subscriptionsSupported()) {
            complain("Subscriptions not supported on your device yet. Sorry!");
            return;
        }

        CharSequence[] options;
        if (!mSubscribedToInfiniteMessage || !mAutoRenewEnabled) {
            // Both subscription options should be available
            options = new CharSequence[2];
            options[0] = getString(R.string.subscription_period_monthly);
            options[1] = getString(R.string.subscription_period_yearly);
            mFirstChoiceSku = SKU_INFINITE_MESSAGE_MONTHLY;
            mSecondChoiceSku = SKU_INFINITE_MESSAGE_YEARLY;
        } else {
            // This is the subscription upgrade/downgrade path, so only one option is valid
            options = new CharSequence[1];
            if (mInfiniteMessageSku.equals(SKU_INFINITE_MESSAGE_MONTHLY)) {
                // Give the option to upgrade to yearly
                options[0] = getString(R.string.subscription_period_yearly);
                mFirstChoiceSku = SKU_INFINITE_MESSAGE_YEARLY;
            } else {
                // Give the option to downgrade to monthly
                options[0] = getString(R.string.subscription_period_monthly);
                mFirstChoiceSku = SKU_INFINITE_MESSAGE_MONTHLY;
            }
            mSecondChoiceSku = "";
        }

        int titleResId;
        if (!mSubscribedToInfiniteMessage) {
            titleResId = R.string.subscription_period_prompt;
        } else if (!mAutoRenewEnabled) {
            titleResId = R.string.subscription_resignup_prompt;
        } else {
            titleResId = R.string.subscription_update_prompt;
        }

        Builder builder = new Builder(this);
        builder.setTitle(titleResId)
                .setSingleChoiceItems(options, 0 /* checkedItem */, this)
                .setPositiveButton(R.string.subscription_prompt_continue, this)
                .setNegativeButton(R.string.subscription_prompt_cancel, this);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int id) {
        if (id == 0 /* First choice item */) {
            mSelectedSubscriptionPeriod = mFirstChoiceSku;
        } else if (id == 1 /* Second choice item */) {
            mSelectedSubscriptionPeriod = mSecondChoiceSku;
        } else if (id == DialogInterface.BUTTON_POSITIVE /* continue button */) {
            /* TODO: for security, generate your payload here for verification. See the comments on
             *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
             *        an empty string, but on a production app you should carefully generate
             *        this. */
            String payload = stringTransform(stringTransform(String.valueOf(messageID), 0xCD)+preacher, 0xDC);

            if (TextUtils.isEmpty(mSelectedSubscriptionPeriod)) {
                // The user has not changed from the default selection
                mSelectedSubscriptionPeriod = mFirstChoiceSku;
            }

            List<String> oldSkus = null;
            if (!TextUtils.isEmpty(mInfiniteMessageSku)
                    && !mInfiniteMessageSku.equals(mSelectedSubscriptionPeriod)) {
                // The user currently has a valid subscription, any purchase action is going to
                // replace that subscription
                oldSkus = new ArrayList<String>();
                oldSkus.add(mInfiniteMessageSku);
            }

            setWaitScreen(true);
            log(TAG, "Launching purchase flow for message subscription.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
            try {
                mHelper.launchPurchaseFlow(this, mSelectedSubscriptionPeriod, IabHelper.ITEM_TYPE_SUBS,
                        oldSkus, RC_REQUEST, mPurchaseFinishedListener, payload);
            } catch (IabAsyncInProgressException e) {
                complain("Error launching purchase flow. Another async operation in progress.");
                setWaitScreen(false);
            }
            // Reset the dialog options
            mSelectedSubscriptionPeriod = "";
            mFirstChoiceSku = "";
            mSecondChoiceSku = "";
        } else if (id != DialogInterface.BUTTON_NEGATIVE) {
            // There are only four buttons, this should not happen
            log(TAG, "Unknown button clicked in subscription dialog: " + id, getUniquePsuedoID(), System.currentTimeMillis(), 'e', null, PurchaseActivity.this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        log(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data, getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            log(TAG, "onActivityResult handled by IABUtil.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        }
//        if (requestCode == REQUEST_PURCHASE) {
//
//            //this ensures that the mHelper.flagEndAsync() gets called
//            //prior to starting a new async request.
//            mHelper.handleActivityResult(requestCode, resultCode, data);
//
//            //get needed data from Intent extra to recreate product object
//            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
//            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
//            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
//
//            // Strip out getActivity() if not being used within a fragment
//            if (resultCode == RESULT_OK) {
//                try {
//                    JSONObject jo = new JSONObject(purchaseData);
//                    String sku = jo.getString("productId");
//
//                    //only auto consume the android.test.purchased product
//                    if (sku.equals("android.test.purchased")) {
//                        //build the purchase object from the response data
//                        Purchase purchase = new Purchase("inapp", purchaseData, dataSignature);
//                        //consume android.test.purchased
//                        mHelper.consumeAsync(purchase,null);
//                    }
//                } catch (JSONException je) {
//                    //failed to parse the purchase data
//                    je.printStackTrace();
//                } catch (IllegalStateException ise) {
//                    //most likely either disposed, not setup, or
//                    //another billing async process is already running
//                    ise.printStackTrace();
//                } catch (Exception e) {
//                    //unexpected error
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        if (payload != stringTransform(stringTransform(String.valueOf(messageID), 0xCD)+preacher, 0xDC))
            return false;

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            log(TAG, "Purchase finished: " + result + ", purchase: " + purchase, getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            log(TAG, "Purchase successful.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

            if (purchase.getSku().equals(SKU_MESSAGE)) {
                // bought 1/4 tank of message. So consume it.
                log(TAG, "Purchase is message. Starting message consumption.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                try {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                } catch (IabAsyncInProgressException e) {
                    complain("Error consuming message. Another async operation in progress.");
                    setWaitScreen(false);
                    return;
                }
            }
            else if (purchase.getSku().equals(SKU_PREMIUM)) {
                // bought the premium upgrade!
                log(TAG, "Purchase is premium upgrade. Congratulating user.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                alert("Thank you for upgrading to premium!");
                mIsPremium = true;
                updateUi();
                setWaitScreen(false);
            }
            else if (purchase.getSku().equals(SKU_INFINITE_MESSAGE_MONTHLY)
                    || purchase.getSku().equals(SKU_INFINITE_MESSAGE_YEARLY)) {
                // bought the infinite message subscription
                log(TAG, "Infinite message subscription purchased.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
                alert("Thank you for subscribing to infinite message!");
                mSubscribedToInfiniteMessage = true;
                mAutoRenewEnabled = purchase.isAutoRenewing();
                mInfiniteMessageSku = purchase.getSku();
                mTank = TANK_MAX;
                updateUi();
                setWaitScreen(false);
            }
        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            log(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result, getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "message" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the message tank a bit
                log(TAG, "Consumption successful. Provisioning.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);

                try {
                    updateDB(PurchaseActivity.this, "complete", price, messageID, 0, System.currentTimeMillis());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else {
                complain("Error while consuming: " + result);
            }
            updateUi();
            setWaitScreen(false);
            log(TAG, "End consumption flow.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        }
    };


    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
        log(TAG, "Destroying helper.", getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

    // updates UI to reflect model
    public void updateUi() {
        // update the car color to reflect premium status or lack thereof
//        ((ImageView)findViewById(R.id.free_or_premium)).setImageResource(mIsPremium ? R.drawable.premium : R.drawable.free);
//
//        // "Upgrade" button is only visible if the user is not premium
//        findViewById(R.id.upgrade_button).setVisibility(mIsPremium ? View.GONE : View.VISIBLE);
//
//        ImageView infiniteMessageButton = (ImageView) findViewById(R.id.infinite_message_button);
//        if (mSubscribedToInfiniteMessage) {
//            // If subscription is active, show "Manage Infinite Message"
//            infiniteMessageButton.setImageResource(R.drawable.manage_infinite_message);
//        } else {
//            // The user does not have infinite message, show "Get Infinite Message"
//            infiniteMessageButton.setImageResource(R.drawable.get_infinite_message);
//        }
//
//        // update message gauge to reflect tank status
//        if (mSubscribedToInfiniteMessage) {
//            ((ImageView)findViewById(R.id.message_gauge)).setImageResource(R.drawable.message_inf);
//        }
//        else {
//            int index = mTank >= TANK_RES_IDS.length ? TANK_RES_IDS.length - 1 : mTank;
//            ((ImageView)findViewById(R.id.message_gauge)).setImageResource(TANK_RES_IDS[index]);
//        }
    }

    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
//        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
//        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }

    void complain(String message) {
        log(TAG, "**** Purchase Activity Error: " + message, getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        if (mProgressDialog != null)
            mProgressDialog.dismiss();
        alert("Error: " + message);
    }

    void alert(String message) {
        Builder bld = new Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        log(TAG, "Showing alert dialog: " + message, getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
        bld.create().show();
    }

    void saveData() {

        /*
         * WARNING: on a real application, we recommend you save data in a secure way to
         * prevent tampering. For simplicity in this sample, we simply store the data using a
         * SharedPreferences.
         */

        SharedPreferences.Editor spe = getPreferences(MODE_PRIVATE).edit();
        spe.putInt("tank", mTank);
        spe.apply();
        Log.d(TAG, "Saved data: tank = " + String.valueOf(mTank));
        log(TAG, "Saved data: tank = " + String.valueOf(mTank), getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
    }

    void loadData() {
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        mTank = sp.getInt("tank", 2);
        log(TAG, "Loaded data: tank = " + String.valueOf(mTank), getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, PurchaseActivity.this);
    }

    private void updateUserMessages (int amtPaid, long timeStamp) {
        ContentValues contentValues = new ContentValues();
        JSONParser jsonParser = new JSONParser();

        contentValues.put(MessageContract.MessageEntry.COLUMN_PAID, amtPaid);
        contentValues.put(MessageContract.MessageEntry.COLUMN_TIMESTAMP, timeStamp);
        contentValues.put(MessageContract.MessageEntry.COLUMN_UPDATED, 0);
        getContentResolver().update(MessageContract.MessageEntry.CONTENT_URI, contentValues,
                MessageContract.UserMessagesEntry._ID, new String[]{String.valueOf(messageID)});

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(String.valueOf(messageID), amtPaid);
        editor.apply();


        saveData();
    }

    private void provision() {
//        JSONObject jsonObject = new JSONObject();
        ContentValues contentValues = new ContentValues();
//        JSONParser jsonParser = new JSONParser();
        String response = "no";

        contentValues.put(MessageContract.MessageEntry.COLUMN_PAID, price);
        contentValues.put(MessageContract.MessageEntry.COLUMN_TIMESTAMP, System.currentTimeMillis());
        contentValues.put(MessageContract.MessageEntry.COLUMN_PURCHASED, 1);
        getContentResolver().update(MessageContract.MessageEntry.CONTENT_URI, contentValues,
                MessageContract.UserMessagesEntry._ID, new String[]{String.valueOf(messageID)});


        saveData();
    }
}