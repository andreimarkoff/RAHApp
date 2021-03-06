package com.workout.pahapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.workout.pahapp.databinding.FragmentHomeBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class HomeFragment extends Fragment implements OnMapReadyCallback, EasyPermissions.PermissionCallbacks {
    private FragmentHomeBinding binding;

    private GoogleMap mMap;

    ArrayList<ArrayList<LatLng>> pathPoints = new ArrayList<ArrayList<LatLng>>();
    private Long curTimeMillis;
    private Boolean isTracking = false;

    private RunViewModel mRunViewModel;
    private UserViewModel mUserViewModel;

    private Float weight = 80f;
    private String age = "18";
    private String sex = "Male";
    TrackingService service = new TrackingService();



    public HomeFragment() {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        pathPoints.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        mUserViewModel.getAllUser().observe(getViewLifecycleOwner(), new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                if(!users.isEmpty()){
                    int sizeOfUserList = users.size();
                    if (users.get(sizeOfUserList-1).getWeight()!=null){
                        weight = users.get(sizeOfUserList-1).getWeight();
                        age = users.get(sizeOfUserList-1).getAge();
                        sex = users.get(sizeOfUserList-1).getSex();
                    }
                }
            }
        });

    }


    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRunViewModel = ViewModelProviders.of(this).get(RunViewModel.class);

        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getChildFragmentManager().findFragmentById(R.id.map).onCreate(savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        BottomNavigationView botNavView = getActivity().findViewById(R.id.bottomNavigationView);
        requestPermissions();
        binding.startRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRun();
                Log.d(String.valueOf(1), "Sosamba");
                binding.finishRun.setVisibility(View.VISIBLE);
                binding.startRun.setVisibility(View.GONE);
                botNavView.setVisibility(View.GONE);
            }
        });
        binding.finishRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endRun();
                unsubscribeFromObservers(service);
                binding.finishRun.setVisibility(View.GONE);
                TrackingService.handler.removeCallbacks(TrackingService.timer);
                binding.startRun.setVisibility(View.VISIBLE);
                botNavView.setVisibility(View.VISIBLE);
            }
        });
        mapFragment.getMapAsync(this);

        subscribeToObservers(service);

    }
    private void unsubscribeFromObservers(TrackingService service){
        service.isTracking.removeObservers(this);
        service.pathPoints.removeObservers(this);
        service.timeRunInMillis.removeObservers(this);
        Log.d(String.valueOf(1), "Kek:" + service.isTracking.hasActiveObservers() + service.pathPoints.hasActiveObservers() + service.timeRunInMillis.hasActiveObservers());
    }
    private void subscribeToObservers(TrackingService service){ ;
        service.isTracking.observeForever(new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                Log.d(String.valueOf(2), "Traaaaaacking " + aBoolean );
                updateTracking(aBoolean);

            }
        });
        service.pathPoints.observeForever(new Observer<ArrayList<ArrayList<LatLng>>>() {
            @Override
            public void onChanged(ArrayList<ArrayList<LatLng>> arrayLists) {
                pathPoints = arrayLists;
                addLatestPolyline();
                moveCameraToUser();
                Log.d(String.valueOf(2), "pathpoints observer" );
                addAllPolylines();
            }
        });
        service.timeRunInMillis.observeForever(new Observer<Long>() {
            @Override
            public void onChanged(Long aLong) {
                curTimeMillis = aLong;
                String formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeMillis);
                if (binding != null) {
                    binding.duration.setText(formattedTime);
                }
            }
        });

    }
    private void startRun(){

        if(isTracking){
            Log.d(String.valueOf(2), "StartRUn" + isTracking );
            sendCommand("ACTION_PAUSE_SERVICE");
        }else {
            sendCommand("ACTION_START_OR_RESUME_SERVICE");

        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
           mMap = googleMap;
            if(TrackingUtility.hasLocationPermissions(requireContext())){
                if(mMap!= null){
                    mMap.setMyLocationEnabled(true);
                }
            }
           addAllPolylines();
    }
    private void updateTracking(Boolean isTracking){
        this.isTracking = isTracking;
        if(isTracking){
            Log.d(String.valueOf(99), "isTrah ");
        }else{

        }
    }
    private void addLatestPolyline(){
        int lastIndex = pathPoints.size() - 1;
        if(!pathPoints.isEmpty() && pathPoints.get(lastIndex).size() > 1){
            LatLng preLastLatLng = pathPoints.get(lastIndex).get(pathPoints.get(lastIndex).size() - 2);
            ArrayList<LatLng> arLastLatLng = pathPoints.get(lastIndex);
            Log.d(String.valueOf(99), "pathPoints are" + pathPoints);
            LatLng lastLatLng = arLastLatLng.get(arLastLatLng.size() - 1);
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.color(Color.RED);
            polylineOptions.width(8f);
            polylineOptions.add(preLastLatLng);
            polylineOptions.add(lastLatLng);
            mMap.addPolyline(polylineOptions);
        }
    }
    private void addAllPolylines(){
        for (ArrayList<LatLng> polyline: pathPoints) {
            PolylineOptions polylineOptions = new PolylineOptions().color(Color.RED).width(8f).addAll(polyline);
            Log.d(String.valueOf(99), "addAllPolylines" + polylineOptions.getPoints().toString());
            mMap.addPolyline(polylineOptions);
            Log.d(String.valueOf(5), "map" + mMap.toString());
        }
    }
    private void endRun(){
        mMap.snapshot(new GoogleMap.SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                int distanceInMeters = 0;
                for (ArrayList<LatLng> polyline: pathPoints) {
                    distanceInMeters += Math.round(TrackingUtility.calculatePolylineLength(polyline));
                }
                float avgSpeed = Math.round((distanceInMeters / 1000f) / (curTimeMillis / 1000f / 60 / 60) * 10) / 10f;
                long dateTimeStamp = Calendar.getInstance().getTimeInMillis();
                float caloriesBurned = (distanceInMeters / 1000f) * weight;
                Log.d(String.valueOf(3), "onSnapshotReady: " + dateTimeStamp + ":  " + avgSpeed + " :  " + pathPoints);
                float rating= TrackingUtility.getRating(distanceInMeters,curTimeMillis,sex);
                Run run = new Run(TrackingUtility.bitmapToBytes(bitmap), dateTimeStamp, avgSpeed, distanceInMeters, curTimeMillis, (int) caloriesBurned, rating);
                mRunViewModel.insertRun(run);
                pathPoints.clear();
                sendCommand("ACTION_STOP_SERVICE");
            }
        });
    }
    private void sendCommand(String action){
        Intent intent = new Intent(requireContext(),TrackingService.class);
        intent.setAction(action);
        requireContext().startService(intent);
    }
    private void moveCameraToUser(){
        int lastIndex = pathPoints.size() - 1;
        if(!pathPoints.isEmpty() && !pathPoints.get(lastIndex).isEmpty()){
            ArrayList<LatLng> last = pathPoints.get(lastIndex);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(last.get(last.size() -1), 15f));
        }
    }


    private void requestPermissions(){
        if(TrackingUtility.hasLocationPermissions(requireContext())){
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            EasyPermissions.requestPermissions(this, "In order to use this app location permissions should be accepted",0,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);
        }else{
            EasyPermissions.requestPermissions(
                    this,
                    "You need to accept location permissions to use this app.",
                    0,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            );
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder dialog = new AppSettingsDialog.Builder(this);
            dialog.build().show();
        } else {
            requestPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);
    }
}