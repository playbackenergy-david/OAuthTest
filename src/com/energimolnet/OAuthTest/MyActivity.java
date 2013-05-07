package com.energimolnet.OAuthTest;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


public class MyActivity extends Activity
{
    private static final String TAG = "com.pe.EnergimolnetTest";
    EnergimolnetAPI energimolnetAPI;

    //
    public static final String CLIENT_ID = "demoapp"; //change this to your client ID issued by energimolnet
    private static final String CLIENT_PASSWORD = "demopass";
    public static final String CALLBACK_URI = "oauth://energimolnet";

    private class GetOAuthAccessToken extends AsyncTask<String, Void, Boolean>{

        @Override
        protected Boolean doInBackground(String... code) {
            return energimolnetAPI.authorizeAndAcquireTokens(code[0]);
        }

        @Override
        protected void onPostExecute(Boolean response){
            //if we have an access token then enable the meters button
            if(response){
                findViewById(R.id.btn_meters).setEnabled(true);
            }
        }

    }
    private class GetMeters extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... voids) {
            return energimolnetAPI.getMeters();
        }

        @Override
        protected void onPostExecute(String meters){
            //display the meters response in the textview
            //here we convert from html so we can display öäå etc characters
            ((TextView)findViewById(R.id.txt_response)).setText(meters);
        }

    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        energimolnetAPI = new EnergimolnetAPI(CLIENT_ID, CLIENT_PASSWORD, CALLBACK_URI);


        if((getIntent()!=null) && (getIntent().getData()!=null)){
            //we received a callback from the browser with our code
            Uri uri = getIntent().getData();
            Log.d(TAG, "callback uri:" + uri);
            //get the code from the callback URL
            String code = uri.getQueryParameter("code");
            //try and get tokens
            new GetOAuthAccessToken().execute(code);
        }

        setContentView(R.layout.main);

        findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Generate the URL that is needed to request tokens from Energimolnet
                String authURL = energimolnetAPI.getAuthorizationUrl();
                Log.d(TAG, "Auth URI:"+authURL);

                //Launch the browser and point it towards the authentication URL
                Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(authURL));
                startActivity(i);
            }
        });

        findViewById(R.id.btn_meters).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GetMeters().execute();
            }
        });

    }



}
