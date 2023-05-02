package com.example.diplomka;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


public class HTTP extends AsyncTask<String, Void, String> {
    private URL url;
    private ISendDataActivity activity;

    public HTTP(ISendDataActivity activity, String url) {
        this.activity = activity;
        Log.v("HTTP", "HTTP initialized with url: " + url);
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
            String nodeKey = BuildConfig.NODE_KEY;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("API-Key", nodeKey);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(15000);
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

            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try(BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    Log.v("HTTP response", response.toString());
                } catch (Exception e) {
                    Log.v("HTTP", "Exception reading response");
                    Log.v("HTTP", e.getMessage());
                }
                conn.disconnect();
                return response.toString();
            } else {
                conn.disconnect();
                return Integer.toString(responseCode);
            }
        } catch (ProtocolException e) {
            Log.v("HTTP", "ProtocolException");
            Log.v("HTTP", e.getMessage());
        } catch (IOException e) {
            Log.v("HTTP", "IOException");
            Log.v("HTTP", e.getMessage());
            return "408";
        } catch (Exception e) {
            Log.v("HTTP", "Exception");
            Log.v("HTTP", e.getMessage());
        }
        return "500";
    }

    @Override
    protected void onPostExecute(String result) {
        Log.v("HTTP result", result);
        if (result.equals("200")) {
            activity.finishLoadingPopup();
        } else {
            activity.cancelLoadingPopup();
        }
    }
}