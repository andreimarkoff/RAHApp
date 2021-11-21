package com.example.pahapp;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.os.Build;

import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.EasyPermissions;

public class TrackingUtility {
    public static Boolean hasLocationPermissions(Context context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            if(EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)){
                return true;
            }else {
                return false;
            }
        }else {
            if(EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                return true;
            }else{
                return false;
            }
        }
    }
    public static Float calculatePolylineLength(ArrayList<LatLng> polyline){
        Float distance = 0f;
        for(int i = 0; i<= polyline.size() - 1;i++){
            LatLng pos1 = polyline.get(i);
            LatLng pos2 = polyline.get(i + 1);
            float[] results = new float[1];
            Location.distanceBetween(pos1.latitude, pos1.longitude,pos2.latitude, pos2.longitude, results);
            distance += results[0];
        }
        return distance;
    }
    public static String getFormattedStopWatchTime(Long ms) {
        Long milliSeconds = ms;
        long hours = TimeUnit.MILLISECONDS.toHours(milliSeconds);
        milliSeconds -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliSeconds);
        milliSeconds -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliSeconds);
        String resHours;
        String resMinutes;
        String resSeconds;
        if (hours < 10) {
            resHours = "0" + String.valueOf(hours);
        } else {
            resHours = String.valueOf(hours);
        }
        if (minutes < 10) {
            resMinutes = "0" + String.valueOf(minutes);
        } else {
            resMinutes = String.valueOf(minutes);
        }
        if (seconds < 10) {
            resSeconds = "0" + String.valueOf(seconds);
        } else {
            resSeconds = String.valueOf(seconds);
        }
        return resHours + ":" + resMinutes + ":" + resSeconds;
    }
}