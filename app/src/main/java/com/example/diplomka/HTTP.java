package com.example.diplomka;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


public class HTTP extends AsyncTask<String, Void, String> {
    private URL url;
    private MapActivity map;

    public HTTP(MapActivity map) {
        this.map = map;
        Log.v("HTTP", "HTTP initialized");
        try {
            this.url = new URL("http://ulice.nti.tul.cz:5000");
        } catch (MalformedURLException e) {

        }
    }

    public HTTP(MapActivity map, String url) {
        this.map = map;
        Log.v("HTTP", "HTTP initialized with url");
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            Log.v("HTTP", "MalformedURLException");
        }
    }

    @Override
    protected String doInBackground(String... strings) {
        String json = strings[0];
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.connect();

            try(OutputStream os = conn.getOutputStream()) {
                OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                osw.write(json);
                osw.flush();
                osw.close();
                os.flush();
                os.close();
            }

            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return Integer.toString(responseCode);
        } catch (ProtocolException e) {
            Log.v("HTTP", "ProtocolException");
            Log.v("HTTP", e.getMessage());
        } catch (IOException e) {
            Log.v("HTTP", "IOException");
            Log.v("HTTP", e.getMessage());
            return "404";
        } catch (Exception e) {
            Log.v("HTTP", "Exception");
            Log.v("HTTP", e.getMessage());
        }
        return "500";
    }

    @Override
    protected void onPostExecute(String result) {
        Log.v("HTTP result", result);
        if (result == "200") {
            map.finishLoadingPopup();
        } else {
            map.cancelLoadingPopup();
        }
    }
}