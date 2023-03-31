package com.example.diplomka;

import static com.example.diplomka.Tools.getDistanceInMeters;
import static com.example.diplomka.Tools.getHumanDate;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.diplomka.databinding.ActivityRealtimeMapBinding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class RealtimeMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener, ISendDataActivity, ILocationListenActivity {

    private GoogleMap mMap;
    private ActivityRealtimeMapBinding binding;
    private DataModel dm;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private int session;
    private List<DataPoint> dataPoints;
    private List<StreetData> dataStreets;
    private List<Polyline> polylineList;
    private List<DataPoint> markers;
    private PolylineOptions polylineOptions;
    private int part = 1;
    private DataPoint lastDataPoint;
    private boolean firstPoint = true;
    private double distanceInPart = 0;
    private Context ctx;
    private View popupAsyncView;
    private PopupWindow popupAsyncWindow;

    //mezi gps měřeními
    private static final float maxDistanceM = 100;
    private long minTimeMs = 1000;
    private float minDistanceM = 5;
    private int sidewalk = 0;
    private int sidewalk_width = 0;
    private int green = 0;
    private int comfort = 0;
    private boolean streetDataChanged = false;
    private int from;
    private int to;
    private Polyline lastPolyline;
    private boolean newPart = false;
    private int id;
    private int maxPart = 0;
    private final double roundingGPS = 1000000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRealtimeMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ctx = getApplicationContext();
        dm = new DataModel(ctx);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        //getExtra
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                session = 0;
            } else {
                session = extras.getInt("session");
            }
        } else {
            session = (Integer) savedInstanceState.getSerializable("session");
        }

        dataPoints = new ArrayList<>();
        polylineList = new ArrayList<>();
        markers = new ArrayList<>();

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
        if (locationListener == null) {
            locationListener = new LocationChangeListener(ctx, this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                            && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, minDistanceM, locationListener);
                    }
                }
            }
        }

        Button send_button = findViewById(R.id.send_button);
        send_button.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Odeslat data");
            builder.setMessage("Opravdu chcete odeslat záznamy s id " + session + " včetně vyplněných dat?");

            builder.setPositiveButton("Ano", (dialog, which) -> {
                sendStreetDataAndDataPointsToServer(session);
                dialog.dismiss();
                createLoadingPopup();
            });
            builder.setNegativeButton("Ne", (dialog, which) -> {
                // Do nothing
                dialog.dismiss();
            });
            AlertDialog alert = builder.create();
            alert.show();
        });

        // Naplnit autofill stringy z file
        fillAutofillArrays();
    }

    @Override
    protected void onPause() {
        Log.v("Activity lifecycle RealtimeMap", "onPause");
        // Vrácení zhasínání obrazovky na system default
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Odregistrace updatů lokace
        if (locationManager != null)
            locationManager.removeUpdates(locationListener);
        // Kontrola a odstranění osamocených data pointů a cest
        dm.deleteSoloDataPoints();
        dm.deleteSoloStreetData();
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.v("Activity lifecycle RealtimeMap", "onResume");
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
        if (locationListener == null) {
            locationListener = new LocationChangeListener(ctx, this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                            && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, minDistanceM, locationListener);
                    }
                }
            }
        }
        super.onResume();
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
        // Pro IDE, aby neřvalo, že není permission, když vez permissionu se ani nedá tato třída vytvořit v mainu
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        Log.d("Map Realtime", getResources().getString(R.string.night_mode));
        if (getResources().getString(R.string.night_mode).equals("night")) {
            try {
                // Customise the styling of the base map using a JSON object defined in a raw resource file.
                boolean success = googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                this, R.raw.map_style_night));
                if (!success) {
                    Log.e("Map Realtime", "Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e("Map Realtime", "Can't find style. Error: ", e);
            }
        }

        // Set listeners for click events.
        googleMap.setOnMapLongClickListener(latLng -> onMapLongClick(latLng));
        googleMap.setOnPolylineClickListener(polyline -> onPolylineClick(polyline));
    }

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
        if (minDistanceLines > 100.0F) {
            Toast.makeText(this, "Zaznamenán long click dále než 100 m od nejbližšího bodu.", Toast.LENGTH_LONG).show();
            return;
        }
        if (closestPolyline != null) {
            boolean isMarkered = false;
            for (DataPoint marker : markers) {
                if (nearestPointLine.latitude == marker.lat && nearestPointLine.longitude == marker.lon) {
                    isMarkered = true;
                    break;
                }
            }
            // Pokud nejbližší bod longclicku nemá marker projdi body dané části a najdi, který je to bod
            if (!isMarkered) {
                for (DataPoint dataPoint : dataPoints) {
                    if (nearestPointLine.latitude == dataPoint.lat && nearestPointLine.longitude == dataPoint.lon) {
                        // Přidej marker
                        mMap.addMarker(new MarkerOptions().position(nearestPointLine).title(getHumanDate(dataPoint.dt)));
                        markers.add(dataPoint);
                        maxPart = part + 1;

                        // Načti body dané session a čášti, uprav na nové části
                        List<DataPoint> oldPoints = dm.getDataPoints(session, dataPoint.part);
                        dm.updateSplitStreetData(dataPoint.id, dataPoint.part, maxPart);
                        dm.updateSplitDataPoints(dataPoint.session, dataPoint.dt, dataPoint.part, maxPart);


                        // Projdi všechny body dané části a utvoř nové 2 polyline, starou odstraň
                        PolylineOptions polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.accepted));
                        Polyline polyline;
                        List<LatLng> polylinePoints = closestPolyline.getPoints();
                        for (DataPoint oldPoint : oldPoints) {
                            LatLng latLng = new LatLng(Math.round(oldPoint.lat * roundingGPS) / roundingGPS, Math.round(oldPoint.lon * roundingGPS) / roundingGPS);
                            if (polylinePoints.size() > 1)
                                polylinePoints.remove(latLng);
                            polylineOptions.add(latLng);
                            if (oldPoint.lat == nearestPointLine.latitude && oldPoint.lon == nearestPointLine.longitude) {
                                polyline = mMap.addPolyline(polylineOptions);
                                polylineList.add(polyline);
                                polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.accepted));
                                polylineOptions.add(latLng);
                            }
                        }
                        // Postupné odstraňování bodů staré polyline a poslední bod, který zůstal v listu by měl být ten poslední
                        polylineOptions.add(polylinePoints.get(0));
                        polyline = mMap.addPolyline(polylineOptions);
                        polylineList.add(polyline);
                        polylineList.remove(closestPolyline);
                        closestPolyline.remove();
                        break;
                    }
                }
            } else {
                Toast.makeText(this, "Zaznamenán long click na marker", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
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
        int finalFrom = from;
        int finalTo = to;
        buttonSave.setOnClickListener(v -> {
            dm.updateStreetData(finalFrom, finalTo, spinnerSidewalk.getSelectedItemPosition(),
                    spinnerSidewalkWidth.getSelectedItemPosition(), spinnerGreen.getSelectedItemPosition(),
                    spinnerComfort.getSelectedItemPosition());
            popupWindow.dismiss();
        });

        Button buttonCancel = (Button) popupView.findViewById(R.id.cancel_button);
        buttonCancel.setOnClickListener(v -> popupWindow.dismiss());

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }

    private void fillAutofillArrays() {
        View autofillView = findViewById(R.id.auto_input_include);

        Spinner spinnerSidewalk = (Spinner) autofillView.findViewById(R.id.sidewalk_spinner);
        ArrayAdapter<CharSequence> adapterSidewalk = ArrayAdapter.createFromResource(this,
                R.array.sidewalk_array, android.R.layout.simple_spinner_item);
        adapterSidewalk.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSidewalk.setAdapter(adapterSidewalk);
        spinnerSidewalk.setSelection(sidewalk);
        spinnerSidewalk.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                streetDataChanged = true;
                comfort = spinnerSidewalk.getSelectedItemPosition();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        Spinner spinnerSidewalkWidth = (Spinner) autofillView.findViewById(R.id.sidewalk_width_spinner);
        ArrayAdapter<CharSequence> adapterSidewalkWidth = ArrayAdapter.createFromResource(this,
                R.array.sidewalk_width_array, android.R.layout.simple_spinner_item);
        adapterSidewalkWidth.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSidewalkWidth.setAdapter(adapterSidewalkWidth);
        spinnerSidewalkWidth.setSelection(sidewalk_width);
        spinnerSidewalkWidth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                streetDataChanged = true;
                comfort = spinnerSidewalkWidth.getSelectedItemPosition();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        Spinner spinnerGreen = (Spinner) autofillView.findViewById(R.id.green_spinner);
        ArrayAdapter<CharSequence> adapterGreen = ArrayAdapter.createFromResource(this,
                R.array.green_array, android.R.layout.simple_spinner_item);
        adapterGreen.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGreen.setAdapter(adapterGreen);
        spinnerGreen.setSelection(green);
        spinnerGreen.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                streetDataChanged = true;
                comfort = spinnerGreen.getSelectedItemPosition();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        Spinner spinnerComfort = (Spinner) autofillView.findViewById(R.id.safespace_spinner);
        ArrayAdapter<CharSequence> adapterComfort = ArrayAdapter.createFromResource(this,
                R.array.comfort_array, android.R.layout.simple_spinner_item);
        adapterComfort.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerComfort.setAdapter(adapterComfort);
        spinnerComfort.setSelection(comfort);
        spinnerComfort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                streetDataChanged = true;
                comfort = spinnerComfort.getSelectedItemPosition();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }



    private void sendStreetDataAndDataPointsToServer(int session) {
        ArrayList<DataPoint> points = dm.getDataPoints(session);
        ArrayList<StreetData> streets = dm.getStreetData(session);
        ArrayList<FullData> out = new ArrayList<>();

        StreetData lastStreet = null;
        for (DataPoint point:
             points) {
            int id = point.id;
            boolean last = false;
            for (StreetData street:
                 streets) {
                if (id >= street.from && id <= street.to) {
                    last = true;
                    lastStreet = street;
                    if (id < street.to) {
                        out.add(new FullData(point.dt, point.lat, point.lon, point.noise, street.sidewalk,
                                street.sidewalk_width, street.green, street.comfort));
                        last = false;
                        break;
                    }
                }
            }
            if (last) {
                out.add(new FullData(point.dt, point.lat, point.lon, point.noise, lastStreet.sidewalk,
                        lastStreet.sidewalk_width, lastStreet.green, lastStreet.comfort));
            }
        }

        FullDataNamedArray finalJson = new FullDataNamedArray(out);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        String arrayToJson = "";
        try {
            arrayToJson = objectMapper.writeValueAsString(finalJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HTTP http = new HTTP(this,"http://ulice.nti.tul.cz:5000/upload/fulldata");
        AsyncTask<String, Void, String> result = http.execute(arrayToJson);
        Log.v("HTTP Async", result.getStatus().toString());
    }

    public void createLoadingPopup() {
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        popupAsyncView = inflater.inflate(R.layout.popup_async_task, null);
        ImageView imageView = popupAsyncView.findViewById(R.id.image_progress); //Initialize ImageView via FindViewById or programatically

        RotateAnimation anim = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        // Setup anim with desired properties
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE); // repeat animation indefinitely
        anim.setDuration(1250); // animation cycle length in milliseconds

        // Start animation
        imageView.startAnimation(anim);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        // focusable true by default
        popupAsyncWindow = new PopupWindow(popupAsyncView, width, height);
        popupAsyncWindow.showAtLocation(popupAsyncView, Gravity.CENTER, 0, 0);

        Button send_button = findViewById(R.id.send_button);
        send_button.setClickable(false);
    }

    public void finishLoadingPopup() {
        ImageView imageView = popupAsyncView.findViewById(R.id.image_progress);
        // Stop animation and change source image
        imageView.setAnimation(null);
        imageView.setImageResource(R.drawable.check_mark);

        // Change info text
        TextView textView = popupAsyncView.findViewById(R.id.info_text);
        textView.setText("Odesláno, díky!");

        dm.deleteDataPointsBySession(session);
        dm.deleteStreetDataBySession(session);

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            Button send_button = findViewById(R.id.send_button);
            send_button.setClickable(true);
            popupAsyncWindow.dismiss();
            finish();
            }, 5000);
    }

    public void cancelLoadingPopup() {
        ImageView imageView = popupAsyncView.findViewById(R.id.image_progress);
        // Stop animation and change source image
        imageView.setAnimation(null);
        imageView.setImageResource(R.drawable.cross);

        // Change info text
        TextView textView = popupAsyncView.findViewById(R.id.info_text);
        textView.setText("Odeslání se nezdařilo.");

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            Button send_button = findViewById(R.id.send_button);
            send_button.setClickable(true);
            popupAsyncWindow.dismiss();
            }, 5000);
    }

    @Override
    public void locationChanged(long timestamp, double latitude, double longitude, double noise, int accuracyInMeters) {
        DataPoint currentDataPoint = new DataPoint(id, session, timestamp, latitude, longitude, (float) noise, part);
        LatLng position = new LatLng(latitude, longitude);
        if (polylineOptions == null) {
            polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.accepted));
        } else {
            lastPolyline.remove();
        }
        polylineOptions.add(position);
        lastPolyline = mMap.addPolyline(polylineOptions);

        if (lastDataPoint != null)
            distanceInPart += getDistanceInMeters(lastDataPoint.lat, latitude, lastDataPoint.lon, longitude);

        if (distanceInPart >= maxDistanceM || streetDataChanged) {
            mMap.addMarker(new MarkerOptions().position(position)
                    .title(getHumanDate(timestamp)));
            markers.add(currentDataPoint);

            polylineList.add(lastPolyline);
            polylineOptions = new PolylineOptions().clickable(true)
                    .color(ContextCompat.getColor(this, R.color.accepted));
            polylineOptions.add(position);
            lastPolyline = mMap.addPolyline(polylineOptions);

            distanceInPart = 0;
            if (maxPart > part) {
                part = maxPart + 1;
            } else {
                part++;
            }
            newPart = true;

            streetDataChanged = false;
        }

        dm.addDataPoints(timestamp, session, latitude, longitude, noise, part);

        if (firstPoint) {
            firstPoint = false;
            id = dm.getDataPointsMaxId(session);
            mMap.addMarker(new MarkerOptions().position(position)
                    .title(getHumanDate(timestamp)));
            // pro první bod přepíše ID po zjištění z DB
            currentDataPoint.setId(id);
            markers.add(currentDataPoint);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 17.0f));

            from = id;
            to = from;
            id++;
            dm.addStreetData(session, from, to, part, sidewalk, sidewalk_width, green, comfort, 1);
        } else {
            id++;
            to++;
            dm.updateStreetDataTo(from, to);
            if (newPart) {
                newPart = false;
                from = to;
                dm.addStreetData(session, from, to, part, sidewalk, sidewalk_width, green, comfort, 1);
            }
        }
        lastDataPoint = currentDataPoint;
        dataPoints.add(lastDataPoint);
    }

    @Override
    protected void onDestroy() {
        locationManager.removeUpdates(locationListener);
        super.onDestroy();
    }
}