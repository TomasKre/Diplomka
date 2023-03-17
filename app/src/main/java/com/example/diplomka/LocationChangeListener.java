package com.example.diplomka;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class LocationChangeListener implements LocationListener {

    public static DataModel dm;
    private ILocationListenActivity activity;
    private SoundMeter micRecording;

    public LocationChangeListener(Context ctx, ILocationListenActivity activity) {
        dm = new DataModel(ctx);
        this.activity = activity;
        micRecording = new SoundMeter(ctx);
        micRecording.start();
    }

    @Override
    public void onLocationChanged(Location loc) {
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();
        Long ts = System.currentTimeMillis();
        double noise = micRecording.getAmplitude();

        String latitude = "Latitude: " + lat;
        Log.v("onLocationChanged", latitude);
        String longitude = "Longitude: " + lon;
        Log.v("onLocationChanged", longitude);
        String timestamp = ts.toString();
        Log.v("onLocationChanged", "Time in millis: " + timestamp);
        Log.v("onLocationChanged", "Noise: " + noise);

        if (activity != null) {
            // vrací přesnost v metrech ve formě standardní odchylky
            // reálná hodnota nachází v okruhu x metrů od naměřeného bodu s pravděpodobností
            // 1σ ~ 68.2 %
            // 2σ ~ 95.4 %
            // 3σ ~ 99.6 %
            // 4σ ~ 99.8 %
            if (loc.hasAccuracy()) {
                int acc = Math.round(2 * loc.getAccuracy());
                activity.locationChanged(ts, lat, lon, noise, acc);
            } else {
                activity.locationChanged(ts, lat, lon, noise,-1);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

}

