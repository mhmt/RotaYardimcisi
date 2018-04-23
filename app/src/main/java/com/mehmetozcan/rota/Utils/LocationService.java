package com.mehmetozcan.rota.Utils;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;


/**
 * Created by mehmet on 24.08.2015.
 */
public class LocationService extends Service implements LocationListener {
    private final Context mContext;
    Location location;
    private static final String TAG = "_LocationService";
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;


    double latitude;
    double longitude;
    final static int PERMISSIONS_REQUEST_CODE = 1;
    private static final long UPDATE_DISTANCE = 10; // Yer Bilgisini Güncelleme Mesafesi :  10 Metre

    private static final long UPDATE_TIME = 1000 * 60 * 1 ; //Yer bilgisini güncelleme zamanı:  1 dakika

    protected LocationManager locationManager;

    public LocationService(Context context){
        this.mContext = context;
        getLocation();

    }

    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // GPS Durumu alınıyor
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // Ağ Durumu alınıyor
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // Hiçbir Ağ Sağlayıcı Yok ise
            } else {
                this.canGetLocation = true;
                // Yer bilgisi Ağ üzerinden alınıyor
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            UPDATE_TIME,
                            UPDATE_DISTANCE, this);
                    Log.d(TAG, "Location Getting from Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            Log.d(TAG,"Location Updated. Lat: "+latitude+"  Long: "+longitude);
                        }
                    }
                }
                // Yer bilgisi GPS üzerinden alınıyor
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                UPDATE_TIME,
                                UPDATE_DISTANCE, this);
                        Log.d(TAG, "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                Log.d(TAG,"Location Updated. Lat: "+latitude+"  Long: "+longitude);

                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    public void stopUsingGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(LocationService.this);
            Log.d(TAG,"GPS using Stopped!");
        }
    }

    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }
        return latitude;
    }

    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }
        return longitude;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        alertDialog.setTitle("GPS does not enabled!");


        alertDialog.setMessage("Do you wanna go to Settings Page for activating GPS?");


        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}