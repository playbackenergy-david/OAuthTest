package com.example.OAuthTest;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import org.apache.http.client.HttpClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;


public class MyActivity extends Activity
{
    public static final String TAG = "com.pe.EnergimolnetTest";
    private static final String CALLBACK_URI = "oauth://energimolnet";
    private static final String PROTECTED_RESOURCE_URL = "https://app.energimolnet.se/api/1.1/users/me";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String authURI = EnergimolnetAPI.getAuthorizationUrl("demoapp", CALLBACK_URI);

        Log.d(TAG, "Auth URI:"+authURI);

        if((getIntent()!=null) && (getIntent().getData()!=null)){
            Uri uri = getIntent().getData();

            Log.d(TAG, "callback uri:" + uri);
            String accessToken = uri.getQueryParameter("code");

            Log.d(TAG, "access token:" + accessToken);
            String content = EnergimolnetAPI.getBodyofAccessToken("demoapp", "demopass", CALLBACK_URI, accessToken).toString();
            Log.d(TAG, "content:" + content );

            URL url;
            HttpURLConnection urlConnection=null;
            try {
                url = new URL(EnergimolnetAPI.getAccessTokenEndpoint());
                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setFixedLengthStreamingMode(content.getBytes("UTF-8").length);
                Log.d(TAG, "content length:" + content.getBytes("UTF-8").length);

                urlConnection.connect();
                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                writeStream(out, content.getBytes("UTF-8"));

                Log.d(TAG, "response code:" + urlConnection.getResponseCode());
                Log.d(TAG, "response msg:" + urlConnection.getResponseMessage());

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                readStream(in);
            } catch (Exception e){
                Log.e(TAG, "Exception when posting contents", e);
            }
            finally {
                urlConnection.disconnect();
            }

        } else {
            Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(authURI));
            startActivity(i);
        }

        setContentView(R.layout.main);

    }

    private void readStream(InputStream in) throws IOException{
        Log.d(TAG, "response:"+inputStreamToString(in).toString());
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
