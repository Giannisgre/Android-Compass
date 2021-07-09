package eu.giannisgrevenitis.compass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocationListener{
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private Compass compass;
    private LocationManager locationManager;
    private Location mLocation;
    private Double latitude = 0.0, longitude = 0.0;
    private TextView longTextView, latTextView;
    private boolean isGPSEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
        compass = new Compass(this);
        compass.arrow =  findViewById(R.id.arrowImage);
        longTextView = findViewById(R.id.longText);
        latTextView = findViewById(R.id.latText);
        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(),R.raw.sound1);
        mediaPlayer.start();
    }

    // For Compass
    @Override
    protected void onStart(){
        super.onStart();
        compass.start();
        procedures();
    }

    @Override
    protected void onStop(){
        super.onStop();
        compass.stop();
        writeToFile();
    }

    @Override
    protected void onPause(){
        super.onPause();
        compass.stop();
    }

    @Override
    protected  void onResume(){
        super.onResume();
        compass.start();
    }

    //Permissions
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this).setMessage(message).setPositiveButton("OK", okListener).setNegativeButton("Cancel", null).create().show();
    }

    private void procedures(){
        List<String> permissionsNeeded = new ArrayList<>();

        final List<String> permissionsList = new ArrayList<>();
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("GPS");
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionsNeeded.add("Write Storage");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++) {
                    message = new StringBuilder().append(message).append(", ").append(permissionsNeeded.get(i)).toString();
                }
                showMessageOKCancel(message, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }
        //Every time the procedures() method is called, other than the first one, getLocation() is called.
		getLocation();
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
            {
                Map<String, Integer> perms = new HashMap<>();
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    //Call getLocation() when user accepts permissions
                    getLocation();
                }
                else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "A Permission is Denied. Functionality not guaranteed.", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //**For Fine Location**
    @Override
    public void onLocationChanged(Location location){
        updateLocation(location);
    }
    @Override
    public void onProviderDisabled(String provider){}
    @Override
    public void onProviderEnabled(String provider){}
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras){}

	//Method to get longitude and latitude and update the corresponding UI TextView.
    public void updateLocation(Location location){
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        String userlat = "Lat: " + latitude;
        String userlong = " Long: " + longitude;
        latTextView.setText(userlat);
        longTextView.setText(userlong);

    }

    @SuppressLint("MissingPermission")
    public void getLocation() {
		//Check if GPS is activated
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
			//If yes, update location and update UI accordingly.
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
            if (locationManager != null) {
                mLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (mLocation != null) {
                    updateLocation(mLocation);
                }
            }
        }
    }

    //**For Write External Storage (local db)**
    public void writeToFile(){
        File logFile = new File(getApplicationContext().getFilesDir(), "CompassLog.txt");
        //If file doesn't exist, try creating it
        if(!logFile.exists()){
            try{
                logFile.createNewFile();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        try{
            String myStr = "Lat: " + latitude + " Long: " + longitude;
            String myDefaultStr = "Lat: 0.0 Long: 0.0";
			//First, read from file and check if coordinates i want to append already exist in file (to avoid duplicates)
            BufferedReader bufferedReader = new BufferedReader(new FileReader(logFile));
            String mLine;
            boolean flag = false;
            while((mLine = bufferedReader.readLine()) != null){
                if(mLine.length() > 0) {
                    if (mLine.equals(myStr)) {
                        flag = true;
                    }
                }
            }
            bufferedReader.close();
            //An den tis emperiexei kai an to string mou den einai to default(0.0 , 0.0)
			//If coordinates not in file and myStr doesn't have default values(0.0 , 0.0)
            if((!flag) && !myStr.equals(myDefaultStr)) {
				//Create BufferedWriter for the CompassLog.txt file in append mode
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
				//Append the string to be saved to file, add new line and close BufferedWriter.
                bufferedWriter.append(myStr);
                bufferedWriter.newLine();
                bufferedWriter.close();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
