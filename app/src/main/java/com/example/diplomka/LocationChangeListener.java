package com.example.diplomka;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.util.Log;
import android.widget.Toast;

public class LocationChangeListener implements LocationListener {

    public static DataModel dm;
    private MainActivity main;
    private SoundMeter micRecording;

    public LocationChangeListener(Context ctx, MainActivity ma) {
        dm = new DataModel(ctx);
        main = ma;
        micRecording = new SoundMeter(ctx);
        micRecording.start();
    }

    @Override
    public void onLocationChanged(Location loc) {
        String latitude = "Latitude: " + loc.getLatitude();
        Log.v("onLocationChanged", latitude);
        String longitude = "Longitude: " + loc.getLongitude();
        Log.v("onLocationChanged", longitude);
        Long timestamp = System.currentTimeMillis();
        String ts = timestamp.toString();
        Log.v("onLocationChanged", "Time in millis: " + ts);

        double noise = micRecording.getAmplitude();
        Log.v("onLocationChanged", "Noise: " + noise);
        dm.addData(timestamp, main.session, loc.getLatitude(), loc.getLongitude(), noise);


        main.showData(dm);
    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

}

