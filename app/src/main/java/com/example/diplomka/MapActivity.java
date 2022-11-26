package com.example.diplomka;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.diplomka.databinding.ActivityMapBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Date;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener {

    private GoogleMap mMap;
    private ActivityMapBinding binding;
    private DataModel dm;
    private String msg;
    private List<DataPoint> dataPoints;
    private List<StreetData> streetData;
    private int allPaths = 0;
    private int greenPaths = 0;
    private Context ctx;

    private static final int maxTimeMs = 30000; //mezi gps měřeními

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ctx = getApplicationContext();
        dm = new DataModel(ctx);

        //getExtra
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                msg = null;
            } else {
                msg = extras.getString("item");
            }
        } else {
            msg = (String) savedInstanceState.getSerializable("item");
        }

        int session = Integer.parseInt(msg.split("\\)")[0]);
        dataPoints = dm.getDataPoints(session);
        streetData = dm.getStreetData(session);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng lastPosition = null;
        int lastId = 0;
        Long lastDatetimeMillis = (long) 0;
        // Color ve tvaru int 0xAARRGGBB
        PolylineOptions polylineOptions = new PolylineOptions().clickable(true).color(0xffff0000);
        for (DataPoint dataPoint : dataPoints) {
            if(lastPosition != null) {
                // Max 5 minut mezi záznamy (300000 ms)
                if (dataPoint.dt - lastDatetimeMillis < maxTimeMs) {
                    polylineOptions.add(lastPosition);
                    polylineOptions.add(new LatLng(dataPoint.lat, dataPoint.lon));
                    for (StreetData street : streetData) {
                        if((lastId == street.from && dataPoint.id == street.to) ||
                                (lastId == street.to && dataPoint.id == street.from)) {
                            polylineOptions.color(ContextCompat.getColor(this, R.color.accepted));
                            greenPaths++;
                            break;
                        }
                    }
                    allPaths++;
                    Polyline polyline = googleMap.addPolyline(polylineOptions);
                }
            }
            polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
            lastPosition = new LatLng(dataPoint.lat, dataPoint.lon);
            lastDatetimeMillis = dataPoint.dt;
            lastId = dataPoint.id;

            mMap.addMarker(new MarkerOptions().position(lastPosition).title(new Date(dataPoint.dt).toString())
                    /*.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker))*/);
            // Odkomentovat pro custom map marker
        }
        // Místo pouhého spojování bodů lze nakreslit cestu https://abhiandroid.com/programming/googlemaps

        checkSendButton();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(lastPosition));
        mMap.animateCamera(CameraUpdateFactory.zoomTo( 15.0f ));
        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this);
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        List<LatLng> geoPoints = polyline.getPoints();
        LatLng pointA = geoPoints.get(0);
        LatLng pointB = geoPoints.get(1);
        int from = 0, to = 0;
        // Asi není nejelegantnější řešení, ale cesty nejspíše nebudou mít počet bodů v řádech tisíců
        for (DataPoint dataPoint : dataPoints) {
            if(dataPoint.lat == pointA.latitude && dataPoint.lon == pointA.longitude) {
                from = dataPoint.id;
            }
            if(dataPoint.lat == pointB.latitude && dataPoint.lon == pointB.longitude) {
                to = dataPoint.id;
            }
        }

        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_input_data, null);

        Spinner spinnerSidewalk = (Spinner) popupView.findViewById(R.id.traffic_spinner);
        ArrayAdapter<CharSequence> adapterSidewalk = ArrayAdapter.createFromResource(this,
                R.array.sidewalk_array, android.R.layout.simple_spinner_item);
        adapterSidewalk.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSidewalk.setAdapter(adapterSidewalk);

        Spinner spinnerSidewalkWidth = (Spinner) popupView.findViewById(R.id.parking_spinner);
        ArrayAdapter<CharSequence> adapterSidewalkWidth = ArrayAdapter.createFromResource(this,
                R.array.sidewalk_width_array, android.R.layout.simple_spinner_item);
        adapterSidewalkWidth.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSidewalkWidth.setAdapter(adapterSidewalkWidth);

        Spinner spinnerGreen = (Spinner) popupView.findViewById(R.id.green_spinner);
        ArrayAdapter<CharSequence> adapterGreen = ArrayAdapter.createFromResource(this,
                R.array.green_array, android.R.layout.simple_spinner_item);
        adapterGreen.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGreen.setAdapter(adapterGreen);

        Spinner spinnerComfort = (Spinner) popupView.findViewById(R.id.sidewalk_spinner);
        ArrayAdapter<CharSequence> adapterComfort = ArrayAdapter.createFromResource(this,
                R.array.comfort_array, android.R.layout.simple_spinner_item);
        adapterComfort.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerComfort.setAdapter(adapterComfort);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        // focusable true by default
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height);

        Button buttonSave = (Button) popupView.findViewById(R.id.save_button);
        if (from != 0 && to != 0) {
            int finalFrom = from;
            int finalTo = to;
            buttonSave.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_DOWN:
                            break;
                        case MotionEvent.ACTION_UP:
                            popupWindow.dismiss();
                            if(polyline.getColor() == ContextCompat.getColor(ctx, R.color.accepted)) {
                                dm.updateStreetData(finalFrom, finalTo, spinnerSidewalk.getSelectedItemPosition(),
                                        spinnerSidewalkWidth.getSelectedItemPosition(), spinnerGreen.getSelectedItemPosition(),
                                        spinnerComfort.getSelectedItemPosition());
                            } else {
                                dm.addStreetData(finalFrom, finalTo, spinnerSidewalk.getSelectedItemPosition(),
                                        spinnerSidewalkWidth.getSelectedItemPosition(), spinnerGreen.getSelectedItemPosition(),
                                        spinnerComfort.getSelectedItemPosition());
                                greenPaths++;
                                checkSendButton();
                                polyline.setColor(ContextCompat.getColor(ctx, R.color.accepted));
                            }
                            break;
                    }
                    return false;
                }
            });
        }

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }

    private void checkSendButton() {
        Log.v("Map paths", "Green paths: " + greenPaths + "/" + allPaths);
        Button send_button = findViewById(R.id.send_button);
        if (allPaths == greenPaths) {
            send_button.setClickable(true);
            send_button.setBackgroundColor(ContextCompat.getColor(this, R.color.accepted));
        } else {
            send_button.setClickable(false);
            send_button.setBackgroundColor(ContextCompat.getColor(this, R.color.denied));
        }
    }
}