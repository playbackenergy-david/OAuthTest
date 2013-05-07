package com.example.OAuthTest;

import android.net.Uri;
import android.util.Log;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by: davidbriggs
 * Date: 4/29/13
 *
 *
 */
public class EnergimolnetAPI {
    //a string for tagging log messages
    public static final String TAG = "com.pe.EnergimolnetTest.API";


    //API endpoints
    private static final String API_URL = "https://app.energimolnet.se/api/1.1/";
    private static final String ME = API_URL + "users/me/me?";
    private static final String METERS = API_URL + "users/me/meters?";

    //This must be added to the end of every request
    private static final String ACCESS_TOKEN = "access_token=%s&opt_pretty=1";

    //Authorization and token endpoints
    private static final String AUTHORIZATION_ENDPOINT = "https://app.energimolnet.se/oauth2/authorize?client_id=%s&response_type=code&redirect_uri=%s";
    private static final String TOKEN_ENDPOINT = "https://app.energimolnet.se/oauth2/grant";

    //the member fields needed for calls to the api
    private String mClientId;
    private String mClientSecret;
    private String mCallback;


    //fields for storing token data
    private String mCode;
    private String mRefreshToken;
    private String mAccessToken;

    //the actual time the token expires, not the lifetime
    private long mExpires;

    public EnergimolnetAPI(String clientID, String clientSecret, String callback){
        mClientId = clientID;
        mClientSecret = clientSecret;
        mCallback = callback;
    }

    public String getAuthorizationUrl() {
        return String.format(AUTHORIZATION_ENDPOINT,
                mClientId,
                Uri.encode(mCallback));
    }

    /**
     *
     * requests an access token and a fresh token from energimolnet
     *
     *
     * The response to the request for an access token should look like this:
     * {    "token_type":"bearer",
            "expires_in":3600,
            "refresh_token":"a3cd1c9a1adda6fe9f42f56fd5ebdfa2e64bc4d7",
            "scope":null,
            "access_token":"fa466ba6b27ffa9630c323aaf7c91a59f7056225"
     * }
     *
     * @param code the autorization code
     * @return whether authorization was successful
     */
    public boolean authorizeAndAcquireTokens(String code) {
        //set the code for later
        mCode = code;
        //get and set the access/refresh tokens
        return getNewAccessToken(false);
    }

    /**
     * Requests a new access token after authorization has already occurred.
     *
     */
    private void refreshToken() throws Exception{
        if(mRefreshToken==null){
            throw new Exception("Cannot request a new access token when refresh token is null");
        }
        getNewAccessToken(true);
    }

    /**
     *
     * @param refreshing if we're requesting a new access token after authorization i.e., refreshing
     * @return if the request for a token was successful
     */
    private boolean getNewAccessToken(boolean refreshing){

        URL url;
        HttpURLConnection urlConnection=null;

        try {
            //get the content to POST
            String content = getBodyofAccessTokenRequest(refreshing);
            Log.d(TAG, "content to POST:" + content );

            //URL and connection establishment
            url = new URL(TOKEN_ENDPOINT);
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setFixedLengthStreamingMode(content.getBytes("UTF-8").length);
            urlConnection.connect();

            //POST the contents
            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            writeStream(out, content.getBytes("UTF-8"));

            //read the input stream for the response
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            //Convert the response to a string and then to JSON.
            JSONObject jsonResponse = new JSONObject(inputStreamToString(in).toString());

            //set the access token
            mAccessToken = jsonResponse.getString("access_token");

            //if we're getting an access token for the first time
            //then we're also getting a refresh token, save it
            if(!refreshing){
                mRefreshToken = jsonResponse.getString("refresh_token");
            }

            //set the expiration time
            mExpires = System.currentTimeMillis() + jsonResponse.getLong("expires_in")*1000;
            return true;

        } catch (Exception e){
            Log.e(TAG, "Exception when requesting tokens", e);
            return false;
        }
        finally {
            if(urlConnection!=null) urlConnection.disconnect();
        }
    }


    /**
     *
     * @return A string response from the METERS request
     */
    public String getMeters(){
        try {
            //wrapped in a JSON array just to show how easy it is to convert
            //to JSON.
            return new JSONArray(energimolnetRequest(METERS)).toString();
        } catch (JSONException e){
            Log.e(TAG, "Couldn't parse JSON in meter request", e);
            return null;
        }

    }

    /**
     * Makes a request to the Energimolnet server
     *
     *
     *
     * @param requestURL the endpoint and any parameters
     * @return the response from the GET request
     */
    private String energimolnetRequest(String requestURL) {
        URL url;
        HttpURLConnection urlConnection=null;

        try {
            url = new URL(requestURL + String.format(ACCESS_TOKEN, mAccessToken));
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            Log.d(TAG, "response code:" + urlConnection.getResponseCode());
            Log.d(TAG, "response msg:" + urlConnection.getResponseMessage());
            //check if the access token had expired
            if((urlConnection.getResponseCode()==401)||(urlConnection.getResponseMessage().contains("invalid_grant"))){
                //if so request the tokens again and re do this request
                //disconnect the old connection
                urlConnection.disconnect();
                //update the tokens
                refreshToken();
                //make a new urlconnection with the same URL as before
                url = new URL(requestURL + String.format(ACCESS_TOKEN, mAccessToken));
                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
            }
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            //wrap the response
            return inputStreamToString(in).toString();

        } catch (Exception e){
            Log.e(TAG, "Exception during request", e);
            return null;
        }
        finally {
            if(urlConnection!=null) urlConnection.disconnect();
        }
    }

    private String getBodyofAccessTokenRequest(boolean refreshing){
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        StringBuilder body=new StringBuilder();
        params.add(new BasicNameValuePair("client_id", mClientId));
        params.add(new BasicNameValuePair("client_secret", mClientSecret));

        if(refreshing){
            params.add(new BasicNameValuePair("refresh_token", mRefreshToken));
            params.add(new BasicNameValuePair("grant_type", "refresh_token"));
        } else {
            params.add(new BasicNameValuePair("redirect_uri", Uri.encode(mCallback)));
            params.add(new BasicNameValuePair("code", mCode));
            params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        }

        int i=0;
        for(BasicNameValuePair pair : params){
            if(i==0){
                body.append(pair.toString());
            } else {
                body.append("&");
                body.append(pair.toString());
            }
            i++;
        }
        return body.toString();

    }

    private static StringBuilder inputStreamToString(InputStream is) throws IOException {
        String line = "";
        StringBuilder total = new StringBuilder();

        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        // Read response until the end
        while ((line = rd.readLine()) != null) {
            total.append(line);
        }

        // Return full string
        return total;
    }

    private void writeStream(OutputStream out, byte[] bytes) {
        try{
            out.write(bytes);
            out.close();
        } catch (Exception e){
            Log.e(TAG, "exception when writing to stream");
        }
    }
}
