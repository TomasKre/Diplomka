package com.example.diplomka;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.diplomka.databinding.ActivityMapBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener {

    private GoogleMap mMap;
    private ActivityMapBinding binding;
    private DataModel dm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Context ctx = getApplicationContext();
        dm = new DataModel(ctx);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        ArrayList<DataPoint> data = dm.getData();

        LatLng lastPosition = null;
        Long lastDatetimeMillis = (long)0;
        PolylineOptions polylineOptions = new PolylineOptions().clickable(true);
        for (DataPoint dataPoint : data) {
            lastPosition = new LatLng(dataPoint.lat, dataPoint.lon);
            // max 3 minuty mezi záznamy (300000 ms)
            if (dataPoint.dt - lastDatetimeMillis < 300000) {
                polylineOptions.add(lastPosition);
            } else {
                Polyline polyline = googleMap.addPolyline(polylineOptions);
                polylineOptions = new PolylineOptions().clickable(true);
            }
            lastDatetimeMillis = dataPoint.dt;
            mMap.addMarker(new MarkerOptions().position(lastPosition).title(new Date(dataPoint.dt).toString()));
        }
        // Místo mouhého spojování bodů lze nakreslit cestu https://abhiandroid.com/programming/googlemaps

        mMap.moveCamera(CameraUpdateFactory.newLatLng(lastPosition));
        mMap.animateCamera(CameraUpdateFactory.zoomTo( 15.0f ));
        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this);
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        Toast.makeText(this, "Kliknuto na čáru", Toast.LENGTH_LONG).show();

    }
}