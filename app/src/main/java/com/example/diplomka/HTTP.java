package com.example.diplomka;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class HTTP {
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

    public String sendData(String json) {
        Log.v("HTTP sendData", json);
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(json);
            os.flush();
            os.close();

            if (conn.getResponseCode() == 200) {
                return conn.getResponseMessage();
            }
        } catch (ProtocolException e) {
            Log.v("HTTP", "ProtocolException");
        } catch (IOException e) {
            Log.v("HTTP", "IOException");
        }
        return "";
    }
}