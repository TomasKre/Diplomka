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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
    private List<StreetData> dataStreets;
    private int allPaths = 0;
    private int greenPaths = 0;
    private Context ctx;

    //mezi gps měřeními
    private static final int maxTimeMs = 300000;
    private static final float maxDistanceM = 100;

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
        dataStreets = dm.getStreetData(session);

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
        Long lastDatetimeMillis = (long) 0;
        int lastId = 0;
        int lastPart = 0;
        int i = 0;
        int id_from = 0;
        int part = 1;
        // Případně color ve tvaru int 0xAARRGGBB
        PolylineOptions polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));

        //TODO: nepovolit zadávat data, pokud ještě probíhá sběr
        if (dataStreets.size() > 0) {
            for (DataPoint dataPoint : dataPoints) {
                if (lastPosition != null) {
                    LatLng position = new LatLng(dataPoint.lat, dataPoint.lon);
                    if (dataPoint.dt - lastDatetimeMillis < maxTimeMs) {
                        polylineOptions.add(position);
                        if (dataPoint.part != lastPart) {
                            for (StreetData dataStreet : dataStreets) {
                                if (dataStreet.part == lastPart) {
                                    if (dataStreet.isInput) {
                                        polylineOptions.color(ContextCompat.getColor(ctx, R.color.accepted));
                                        greenPaths++;
                                    }
                                    allPaths++;
                                    Polyline polyline = googleMap.addPolyline(polylineOptions);
                                    polylineOptions = new PolylineOptions().clickable(true)
                                            .color(ContextCompat.getColor(this, R.color.denied));
                                    polylineOptions.add(position);
                                    mMap.addMarker(new MarkerOptions().position(position)
                                            .title(new Date(dataPoint.dt).toString()));
                                }
                            }
                            lastPart = dataPoint.part;
                        }
                    } else {
                        Polyline polyline = googleMap.addPolyline(polylineOptions);
                        polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                        polylineOptions.add(position);
                        mMap.addMarker(new MarkerOptions().position(position).title(new Date(dataPoint.dt).toString()));
                        allPaths++;
                    }
                } else {
                    LatLng position = new LatLng(dataPoint.lat, dataPoint.lon);
                    polylineOptions.add(position);
                    mMap.addMarker(new MarkerOptions().position(position).title(new Date(dataPoint.dt).toString()));
                }
                lastPosition = new LatLng(dataPoint.lat, dataPoint.lon);
                lastDatetimeMillis = dataPoint.dt;
            }
            for (StreetData dataStreet : dataStreets) {
                if (dataStreet.part == lastPart) {
                    if (dataStreet.isInput) {
                        polylineOptions.color(ContextCompat.getColor(ctx, R.color.accepted));
                        greenPaths++;
                    }
                    allPaths++;
                    Polyline polyline = googleMap.addPolyline(polylineOptions);
                    polylineOptions = new PolylineOptions().clickable(true)
                            .color(ContextCompat.getColor(this, R.color.denied));
                    mMap.addMarker(new MarkerOptions().position(lastPosition)
                            .title(new Date(lastDatetimeMillis).toString()));
                }
            }
        } else {
            double meters = 0.0;
            for (DataPoint dataPoint : dataPoints) {
                if(lastPosition != null) {
                    if (id_from == 0) {
                        id_from = dataPoint.id;
                    }
                    LatLng position = new LatLng(dataPoint.lat, dataPoint.lon);
                    if (dataPoint.dt - lastDatetimeMillis < maxTimeMs) {
                        meters += getDistanceInMeters(lastPosition.latitude, dataPoint.lat,
                                lastPosition.longitude, dataPoint.lon);
                        polylineOptions.add(position);
                        dm.updateDataPoints(dataPoint.id, part);
                        if (meters >= maxDistanceM) {
                            meters = 0;
                            Polyline polyline = googleMap.addPolyline(polylineOptions);
                            dm.addStreetData(id_from, dataPoint.id, part++, 0, 0,
                                    0, 0, 0);
                            polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                            polylineOptions.add(position);
                            allPaths++;
                            mMap.addMarker(new MarkerOptions().position(position).title(new Date(dataPoint.dt).toString()));
                        }
                    } else {
                        meters = 0;
                        Polyline polyline = googleMap.addPolyline(polylineOptions);
                        dm.addStreetData(id_from, dataPoint.id, part++, 0, 0,
                                0, 0, 0);
                        polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                        allPaths++;
                        mMap.addMarker(new MarkerOptions().position(position).title(new Date(dataPoint.dt).toString()));
                    }
                } else {
                    LatLng position = new LatLng(dataPoint.lat, dataPoint.lon);
                    polylineOptions.add(position);
                    mMap.addMarker(new MarkerOptions().position(position).title(new Date(dataPoint.dt).toString()));
                }
                lastPosition = new LatLng(dataPoint.lat, dataPoint.lon);
                lastDatetimeMillis = dataPoint.dt;
                lastId = dataPoint.id;
            }
            if (meters > 0) {
                Polyline polyline = googleMap.addPolyline(polylineOptions);
                dm.addStreetData(id_from, lastId, part, 0, 0,
                        0, 0, 0);
                allPaths++;
                mMap.addMarker(new MarkerOptions().position(lastPosition).title(new Date(lastDatetimeMillis).toString()));
            }
        }

        // Custom map marker takto:
        /*mMap.addMarker(new MarkerOptions().position(lastPosition).title(new Date(dataPoint.dt).toString())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker)));*/

        // Místo pouhého spojování bodů lze nakreslit cestu https://abhiandroid.com/programming/googlemaps

        checkSendButton();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 18.0f));
        //lze vylepšit zazoomováním na těžiště bodů, místo na poslední bod
        // případně i zoomem dle max N-S a W-E

        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this);
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        List<LatLng> geoPoints = polyline.getPoints();
        LatLng pointA = geoPoints.get(0);
        LatLng pointB = geoPoints.get(geoPoints.size() - 1);
        int from = 0, to = 0;
        // Asi není nejelegantnější řešení, ale cesty nejspíše nebudou mít počet bodů v řádech tisíců
        // Jsou pouze z jedné session
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
                            dm.updateStreetData(finalFrom, finalTo, spinnerSidewalk.getSelectedItemPosition(),
                                    spinnerSidewalkWidth.getSelectedItemPosition(), spinnerGreen.getSelectedItemPosition(),
                                    spinnerComfort.getSelectedItemPosition());
                            if(polyline.getColor() == ContextCompat.getColor(ctx, R.color.denied)) {
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
        if (allPaths == greenPaths && allPaths > 0) {
            send_button.setClickable(true);
            send_button.setBackgroundColor(ContextCompat.getColor(this, R.color.accepted));
        } else {
            send_button.setClickable(false);
            send_button.setBackgroundColor(ContextCompat.getColor(this, R.color.denied));
        }
    }

    public static double getDistanceInMeters(double lat1, double lat2, double lon1, double lon2) {
        double R = 6371.01; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // convert to meters
    }
}