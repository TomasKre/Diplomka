package com.example.diplomka;

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
    URL url;

    public HTTP() {
        Log.v("HTTP", "HTTP initialized");
        try {
            this.url = new URL("http://ulice.nti.tul.cz:5000");
        } catch (MalformedURLException e) {

        }
    }

    public HTTP(String url) {
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
            }
            conn.disconnect();
        } catch (ProtocolException e) {
            Log.v("HTTP", "ProtocolException");
            Log.v("HTTP", e.getMessage());
        } catch (IOException e) {
            Log.v("HTTP", "IOException");
            Log.v("HTTP", e.getMessage());
        } catch (Exception e) {
            Log.v("HTTP", "Exception");
            Log.v("HTTP", e.getMessage());
        }
        return "";
    }
}