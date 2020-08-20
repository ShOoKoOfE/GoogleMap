package com.shokoofeadeli.googlemap;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback , GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private GoogleMap map;
    static final int REQUEST_ERROR = 1001;
    EditText editTextLocation;
    LocationListener locationListener;
    GoogleApiClient googleApiClient;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    ArrayList<LatLng> markerPoints = new ArrayList<>();
    LatLng currentLatLng;
    float zoom = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        editTextLocation = findViewById(R.id.editTextLocation);
        ImageButton imageButtonSearch = findViewById(R.id.imageButtonSearch);

        checkPlayServices(this);

        imageButtonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String locatingName = editTextLocation.getText().toString();
                pointToLocation(locatingName);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();

        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                CurrentLocation();
                return false;
            }
        });

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (markerPoints.size() > 4) {
                    markerPoints.clear();
                    CurrentLocation();
                }
                markerPoints.add(latLng);
                MarkerOptions options = new MarkerOptions();
                options.position(latLng);
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                map.addMarker(options);

                if(markerPoints.size() == 5){
                    measureDistance();
                }
            }
        });

        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }
            @Override
            public View getInfoContents(Marker marker) {
                LinearLayout info = new LinearLayout(MapsActivity.this);
                info.setOrientation(LinearLayout.VERTICAL);
                TextView title = new TextView(MapsActivity.this);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());
                TextView snippet = new TextView(MapsActivity.this);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());
                info.addView(title);
                info.addView(snippet);
                return info;
            }
        });
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;
            }
        });
    }

    private void measureDistance() {
        Location currentLocation = new Location("");
        currentLocation.setLatitude(currentLatLng.latitude);
        currentLocation.setLongitude(currentLatLng.longitude);

        Location targetLocation = new Location("");
        targetLocation.setLatitude(markerPoints.get(0).latitude);
        targetLocation.setLongitude(markerPoints.get(0).longitude);

        float minDistanceInMeters = targetLocation.distanceTo(currentLocation);
        LatLng destination = markerPoints.get(0);

        for(LatLng latLng : markerPoints){
            targetLocation.setLatitude(latLng.latitude);
            targetLocation.setLongitude(latLng.longitude);
            float distanceInMeters =  targetLocation.distanceTo(currentLocation);
            if(minDistanceInMeters>distanceInMeters){
                minDistanceInMeters = distanceInMeters;
                destination = latLng;
            }
        }
        Toast.makeText(this,"Min Distance In Meters IS :" + minDistanceInMeters + " And Latitude Is : " + destination.latitude + " And Longitude Is : " +  destination.longitude,Toast.LENGTH_LONG).show();
        //drawingRoute(destination);
    }

    private void drawingRoute(LatLng destination) {
        LatLng origin = currentLatLng;
        String url = getDirectionsUrl(origin, destination);
        DownloadTask downloadTask = new DownloadTask(map);
        downloadTask.execute(url);
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String mode = "mode=driving";
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;
        String output = "json";
        return getString(R.string.url) + output + "?" + parameters + "&key=" + "AIzaSyBIMrTQhaDyVMWtyKKNuKt_SEgM1xD30Sc";
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        CurrentLocation();
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng place = new LatLng(location.getLatitude(), location.getLongitude());
                gotoLocation(place);
                LocationRequest locationRequest = new LocationRequest();
                locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
                locationRequest.setInterval(5000);
                locationRequest.setFastestInterval(1000);

                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        for (Location location : locationResult.getLocations()) {
                            LatLng place = new LatLng(location.getLatitude(), location.getLongitude());
                            gotoLocation(place);
                        }
                    }
                };

                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                // LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, (com.google.android.gms.location.LocationListener) locationListener);
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
                fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,null);
            }
        };

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void checkPlayServices(final Activity activity) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int errorCode = apiAvailability.isGooglePlayServicesAvailable(activity);

        if (errorCode != ConnectionResult.SUCCESS) {
            Dialog errorDialog = apiAvailability
                    .getErrorDialog(activity, errorCode, REQUEST_ERROR,
                            new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    activity.finish();
                                }
                            });
            errorDialog.show();
        } else {
            initMap();
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void pointToLocation(String placeName) {
        map.clear();
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocationName(placeName, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses.size() > 0) {
            double latitude = addresses.get(0).getLatitude();
            double longitude = addresses.get(0).getLongitude();
            LatLng place = new LatLng(latitude, longitude);
            map.addMarker(new MarkerOptions()
                    .position(place)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .anchor(0.5f, 1f)
                    .title("Marker in " + placeName)
                    .snippet("Place Name : " + placeName)
                    .draggable(true));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(place, zoom));
        }
    }

    private void CurrentLocation() {
        map.clear();
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(MapsActivity.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                map.addMarker(new MarkerOptions()
                        .position(currentLatLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                        .anchor(0.5f, 1f)
                        .snippet("Your Current Location")
                        .draggable(true));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoom));
            }
        });
    }

    private void gotoLocation(LatLng latLng) {
        map.addMarker(new MarkerOptions()
                .position(latLng)
                .anchor(0.5f, 1f)
                .draggable(true));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        //LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, (com.google.android.gms.location.LocationListener) locationListener);
    }
}

