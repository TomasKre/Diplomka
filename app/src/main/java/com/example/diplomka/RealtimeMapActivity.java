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
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.diplomka.databinding.ActivityMapBinding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
    private ActivityMapBinding binding;
    private DataModel dm;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private int session;
    private List<DataPoint> dataPoints;
    private List<StreetData> dataStreets;
    private List<Polyline> polylineList;
    private List<DataPoint> markers;
    private int part;
    private int maxPart;
    private int allPaths = 0;
    private int greenPaths = 0;
    private Context ctx;
    private View popupAsyncView;
    private PopupWindow popupAsyncWindow;

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
        dataStreets = new ArrayList<>();
        polylineList = new ArrayList<>();
        markers = new ArrayList<>();
        maxPart = 0;

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
        if (locationListener == null) {
            locationListener = new LocationChangeListener(this, this);
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
        if (minDistanceLines > 100.0F) {
            Toast.makeText(this, "Zaznamenán long click dále než 100 m od nejbližšího bodu.", Toast.LENGTH_LONG).show();
            return;
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
            // Pokud nejbližší bod longclicku nemá marker projdi body dané části a najdi, který je to bod
            if (!isMarkered) {
                for (DataPoint dataPoint : dataPoints) {
                    if (nearestPointLine.latitude == dataPoint.lat && nearestPointLine.longitude == dataPoint.lon) {
                        // Přidej marker
                        mMap.addMarker(new MarkerOptions().position(nearestPointLine).title(getHumanDate(dataPoint.dt)));
                        markers.add(dataPoint);
                        ++maxPart;

                        // Načti body dané session a čášti, uprav na nové části
                        List<DataPoint> oldPoints = dm.getDataPoints(session, dataPoint.part);
                        dm.updateSplitStreetData(dataPoint.id, dataPoint.part, maxPart);
                        dm.updateSplitDataPoints(dataPoint.session, dataPoint.dt, dataPoint.part, maxPart);

                        // Zjištění, zda byla čára user inputovaná
                        boolean isInput = false;
                        PolylineOptions polylineOptions;
                        if (closestPolyline.getColor() != ContextCompat.getColor(this, R.color.denied)) {
                            isInput = true;
                            polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.accepted));
                            greenPaths++;
                        } else {
                            polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                        }
                        allPaths++;

                        // Algoritmus lze zjednodušit oproti onMapReady, jelikož podmínky času a vzdálenosti jsou již splněny
                        // Projdi všechny body dané části a utvoř nové 2 polyline, starou odstraň
                        Polyline polyline;
                        List<LatLng> polylinePoints = closestPolyline.getPoints();
                        for (DataPoint oldPoint : oldPoints) {
                            LatLng latLng = new LatLng(oldPoint.lat, oldPoint.lon);
                            if (polylinePoints.size() > 1)
                                polylinePoints.remove(latLng);
                            polylineOptions.add(latLng);
                            if (oldPoint.lat == nearestPointLine.latitude && oldPoint.lon == nearestPointLine.longitude) {
                                polyline = mMap.addPolyline(polylineOptions);
                                polylineList.add(polyline);
                                if (isInput) {
                                    polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.accepted));
                                } else {
                                    polylineOptions = new PolylineOptions().clickable(true).color(ContextCompat.getColor(this, R.color.denied));
                                }
                                polylineOptions.add(latLng);
                            }
                        }
                        // Postupné odstraňování bodů staré polyline a poslední bod, který zůstal v listu by měl být ten poslední
                        polylineOptions.add(polylinePoints.get(0));
                        polyline = mMap.addPolyline(polylineOptions);
                        polylineList.add(polyline);
                        polylineList.remove(closestPolyline);
                        closestPolyline.remove();
                        checkSendButton();
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
        dm.addDataPoints(timestamp, session, latitude, longitude, noise, part);
    }
}