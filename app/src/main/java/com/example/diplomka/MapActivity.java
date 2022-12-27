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
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.diplomka.databinding.ActivityMapBinding;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener {

    private GoogleMap mMap;
    private ActivityMapBinding binding;
    private DataModel dm;
    private String msg;
    private List<DataPoint> dataPoints;
    private List<StreetData> dataStreets;
    private List<Polyline> polylineList;
    private List<DataPoint> markers;
    private int maxPart;
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
        polylineList = new ArrayList<>();
        markers = new ArrayList<>();
        maxPart = 0;

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

        googleMap.setOnMapLongClickListener(latLng -> onMapLongClick(latLng));

        LatLng lastPosition = null;
        Long lastDatetimeMillis = (long) 0;
        int lastId = 0;
        int lastPart = 0;
        int id_from = 0;
        int part = 1;
        DataPoint lastDataPoint = null;
        // Případně color ve tvaru int 0xAARRGGBB
        PolylineOptions polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));

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
                                    polylineList.add(polyline);
                                    polylineOptions = new PolylineOptions().clickable(true)
                                            .color(ContextCompat.getColor(this, R.color.denied));
                                    polylineOptions.add(position);
                                    if (dataPoint.part > maxPart)
                                        maxPart = dataPoint.part;
                                    mMap.addMarker(new MarkerOptions().position(position)
                                            .title(getHumanDate(dataPoint.dt)));
                                    markers.add(dataPoint);
                                }
                            }
                            lastPart = dataPoint.part;
                        }
                    } else {
                        Polyline polyline = googleMap.addPolyline(polylineOptions);
                        polylineList.add(polyline);
                        polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                        polylineOptions.add(position);
                        if (dataPoint.part > maxPart)
                            maxPart = dataPoint.part;
                        mMap.addMarker(new MarkerOptions().position(position).title(getHumanDate(dataPoint.dt)));
                        markers.add(dataPoint);
                        allPaths++;
                    }
                } else {
                    LatLng position = new LatLng(dataPoint.lat, dataPoint.lon);
                    polylineOptions.add(position);
                    if (dataPoint.part > maxPart)
                        maxPart = dataPoint.part;
                    mMap.addMarker(new MarkerOptions().position(position).title(getHumanDate(dataPoint.dt)));
                    markers.add(dataPoint);
                }
                lastPosition = new LatLng(dataPoint.lat, dataPoint.lon);
                lastDatetimeMillis = dataPoint.dt;
                lastDataPoint = dataPoint;
            }
            for (StreetData dataStreet : dataStreets) {
                if (dataStreet.part == lastPart) {
                    if (dataStreet.isInput) {
                        polylineOptions.color(ContextCompat.getColor(ctx, R.color.accepted));
                        greenPaths++;
                    }
                    allPaths++;
                    Polyline polyline = googleMap.addPolyline(polylineOptions);
                    polylineList.add(polyline);
                    polylineOptions = new PolylineOptions().clickable(true)
                            .color(ContextCompat.getColor(this, R.color.denied));
                    mMap.addMarker(new MarkerOptions().position(lastPosition)
                            .title(getHumanDate(lastDatetimeMillis)));
                    markers.add(lastDataPoint);
                }
            }
        } else {
            double meters = 0.0;
            for (DataPoint dataPoint : dataPoints) {
                if(lastPosition != null) {
                    LatLng position = new LatLng(dataPoint.lat, dataPoint.lon);
                    if (dataPoint.dt - lastDatetimeMillis < maxTimeMs) {
                        meters += getDistanceInMeters(lastPosition.latitude, dataPoint.lat,
                                lastPosition.longitude, dataPoint.lon);
                        polylineOptions.add(position);
                        if (dataPoint.part > maxPart)
                            maxPart = dataPoint.part;
                        if (meters >= maxDistanceM) {
                            meters = 0;
                            Polyline polyline = googleMap.addPolyline(polylineOptions);
                            polylineList.add(polyline);
                            dm.addStreetData(id_from, dataPoint.id, part++, 0, 0,
                                    0, 0, 0);
                            id_from = dataPoint.id;
                            polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                            polylineOptions.add(position);
                            if (dataPoint.part > maxPart)
                                maxPart = dataPoint.part;
                            allPaths++;
                            mMap.addMarker(new MarkerOptions().position(position).title(getHumanDate(dataPoint.dt)));
                            markers.add(dataPoint);
                        }
                        dm.updateDataPoints(dataPoint.id, part);
                    } else {
                        meters = 0;
                        Polyline polyline = googleMap.addPolyline(polylineOptions);
                        polylineList.add(polyline);
                        dm.addStreetData(id_from, dataPoint.id, part++, 0, 0,
                                0, 0, 0);
                        id_from = dataPoint.id;
                        polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                        allPaths++;
                        mMap.addMarker(new MarkerOptions().position(position).title(getHumanDate(dataPoint.dt)));
                        markers.add(dataPoint);
                        dm.updateDataPoints(dataPoint.id, part);
                    }
                } else {
                    id_from = dataPoint.id;
                    dm.updateDataPoints(dataPoint.id, part);
                    LatLng position = new LatLng(dataPoint.lat, dataPoint.lon);
                    polylineOptions.add(position);
                    if (dataPoint.part > maxPart)
                        maxPart = dataPoint.part;
                    mMap.addMarker(new MarkerOptions().position(position).title(getHumanDate(dataPoint.dt)));
                    markers.add(dataPoint);
                }
                lastPosition = new LatLng(dataPoint.lat, dataPoint.lon);
                lastDatetimeMillis = dataPoint.dt;
                lastId = dataPoint.id;
                lastDataPoint = dataPoint;
            }
            if (meters > 0) {
                Polyline polyline = googleMap.addPolyline(polylineOptions);
                polylineList.add(polyline);
                dm.addStreetData(id_from, lastId, part, 0, 0,
                        0, 0, 0);
                allPaths++;
                mMap.addMarker(new MarkerOptions().position(lastPosition).title(getHumanDate(lastDatetimeMillis)));
                markers.add(lastDataPoint);
            }
        }

        // Custom map marker takto:
        /*mMap.addMarker(new MarkerOptions().position(lastPosition).title(getHumanDate(dataPoint.dt))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker)));*/

        // Místo pouhého spojování bodů lze nakreslit cestu https://abhiandroid.com/programming/googlemaps

        checkSendButton();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 18.0f));
        //lze vylepšit zazoomováním na těžiště bodů, místo na poslední bod
        // případně i zoomem dle max N-S a W-E

        // Set listeners for click events.
        googleMap.setOnPolylineClickListener(this);
    }

    //@Override
    public void onMapLongClick(LatLng clickedLatLng) {
        // Iterate through the list of polylines and find the one that is closest to the long press location
        double minDistanceLines = Double.MAX_VALUE;
        double minDistancePoints;
        Polyline closestPolyline = null;
        LatLng nearestPoint = null;
        LatLng nearestPointLine = null;
        for (Polyline polyline : polylineList) {
            minDistancePoints = Double.MAX_VALUE;
            for (LatLng point : polyline.getPoints()) {
                double distancePoints = getDistanceInMeters(clickedLatLng, point);
                if (distancePoints < minDistancePoints) {
                    nearestPoint = point;
                    minDistancePoints = distancePoints;
                }
            }
            double distanceLines = getDistanceInMeters(clickedLatLng, nearestPoint);
            if (distanceLines < minDistanceLines) {
                minDistanceLines = distanceLines;
                closestPolyline = polyline;
                nearestPointLine = nearestPoint;
            }
        }
        if (closestPolyline != null) {
            boolean isMarkered = false;
            //Možný update přidat k datapointům info jestli je v nich marker? Místo 2 forů
            for (DataPoint marker : markers) {
                if (nearestPointLine.latitude == marker.lat && nearestPointLine.longitude == marker.lon) {
                    isMarkered = true;
                    break;
                }
            }
            if (!isMarkered) {
                for (DataPoint dataPoint : dataPoints) {
                    if (nearestPointLine.latitude == dataPoint.lat && nearestPointLine.longitude == dataPoint.lon) {
                        mMap.addMarker(new MarkerOptions().position(nearestPointLine).title(getHumanDate(dataPoint.dt)));
                        ++maxPart;
                        dm.updateSplitStreetData(dataPoint.id, dataPoint.part, maxPart);
                        dm.updateSplitDataPoints(dataPoint.session, dataPoint.dt, dataPoint.part, maxPart);
                        break;
                        //TODO: rozdělit polyline jednoduché řešení je vše překreslit
                    }
                }
            } else {
                Toast.makeText(this, "Zaznamenán long click na marker", Toast.LENGTH_LONG).show();
            }
        }
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

        Spinner spinnerSidewalk = (Spinner) popupView.findViewById(R.id.sidewalk_spinner);
        ArrayAdapter<CharSequence> adapterSidewalk = ArrayAdapter.createFromResource(this,
                R.array.sidewalk_array, android.R.layout.simple_spinner_item);
        adapterSidewalk.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSidewalk.setAdapter(adapterSidewalk);

        Spinner spinnerSidewalkWidth = (Spinner) popupView.findViewById(R.id.sidewalk_width_spinner);
        ArrayAdapter<CharSequence> adapterSidewalkWidth = ArrayAdapter.createFromResource(this,
                R.array.sidewalk_width_array, android.R.layout.simple_spinner_item);
        adapterSidewalkWidth.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSidewalkWidth.setAdapter(adapterSidewalkWidth);

        Spinner spinnerGreen = (Spinner) popupView.findViewById(R.id.green_spinner);
        ArrayAdapter<CharSequence> adapterGreen = ArrayAdapter.createFromResource(this,
                R.array.green_array, android.R.layout.simple_spinner_item);
        adapterGreen.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGreen.setAdapter(adapterGreen);

        Spinner spinnerComfort = (Spinner) popupView.findViewById(R.id.safespace_spinner);
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
        Button buttonCancel = (Button) popupView.findViewById(R.id.cancel_button);
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

        buttonCancel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_UP:
                        popupWindow.dismiss();
                        break;
                }
                return false;
            }
        });

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }

    private void checkSendButton() {
        Log.v("Map paths", "Green paths: " + greenPaths + "/" + allPaths);
        Button send_button = findViewById(R.id.send_button);
        if (allPaths == greenPaths && allPaths > 0) {
            send_button.setClickable(true);
            send_button.setBackground(ContextCompat.getDrawable(this, R.drawable.button_save_border));
        } else {
            send_button.setClickable(false);
            send_button.setBackground(ContextCompat.getDrawable(this, R.drawable.button_deny_border));
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

    public static double getDistanceInMeters(LatLng point1, LatLng point2) {
        double R = 6371.01; // Radius of the earth

        double latDistance = Math.toRadians(point2.latitude - point1.latitude);
        double lonDistance = Math.toRadians(point2.longitude - point1.longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // convert to meters
    }

    public static String getHumanDate(long timeInMillis) {
        String pattern = "dd.MM.yyyy hh:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(timeInMillis);
    }
}