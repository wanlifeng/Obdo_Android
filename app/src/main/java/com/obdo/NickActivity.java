package com.obdo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.litesuits.http.LiteHttpClient;
import com.litesuits.http.async.HttpAsyncExecutor;
import com.litesuits.http.data.HttpStatus;
import com.litesuits.http.data.NameValuePair;
import com.litesuits.http.exception.HttpException;
import com.litesuits.http.request.Request;
import com.litesuits.http.request.param.HttpMethod;
import com.litesuits.http.response.Response;
import com.litesuits.http.response.handler.HttpResponseHandler;
import com.obdo.data.models.User;
import com.obdo.data.repos.Repo;

/**
 * The user will be able to update his nickname information at this screen
 *
 * @author Marcus Vinícius de Carvalho
 * @since 12/12/2014
 * @version 1.0
 */
public class NickActivity extends ActionBarActivity {
    /**
     * EditText that hold user nickname
     * @since 12/13/2014
     * @see android.widget.EditText
     */
    private EditText editTextNickname;
    /**
     * Button to confirm nickname update
     * @since 12/13/2014
     * @see android.widget.Button
     */
    private Button buttonUpdateNick;
    /**
     * HTTP Request Controller for the NickActivity
     * @since 12/23/2014
     * @see com.obdo.NickActivity
     */
    private HTTPRequestNickController httpRequestController;
    /**
     * User phone number
     * @since 12/23/2014
     */
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nick);
        phoneNumber = getIntent().getStringExtra("EXTRA_PHONE_NUMBER");

        httpRequestController = new HTTPRequestNickController(this);

        onCreateEditTextNickname();
        onCreateButtonUpdateNick();
    }

    /**
     * Initialize EditText and its behaviors.
     * @since 12/12/2014
     * @see android.widget.EditText
     */
    public void onCreateEditTextNickname() {
        editTextNickname = (EditText) findViewById(R.id.editTextNickname);

        editTextNickname.setHint("Type your name");

        editTextNickname.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //updateUserNickname();
                    handled = true;
                }

                return handled;
            }
        });

        editTextNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (after<0)
                    editTextNickname.setText("");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //TODO: refactor with regular expressions
                String newText = s.toString();
                String letters = "abcdefghijklmnopqrstuvwxyz";
                String numbers = "0123456789";
                String possibleValues = letters.toLowerCase() + letters.toUpperCase() + numbers;

                //If typed value is null or empty, do nothing
                if (newText == null || newText.isEmpty()) return;

                //Make sure that we only have valid characters
                for (int i = 0; i < newText.length() && i <= 30 ; i++) {
                    if (!possibleValues.contains(String.valueOf(s.charAt(i)))) {
                        String beforeText = newText.substring(0,i==0?0:i);
                        String afterText = i+1>=newText.length()?"":newText.substring(i+1);
                        newText = beforeText+afterText;
                    }
                }

                //Make sure that we do not have a number as the first character
                while (!newText.isEmpty() && numbers.contains(String.valueOf(newText.charAt(0)))) {
                    newText = newText.length()>1?newText.substring(1):"";
                }

                //Make sure that the first character is always Upper case
                newText = String.valueOf(newText.charAt(0)).toUpperCase() + (newText.length()>1?newText.substring(1):"");

                //Make sure that we always have a limit of 30 characters
                newText=newText.length()>30?newText.substring(0,30):newText;


                if (!newText.equals(editTextNickname.getText().toString())) editTextNickname.setText(newText);
                editTextNickname.setSelection(newText.length());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Initialize Button and its behaviors.
     * @since 12/12/2014
     * @see android.widget.Button
     */
    public void onCreateButtonUpdateNick() {
        buttonUpdateNick = (Button) findViewById(R.id.buttonUpdateNick);

        buttonUpdateNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                httpRequestController.updateUserNickname(phoneNumber,editTextNickname.getText().toString());
            }
        });
    }
}

/**
 * HTTP Request Controller for the NickActivity
 * This class will handle every HTTP Request that the NickActivity needs, as well as UI manipulation
 * @author Marcus Vinícius de Carvalho
 * @since 12/20/2014
 * @version 1.0
 */
class HTTPRequestNickController  {
    /**
     * HTTP Client
     * @since 12/23/2014
     * @see com.litesuits.http.LiteHttpClient
     */
    private LiteHttpClient liteHttpClient;
    /**
     * HTTP Asynchronous Executor
     * @since 12/23/2014
     * @see com.litesuits.http.async.HttpAsyncExecutor
     */
    private HttpAsyncExecutor asyncExecutor;
    /**
     * Activity that calls this class
     * @since 12/23/2014
     */
    private Activity activity;
    /**
     * Server Address saved on the url.xml
     * @since 12/23/2014
     */
    private String serverAddress;

    public HTTPRequestNickController(Activity activity) {
        this.activity = activity;
        liteHttpClient = LiteHttpClient.newApacheHttpClient(activity.getApplicationContext());
        asyncExecutor = HttpAsyncExecutor.newInstance(liteHttpClient);
        serverAddress = activity.getApplicationContext().getString(R.string.server_address);
    }

    /**
     * Update user's name at the server
     * @param phoneNumber User cellphone number
     * @param nick new nickname
     */
    public void updateUserNickname(final String phoneNumber,final String nick) {
        Request request = new Request(serverAddress)
                .setMethod(HttpMethod.Post)
                .addUrlPrifix("http://")
                .addUrlSuffix(activity.getApplicationContext().getString(R.string.url_update_user_nickname_POST))
                .addUrlParam("number", phoneNumber)
                .addUrlParam("name", nick)
                .addHeader("Accept", "application/json");

        asyncExecutor.execute(request, new HttpResponseHandler() {
            @Override
            protected void onSuccess(Response res, HttpStatus status, NameValuePair[] headers) {
                Repo repo = new Repo(activity);
                User user = repo.Users.getByPhoneNumber(phoneNumber);
                user.setName(nick);
                user.save(repo);

                Intent intent = new Intent(activity, ObdoActivity.class);
                activity.startActivity(intent);
            }

            @Override
            protected void onFailure(Response res, HttpException e) {
                Toast.makeText(activity.getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
