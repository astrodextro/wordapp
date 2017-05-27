package com.felixunlimited.word.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import co.paystack.android.Paystack;
import co.paystack.android.PaystackSdk;
import co.paystack.android.model.Card;
import co.paystack.android.model.Charge;
import co.paystack.android.model.Token;
import co.paystack.android.model.Transaction;

import static com.felixunlimited.word.app.Utility.getEmail;
import static com.felixunlimited.word.app.Utility.updateDB;

public class PaystackActivity extends AppCompatActivity {

    //private static final String PUBLIC_KEY = "your public key";


    EditText mEditCardNum;
    EditText mEditCVC;
    EditText mEditExpiryMonth;
    EditText mEditExpiryYear;
    Button mButtonCreateToken;

    TextView mTextCard;
    TextView mTextToken;

    Token token;
    Card card;

    ProgressDialog dialog;
    private EditText mEditAmountInKobo;
    private EditText mEditEmail;
    private Button mButtonPerformTransaction;
    private TextView mTextReference;
    private Charge charge;
    private Transaction transaction;

    private String title, date, preacher;
    private int messageID, price;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paystack);

        messageID = getIntent().getIntExtra("ID", 0);
        title = getIntent().getStringExtra("TITLE");
        preacher = getIntent().getStringExtra("PREACHER");
        date = getIntent().getStringExtra("DATE");
        price = getIntent().getIntExtra("PRICE", 200);

        mEditCardNum = (EditText) findViewById(R.id.edit_card_number);
        mEditCVC = (EditText) findViewById(R.id.edit_cvc);
        mEditExpiryMonth = (EditText) findViewById(R.id.edit_expiry_month);
        mEditExpiryYear = (EditText) findViewById(R.id.edit_expiry_year);
        mEditEmail = (EditText) findViewById(R.id.edit_email);
        mEditAmountInKobo = (EditText) findViewById(R.id.edit_amount_in_kobo);

        mButtonCreateToken = (Button) findViewById(R.id.button_create_token);
        mButtonPerformTransaction = (Button) findViewById(R.id.button_perform_transaction);

        mTextCard = (TextView) findViewById(R.id.textview_card);
        mTextToken = (TextView) findViewById(R.id.textview_token);
        mTextReference = (TextView) findViewById(R.id.textview_reference);

        mEditEmail.setText(getEmail(PaystackActivity.this));
        mEditAmountInKobo.setText(price+".00");

        //initialize sdk
        PaystackSdk.initialize(getApplicationContext());

        mEditCardNum.requestFocus();
        mEditCardNum.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() ==16) {
                    mEditCVC.requestFocus();
                }
            }
        });

        mEditCVC.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() ==3) {
                    mEditExpiryMonth.requestFocus();
                }
            }
        });

        mEditExpiryMonth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() ==2) {
                    mEditExpiryYear.requestFocus();
                }

            }
        });

        mEditExpiryYear.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                validateCardForm();

            }
        });
        //set click listener
        mButtonCreateToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //validate form
                validateCardForm();

                //check card validity
                if (card != null && card.isValid()) {
                    dialog = new ProgressDialog(PaystackActivity.this);
                    dialog.setMessage("Requesting token please wait");
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(true);

                    dialog.show();

                    createToken(card);
                }
            }
        });

        //set click listener
        mButtonPerformTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //validate form
                validateTransactionForm();

                //check card validity
                if (card != null && card.isValid()) {
                    dialog = new ProgressDialog(PaystackActivity.this);
                    dialog.setMessage("Performing transaction... please wait");
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(true);

                    dialog.show();

                    chargeCard();
                }
            }
        });
    }

    private void validateTransactionForm() {

        validateCardForm();

        charge = new Charge();
        charge.setCard(card);
        try {
            charge.putCustomField("Message ID:", messageID+"");
            charge.putCustomField("Message Title:", title);
            charge.putCustomField("Preacher:", preacher);
            charge.putCustomField("Message Date:", date);
            charge.putCustomField("Paid Via", "Android SDK");
            charge.putMetadata("Cart ID", Integer.toString(299390));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //validate fields
        String email = mEditEmail.getText().toString().trim();

        if (isEmpty(email)) {
            mEditEmail.setError("Empty email");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEditEmail.setError("Invalid email");
            return;
        }

        charge.setEmail(email);

/*
        String sAmount = mEditAmountInKobo.getText().toString().trim();
        int amount = -1;
        try {
            amount = Integer.parseInt(sAmount);
        } catch (Exception ignored) {
        }

        if (amount < 1) {
            mEditExpiryMonth.setError("Invalid amount");
            return;
        }

        charge.setAmount(amount);
*/
        charge.setAmount(price * 100);

        // Remember to use a unique reference from your server each time.
        // You may decide not to set a reference, we will provide a value
        // in that case
        //  charge.setReference("7073397683");

        // OUR SDK is Split Payments Aware
        // You may also set a subaccount, transaction_charge and bearer
        // Remember that only when a subaccount is provided will the rest be used
        // charge.setSubaccount("ACCT_azbwwp4s9jidin0iq")
        //        .setBearer(Charge.Bearer.subaccount)
        //        .setTransactionCharge(18);

        // OUR SDK is Plans Aware, and MultiCurrency Aware
        // You may also set a currency and plan
        // charge.setPlan("PLN_sh897hueidh")
        //        .setCurrency("USD");

        // You can add additional parameters to the transaction
        // Our documentation will give details on those we accept.
        // charge.addParameter("someBetaParam","Its value");
         charge.addParameter("message_title",title);
         charge.addParameter("preacher",preacher);
         charge.addParameter("message_date",date);
         charge.addParameter("message_id", String.valueOf(messageID));

    }

    /**
     * Method to validate the form, and set errors on the edittexts.
     */
    private void validateCardForm() {
        //validate fields
        String cardNum = mEditCardNum.getText().toString().trim();

        if (isEmpty(cardNum)) {
            mEditCardNum.setError("Empty card number");
            return;
        }

        //build card object with ONLY the number, update the other fields later
        card = new Card.Builder(cardNum, 0, 0, "").build();
        if (!card.validNumber()) {
            mEditCardNum.setError("Invalid card number");
            return;
        }

        //validate cvc
        String cvc = mEditCVC.getText().toString().trim();
        if (isEmpty(cvc)) {
            mEditCVC.setError("Empty cvc");
            return;
        }
        //update the cvc field of the card
        card.setCvc(cvc);

        //check that it's valid
        if (!card.validCVC()) {
            mEditCVC.setError("Invalid cvc");
            return;
        }

        //validate expiry month;
        String sMonth = mEditExpiryMonth.getText().toString().trim();
        int month = -1;
        try {
            month = Integer.parseInt(sMonth);
        } catch (Exception ignored) {
        }

        if (month < 1) {
            mEditExpiryMonth.setError("Invalid month");
            return;
        }

        card.setExpiryMonth(month);

        String sYear = ("20"+mEditExpiryYear.getText().toString()).trim();
        int year = -1;
        try {
            year = Integer.parseInt(sYear);
        } catch (Exception ignored) {
        }

        if (year < 1) {
            mEditExpiryYear.setError("invalid year");
            return;
        }

        card.setExpiryYear(year);

        //validate expiry
        if (!card.validExpiryDate()) {
            mEditExpiryMonth.setError("Invalid expiry");
            mEditExpiryYear.setError("Invalid expiry");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if ((dialog != null) && dialog.isShowing()){
            dialog.dismiss();
        }
        dialog = null;
    }

    private void chargeCard() {

        transaction = null;
        PaystackSdk.chargeCard(this, charge, new Paystack.TransactionCallback() {
            @Override
            public void onSuccess(Transaction transaction) {
                // This is called only after transaction is successful
                if ((dialog != null) && dialog.isShowing()) {
                    dialog.dismiss();
                }

                try {
                    updateDB(PaystackActivity.this, "complete", price, messageID, 0, System.currentTimeMillis());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                PaystackActivity.this.transaction = transaction;
                Toast.makeText(PaystackActivity.this, transaction.reference, Toast.LENGTH_LONG).show();
                updateTextViews();

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

            @Override
            public void beforeValidate(Transaction transaction) {
                // This is called only before requesting OTP
                // Save reference so you may send to server if
                // error occurs with OTP
                // No need to dismiss dialog
                PaystackActivity.this.transaction = transaction;
                Toast.makeText(PaystackActivity.this, transaction.reference, Toast.LENGTH_LONG).show();
                updateTextViews();
            }

            @Override
            public void onError(Throwable error) {
                if ((dialog != null) && dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (PaystackActivity.this.transaction == null) {
                    Toast.makeText(PaystackActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    mTextReference.setText(String.format("Error: %s", error.getMessage()));
                } else {
                    Toast.makeText(PaystackActivity.this, transaction.reference + " concluded with error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    mTextCard.setText(String.format("%s  concluded with error: %s", transaction.reference, error.getMessage()));
                }
                updateTextViews();

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

        });
    }

    private void createToken(Card card) {
        //then create token using PaystackSdk class
        PaystackSdk.createToken(card, new Paystack.TokenCallback() {
            @Override
            public void onCreate(Token token) {

                //here you retrieve the token, and send to your server for charging.
                if ((dialog != null) && dialog.isShowing()) {
                    dialog.dismiss();
                }

                Toast.makeText(PaystackActivity.this, token.token, Toast.LENGTH_LONG).show();
                PaystackActivity.this.token = token;
                updateTextViews();
            }

            @Override
            public void onError(Throwable error) {
                if ((dialog != null) && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast.makeText(PaystackActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                updateTextViews();
            }
        });
    }

    private void updateTextViews() {
        if (token != null) {
            mTextCard.setText(String.format("Card last 4 digits: %s", token.last4));
            mTextToken.setText(String.format("Token: %s", token.token));
        } else if (transaction != null) {
            mTextReference.setText(String.format("Reference: %s", transaction.reference));
        } else {
            mTextCard.setText(R.string.token_not_gotten);
            mTextToken.setText(R.string.token_null_message);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isEmpty(String s) {
        return s == null || s.length() < 1;
    }
}
