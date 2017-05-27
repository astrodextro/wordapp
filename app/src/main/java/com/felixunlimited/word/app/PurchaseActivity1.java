//package com.felixunlimited.word.app;
//
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.ImageButton;
//import android.widget.TextView;
//
//import com.felixunlimited.word.app.util.IabHelper;
//import com.felixunlimited.word.app.util.IabResult;
//import com.felixunlimited.word.app.util.Inventory;
//import com.felixunlimited.word.app.util.Purchase;
//
//public class PurchaseActivity extends Activity {
//
//    // Debug tag, for logging
//    static final String TAG = "Purchase";
//
//    // The helper object
//    IabHelper mHelper;
//
//    TextView messageView;
//    ImageButton googlePlayButton, airTimeButton;
//    PayLoadTask mPayLoadTask;
//    SharedPreferences mSharedPreferences;
//
//    // Does the user have the premium upgrade?
//    boolean mIsPremium = false;
//
//    // Does the user have an active subscription to the infinite msg plan?
//    boolean mSubscribedToInfiniteMsg = false;
//
//    // SKUs for our products: the premium upgrade (non-consumable) and msg (consumable)
//    static final String SKU_PREMIUM = "premium";
//    static final String SKU_MSG = "msg";
//    static final String SKU_MSG1 = "msg1";
//    static final String SKU_MSG2 = "msg2";
//    static final String SKU_DECL = "decl_sub";
//    static final String SKU_PRAY = "pray_sub";
//
//    // SKU for our subscription (infinite msg)
//    static final String SKU_INFINITE_MSG = "infinite_msg";
//
//    // (arbitrary) request code for the purchase flow
//    static final int RC_REQUEST = 10001;
//
//    // Graphics for the msg gauge
////    static int[] TANK_RES_IDS = { R.drawable.msg0, R.drawable.msg1, R.drawable.msg2,
////            R.drawable.msg3, R.drawable.msg4 };
//
//    // How many units (1/4 tank is our unit) fill in the tank.
//    static final int TANK_MAX = 4;
//
//    // Current amount of msg in tank, in units
//    int mTank;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_message_purchase);
//        String title = getIntent().getStringExtra("TITLE");
//        int price = getIntent().getIntExtra("PRICE", 200);
//
//        messageView = (TextView) findViewById(R.id.message);
//        messageView.setTextSize(20);
//        googlePlayButton = (ImageButton) findViewById(R.id.googleplay_button);
//        airTimeButton = (ImageButton) findViewById(R.id.paypal_button);
//        messageView.setText("You are buying the message:\n\n\nTitle: "+title.toUpperCase()+"\nTotal cost: N"+price+"\n\n\nPlease choose your payment method:");
//
//        googlePlayButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onBuyMsgButtonClicked(v);
//            }
//        });
//        // load game data
//        loadData();
//
//        /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
//         * (that you got from the Google Play developer console). This is not your
//         * developer public key, it's the *app-specific* public key.
//         *
//         * Instead of just storing the entire literal string here embedded in the
//         * program,  construct the key at runtime from pieces or
//         * use bit manipulation (for example, XOR with some other string) to hide
//         * the actual key.  The key itself is not secret information, but we don't
//         * want to make it easy for an attacker to replace the public key with one
//         * of their own and then fake messages from the server.
//         */
//        String base64EncodedPublicKey = Utility.stringTransform(Utility.appKey, 0xCD);
//
//        // Some sanity checks to see if the developer (that's you!) really followed the
//        // instructions to run this sample (don't put these checks on your app!)
////        if (base64EncodedPublicKey.contains("CONSTRUCT_YOUR")) {
////            throw new RuntimeException("Please put your app's public key in MainActivity.java. See README.");
////        }
////        if (getPackageName().startsWith("com.example")) {
////            throw new RuntimeException("Please change the sample's package name! See README.");
////        }
//
//        // Create the helper, passing it our context and the public key to verify signatures with
//        Log.d(TAG, "Creating IAB helper.");
//        mHelper = new IabHelper(this, base64EncodedPublicKey);
//
//        // enable debug logging (for a production application, you should set this to false).
//        mHelper.enableDebugLogging(true);
//
//        // Start setup. This is asynchronous and the specified listener
//        // will be called once setup completes.
//        Log.d(TAG, "Starting setup.");
//        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
//            public void onIabSetupFinished(IabResult result) {
//                Log.d(TAG, "Setup finished.");
//
//                if (!result.isSuccess()) {
//                    // Oh noes, there was a problem.
//                    complain("Problem setting up in-app billing: " + result);
//                    return;
//                }
//
//                // Have we been disposed of in the meantime? If so, quit.
//                if (mHelper == null) return;
//
//                // IAB is fully set up. Now, let's get an inventory of stuff we own.
//                Log.d(TAG, "Setup successful. Querying inventory.");
//                mHelper.queryInventoryAsync(mGotInventoryListener);
//            }
//        });
//    }
//
//    // Listener that's called when we finish querying the items and subscriptions we own
//    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
//        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
//            Log.d(TAG, "Query inventory finished.");
//
//            // Have we been disposed of in the meantime? If so, quit.
//            if (mHelper == null) return;
//
//            // Is it a failure?
//            if (result.isFailure()) {
//                complain("Failed to query inventory: " + result);
//                return;
//            }
//
//            Log.d(TAG, "Query inventory was successful.");
//
////            String msgPrice =
////                    inventory.getSkuDetails(SKU_MSG).getPrice();
////            String msg1Price =
////                    inventory.getSkuDetails(SKU_MSG1).getPrice();
////            String msg2Price =
////                    inventory.getSkuDetails(SKU_MSG2).getPrice();
////            String declSubPrice =
////                    inventory.getSkuDetails(SKU_DECL).getPrice();
////            String praySubPrice =
////                    inventory.getSkuDetails(SKU_PRAY).getPrice();
//            /*
//             * Check for items we own. Notice that for each purchase, we check
//             * the developer payload to see if it's correct! See
//             * verifyDeveloperPayload().
//             */
//
//            // Do we have the premium upgrade?
//            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
//            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
//            Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
//
//            // Do we have the infinite msg plan?
//            Purchase infiniteMsgPurchase = inventory.getPurchase(SKU_INFINITE_MSG);
//            mSubscribedToInfiniteMsg = (infiniteMsgPurchase != null &&
//                    verifyDeveloperPayload(infiniteMsgPurchase));
//            Log.d(TAG, "User " + (mSubscribedToInfiniteMsg ? "HAS" : "DOES NOT HAVE")
//                    + " infinite msg subscription.");
//            if (mSubscribedToInfiniteMsg) mTank = TANK_MAX;
//
//            // Check for msg delivery -- if we own msg, we should fill up the tank immediately
//            Purchase msgPurchase = inventory.getPurchase(SKU_MSG);
//            if (msgPurchase != null && verifyDeveloperPayload(msgPurchase)) {
//                Log.d(TAG, "We have msg. Consuming it.");
//                mHelper.consumeAsync(inventory.getPurchase(SKU_MSG), mConsumeFinishedListener);
//                return;
//            }
//
////            updateUi();
//            setWaitScreen(false);
//            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
//        }
//    };
//
//    // User clicked the "Buy Msg" button
//    public void onBuyMsgButtonClicked(View arg0) {
//        Log.d(TAG, "Buy msg button clicked.");
//
//        if (mSubscribedToInfiniteMsg) {
//            complain("No need! You're subscribed to infinite msg. Isn't that awesome?");
//            return;
//        }
//
//        if (mTank >= TANK_MAX) {
//            complain("Your tank is full. Drive around a bit!");
//            return;
//        }
//
//        // launch the msg purchase UI flow.
//        // We will be notified of completion via mPurchaseFinishedListener
//        setWaitScreen(true);
//        Log.d(TAG, "Launching purchase flow for msg.");
//
//        /* TODO: for security, generate your payload here for verification. See the comments on
//         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
//         *        an empty string, but on a production app you should carefully generate this. */
//        PayLoadTask payLoadTask = new PayLoadTask();
//        String payload = "";
//
//        mHelper.launchPurchaseFlow(this, SKU_MSG, RC_REQUEST,
//                mPurchaseFinishedListener, payload);
//    }
//
//    // User clicked the "Upgrade to Premium" button.
//    public void onUpgradeAppButtonClicked(View arg0) {
//        Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
//        setWaitScreen(true);
//
//        /* TODO: for security, generate your payload here for verification. See the comments on
//         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
//         *        an empty string, but on a production app you should carefully generate this. */
//        String payload = "";
//
//        mHelper.launchPurchaseFlow(this, SKU_PREMIUM, RC_REQUEST,
//                mPurchaseFinishedListener, payload);
//    }
//
//    // "Subscribe to infinite msg" button clicked. Explain to user, then start purchase
//    // flow for subscription.
//    public void onInfiniteMsgButtonClicked(View arg0) {
//        if (!mHelper.subscriptionsSupported()) {
//            complain("Subscriptions not supported on your device yet. Sorry!");
//            return;
//        }
//
//        /* TODO: for security, generate your payload here for verification. See the comments on
//         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
//         *        an empty string, but on a production app you should carefully generate this. */
//        String payload = "";
//
//        setWaitScreen(true);
//        Log.d(TAG, "Launching purchase flow for infinite msg subscription.");
//        mHelper.launchPurchaseFlow(this,
//                SKU_INFINITE_MSG, IabHelper.ITEM_TYPE_SUBS,
//                RC_REQUEST, mPurchaseFinishedListener, payload);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
//        if (mHelper == null) return;
//
//        // Pass on the activity result to the helper for handling
//        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
//            // not handled, so handle it ourselves (here's where you'd
//            // perform any handling of activity results not related to in-app
//            // billing...
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//        else {
//            Log.d(TAG, "onActivityResult handled by IABUtil.");
//        }
//    }
//
//    /** Verifies the developer payload of a purchase. */
//    boolean verifyDeveloperPayload(Purchase p) {
//        String payload = p.getDeveloperPayload();
//
//        /*
//         * TODO: verify that the developer payload of the purchase is correct. It will be
//         * the same one that you sent when initiating the purchase.
//         *
//         * WARNING: Locally generating a random string when starting a purchase and
//         * verifying it here might seem like a good approach, but this will fail in the
//         * case where the user purchases an item on one device and then uses your app on
//         * a different device, because on the other device you will not have access to the
//         * random string you originally generated.
//         *
//         * So a good developer payload has these characteristics:
//         *
//         * 1. If two different users purchase an item, the payload is different between them,
//         *    so that one user's purchase can't be replayed to another user.
//         *
//         * 2. The payload must be such that you can verify it even when the app wasn't the
//         *    one who initiated the purchase flow (so that items purchased by the user on
//         *    one device work on other devices owned by the user).
//         *
//         * Using your own server to store and verify developer payloads across app
//         * installations is recommended.
//         */
//
//        return true;
//    }
//
//    // Callback for when a purchase is finished
//    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
//        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
//            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
//
//            // if we were disposed of in the meantime, quit.
//            if (mHelper == null) return;
//
//            if (result.isFailure()) {
//                complain("Error purchasing: " + result);
//                setWaitScreen(false);
//                return;
//            }
//            if (!verifyDeveloperPayload(purchase)) {
//                complain("Error purchasing. Authenticity verification failed.");
//                setWaitScreen(false);
//                return;
//            }
//
//            Log.d(TAG, "Purchase successful.");
//
//            if (purchase.getSku().equals(SKU_MSG)) {
//                // bought 1/4 tank of msg. So consume it.
//                Log.d(TAG, "Purchase is msg. Starting msg consumption.");
//                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
//            }
//            else if (purchase.getSku().equals(SKU_PREMIUM)) {
//                // bought the premium upgrade!
//                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
//                alert("Thank you for upgrading to premium!");
//                mIsPremium = true;
////                updateUi();
//                setWaitScreen(false);
//            }
//            else if (purchase.getSku().equals(SKU_INFINITE_MSG)) {
//                // bought the infinite msg subscription
//                Log.d(TAG, "Infinite msg subscription purchased.");
//                alert("Thank you for subscribing to infinite msg!");
//                mSubscribedToInfiniteMsg = true;
//                mTank = TANK_MAX;
////                updateUi();
//                setWaitScreen(false);
//            }
//        }
//    };
//
//    // Called when consumption is complete
//    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
//        public void onConsumeFinished(Purchase purchase, IabResult result) {
//            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);
//
//            // if we were disposed of in the meantime, quit.
//            if (mHelper == null) return;
//
//            // We know this is the "msg" sku because it's the only one we consume,
//            // so we don't check which sku was consumed. If you have more than one
//            // sku, you probably should check...
//            if (result.isSuccess()) {
//                // successfully consumed, so we apply the effects of the item in our
//                // game world's logic, which in our case means filling the msg tank a bit
//                Log.d(TAG, "Consumption successful. Provisioning.");
//                mTank = mTank == TANK_MAX ? TANK_MAX : mTank + 1;
//                saveData();
//                alert("You filled 1/4 tank. Your tank is now " + String.valueOf(mTank) + "/4 full!");
//            }
//            else {
//                complain("Error while consuming: " + result);
//            }
////            updateUi();
//            setWaitScreen(false);
//            Log.d(TAG, "End consumption flow.");
//        }
//    };
//
////    // Drive button clicked. Burn msg!
////    public void onDriveButtonClicked(View arg0) {
////        Log.d(TAG, "Drive button clicked.");
////        if (!mSubscribedToInfiniteMsg && mTank <= 0) alert("Oh, no! You are out of msg! Try buying some!");
////        else {
////            if (!mSubscribedToInfiniteMsg) --mTank;
////            saveData();
////            alert("Vroooom, you drove a few miles.");
////            updateUi();
////            Log.d(TAG, "Vrooom. Tank is now " + mTank);
////        }
////    }
//
//    // We're being destroyed. It's important to dispose of the helper here!
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//
//        // very important:
//        Log.d(TAG, "Destroying helper.");
//        if (mHelper != null) {
//            mHelper.dispose();
//            mHelper = null;
//        }
//    }
//
//    // updates UI to reflect model
////    public void updateUi() {
////        // update the car color to reflect premium status or lack thereof
////        ((ImageView)findViewById(R.id.free_or_premium)).setImageResource(mIsPremium ? R.drawable.premium : R.drawable.free);
////
////        // "Upgrade" button is only visible if the user is not premium
////        findViewById(R.id.upgrade_button).setVisibility(mIsPremium ? View.GONE : View.VISIBLE);
////
////        // "Get infinite msg" button is only visible if the user is not subscribed yet
////        findViewById(R.id.infinite_msg_button).setVisibility(mSubscribedToInfiniteMsg ?
////                View.GONE : View.VISIBLE);
////
////        // update msg gauge to reflect tank status
////        if (mSubscribedToInfiniteMsg) {
////            ((ImageView)findViewById(R.id.msg_gauge)).setImageResource(R.drawable.msg_inf);
////        }
////        else {
////            int index = mTank >= TANK_RES_IDS.length ? TANK_RES_IDS.length - 1 : mTank;
////            ((ImageView)findViewById(R.id.msg_gauge)).setImageResource(TANK_RES_IDS[index]);
////        }
////    }
//
//    // Enables or disables the "please wait" screen.
//    void setWaitScreen(boolean set) {
////        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
////        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
//    }
//
//    void complain(String message) {
//        Log.e(TAG, "**** TrivialDrive Error: " + message);
//        alert("Error: " + message);
//    }
//
//    void alert(String message) {
//        AlertDialog.Builder bld = new AlertDialog.Builder(this);
//        bld.setMessage(message);
//        bld.setNeutralButton("OK", null);
//        Log.d(TAG, "Showing alert dialog: " + message);
//        bld.create().show();
//    }
//
//    void saveData() {
//
//        /*
//         * WARNING: on a real application, we recommend you save data in a secure way to
//         * prevent tampering. For simplicity in this sample, we simply store the data using a
//         * SharedPreferences.
//         */
//
//        SharedPreferences.Editor spe = getPreferences(MODE_PRIVATE).edit();
//        spe.putInt("tank", mTank);
//        spe.commit();
//        Log.d(TAG, "Saved data: tank = " + String.valueOf(mTank));
//    }
//
//    void loadData() {
//        SharedPreferences sp = getPreferences(MODE_PRIVATE);
//        mTank = sp.getInt("tank", 2);
//        Log.d(TAG, "Loaded data: tank = " + String.valueOf(mTank));
//    }
//}
