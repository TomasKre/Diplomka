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
        dm.addDataPoints(timestamp, main.session, loc.getLatitude(), loc.getLongitude(), noise, 0);

        // vrací přesnost v metrech ve formě standardní odchylky
        // reálná hodnota nachází v okruhu x metrů od naměřeného bodu s pravděpodobností
        // 1σ ~ 68.2 %
        // 2σ ~ 95.4 %
        // 3σ ~ 99.6 %
        // 4σ ~ 99.8 %
        if (loc.hasAccuracy()) {
            int accuracyInM = (int) Math.round(2 * loc.getAccuracy());
            TextView accuracyTextView = main.findViewById(R.id.accuracy_value);
            accuracyTextView.setText(Integer.toString(accuracyInM));
        }
        
        main.showData(dm);
    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

}

