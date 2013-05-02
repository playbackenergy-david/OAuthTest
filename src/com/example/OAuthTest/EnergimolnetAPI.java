package com.example.OAuthTest;

import android.net.Uri;
import android.util.Log;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: davidbriggs
 * Date: 4/29/13
 * Time: 4:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class EnergimolnetAPI {

    public static final String API_URL = "https://app.energimolnet.se/api/1.1/";

    public static final String AUTHORIZATION_ENDPOINT = "https://app.energimolnet.se/oauth2/authorize?client_id=%s&response_type=code&redirect_uri=%s";
    public static final String TOKEN_ENDPOINT = "https://app.energimolnet.se/oauth2/grant";

    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private static final String GRANT_TYPE = "grant_type";

    public static String getAccessTokenEndpoint() {
        return TOKEN_ENDPOINT;
    }


    public static String getAuthorizationUrl(String clientId, String callback) {
        return String.format(AUTHORIZATION_ENDPOINT,
                clientId,
                Uri.encode(callback));
    }


    public static String getBodyofAccessToken(String clientId, String clientSecret, String callback, String code){
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        StringBuilder body=new StringBuilder();
        try {
            params.add(new BasicNameValuePair("client_id", clientId));
            params.add(new BasicNameValuePair("client_secret", clientSecret));
            params.add(new BasicNameValuePair("code", code));
            params.add(new BasicNameValuePair("redirect_uri", Uri.encode(callback)));
            params.add(new BasicNameValuePair(GRANT_TYPE, GRANT_TYPE_AUTHORIZATION_CODE));
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

        }  catch (Exception e){
            Log.e(MyActivity.TAG, "error parsing json");
        }
        return body.toString();

    }

}
