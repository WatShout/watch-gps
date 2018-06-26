package com.watshout.tracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class MapFragment extends android.app.Fragment implements OnMapReadyCallback {
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    ArrayList<String> requestIDs = new ArrayList<>();

    ArrayList<Marker> markerList;
    MapPlotter mapPlotter;
    MapView mv;


    // General database reference
    DatabaseReference ref = FirebaseDatabase
            .getInstance()
            .getReference();

    // Not sure which of these is better
    FirebaseUser thisUser = FirebaseAuth.getInstance().getCurrentUser();

    String uid = thisUser.getUid();
    String email = thisUser.getEmail();
    String name = thisUser.getDisplayName();

    GoogleMap googleMapGlobal;
    LatLng home;

    String CURRENT_DEVICE_ID;

    Boolean isMapMoving;

    static boolean GPSconnected = false;
    static boolean currentlyTrackingLocation = false;
    static boolean activityRunning = false;

    XMLCreator XMLCreator;

    PopupWindow popupWindow;
    LayoutInflater layoutInflater;

    RelativeLayout mRelativeLayout;

    // Resource file declarations
    Button mStart;
    Button mLap;
    Button mStop;
    Button mCurrent;
    Button mSignOut;
    Button mAddFriend;
    Button mViewFriends;
    TextView mGreeting;
    TextView mTimerText;
    long originalStartTime;

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    Handler handler;
    int Seconds, Minutes, MilliSeconds ;

    private boolean timeRunning = false;

    String[] permissions = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION};

    // For permissions
    int permCode = 200;

    // Gets a unique hardware ID for a device
    @SuppressLint("HardwareIds")
    String getDeviceID() {
        return Settings.Secure.getString(getActivity().getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    // This runs as the map rendering is completed
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        // IMPORTANT
        // This ensures that we can make changes to the map outside of this function
        // which is why we defined it globally
        googleMapGlobal = googleMap;

        // This sets the starting zoom level
        float zoom = 16;

        // Marker list is a array of the current user's Markers
        markerList = new ArrayList<>();

        mapPlotter = new MapPlotter(markerList, googleMapGlobal, true);

        try {
            XMLCreator = new XMLCreator(getActivity().getApplicationContext(), uid);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        FriendData friendData = new FriendData(uid, googleMapGlobal);

        // Starts location-getting process
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity().getApplicationContext());
        FusedLocation fusedLocation = null;
        try {
            fusedLocation = new FusedLocation(getActivity().getApplicationContext(), mapPlotter, uid, XMLCreator);
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        locationRequest = fusedLocation.buildLocationRequest();
        locationCallback = fusedLocation.buildLocationCallback();
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        mapPlotter.moveCamera(zoom);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_map, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
       checkLocationPermissions();

        // This helps the app not crash in certain contexts
        MapsInitializer.initialize(getActivity().getApplicationContext());

        mStart = view.findViewById(R.id.start);
        mStop = view.findViewById(R.id.stop);
        //mCurrent = findViewById(R.id.current);
        //mSignOut = findViewById(R.id.signout);
        //mGreeting = findViewById(R.id.greeting);
        //mAddFriend = findViewById(R.id.addfriend);
        //mViewFriends = findViewById(R.id.viewFriends);
        mTimerText = view.findViewById(R.id.timerText);
        //mLap = findViewById(R.id.lap);
        handler = new Handler() ;

        String greetingText = "Hello, " + email;

        //mGreeting.setText(greetingText);
        mStart.setBackgroundResource(android.R.drawable.btn_default);

        //mRelativeLayout = findViewById(R.id.relative);

        isMapMoving = true;
        mv = (MapView) view.findViewById(R.id.map);
        mv.onCreate(null);
        mv.onResume();
        mv.getMapAsync(this);
        //FragmentManager fm = getChildFragmentManager();
        //SupportMapFragment fragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
       // if (fragment == null)
         //   fragment = SupportMapFragment.newInstance();
        // Defines a 'fragment' of the activity dedicated to the map
        //SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        //SupportMapFragment m = ((SupportMapFragment) getChildFragmentManager()
               //.findFragmentById(R.id.map));
        //SupportMapFragment map= ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        //MapFragment fragment = getChildFragmentManager().findFragmentById(R.id.map);
        //SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
              //  .findFragmentById(R.id.map);
        //mapFragment.getMapAsync(this);

        // Removes the top bar on top of the map
        //getSupportActionBar().hide();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int displayHeight = displayMetrics.heightPixels;
        final int displayWidth = displayMetrics.widthPixels;

        // I know global variables are bad but I have no clue how else to do this
        CURRENT_DEVICE_ID = getDeviceID();
        CurrentID.setCurrent(CURRENT_DEVICE_ID);

        // This is the initial check to see if a user is 'new' or not
        ref.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Log.d("USER", dataSnapshot.exists() + "");

                if (!dataSnapshot.exists()) {

                    layoutInflater = (LayoutInflater) getActivity()
                            .getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    assert layoutInflater != null;
                    ViewGroup container = (ViewGroup) layoutInflater.inflate(R.layout.popup, null);

                    popupWindow = new PopupWindow(container, displayWidth, displayHeight, true);
                    popupWindow.showAtLocation(mRelativeLayout, Gravity.NO_GRAVITY, 0, 0);

                    final EditText mAge = container.findViewById(R.id.age);

                    Button mButton = container.findViewById(R.id.submitinfo);
                    mButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            int age = Integer.parseInt(mAge.getText().toString());

                            ref.child("users").child(uid).child("name").setValue(name);
                            ref.child("users").child(uid).child("age").setValue(age);
                            ref.child("users").child(uid).child("email").setValue(email);
                            ref.child("users").child(uid).child("device").child("ID").setValue(CURRENT_DEVICE_ID);

                            popupWindow.dismiss();

                        }
                    });

                    ref.child("users").child(uid).child("device").child("name").setValue(android.os.Build.MODEL);


                } else {
                    ref.child("users").child(uid).child("device").child("ID").setValue(CURRENT_DEVICE_ID);
                    ref.child("users").child(uid).child("device").child("name").setValue(android.os.Build.MODEL);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Ideally we would want this to be the location one is at when they start the app
        home = new LatLng(37.4419, -122.1430);

        /*
        // Sets current location
        mCurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkLocationPermissions();
                checkLocationPermissions();

                Log.wtf("IDS", requestIDs.toString());

                if (GPSconnected) {

                    mapPlotter.moveCamera(16);

                }
            }
        });
        */

        /*
        mLap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (timeRunning){
                    Log.d("TIME", mTimerText.getText().toString());
                    StartTime = SystemClock.uptimeMillis();
                    TimeBuff = 0;
                    handler.postDelayed(runnable, 0);
                } else {

                }
            }
        });
        */

        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (timeRunning){
                    TimeBuff += MillisecondTime;
                    handler.removeCallbacks(runnable);
                    timeRunning = false;
                } else {
                    StartTime = SystemClock.uptimeMillis();
                    originalStartTime = StartTime;
                    handler.postDelayed(runnable, 0);
                    timeRunning = true;
                }

                if (!activityRunning){
                    ref.child("users").child(uid).child("device").child("current").addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            dataSnapshot.getRef().removeValue();

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {


                        }

                    });

                    activityRunning = true;
                }

                if (!currentlyTrackingLocation){
                    mStart.setBackgroundColor(Color.GREEN);
                    mStart.setText("PAUSE");
                } else {
                    mStart.setBackgroundResource(android.R.drawable.btn_default);
                    mStart.setText("PLAY");
                }

                currentlyTrackingLocation = !currentlyTrackingLocation;


            }
        });

        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MillisecondTime = 0L ;
                StartTime = 0L ;
                TimeBuff = 0L ;
                UpdateTime = 0L ;
                Seconds = 0 ;
                Minutes = 0 ;
                MilliSeconds = 0 ;
                handler.removeCallbacks(runnable);
                timeRunning = false;

                mTimerText.setText("0:00");

                mStart.setBackgroundResource(android.R.drawable.btn_default);
                currentlyTrackingLocation = false;
                mapPlotter.clearPolyLines();
                activityRunning  = false;

                // Performs Firebase realtime database operations
                UploadToDatabase uploadToDatabase = new UploadToDatabase(uid);
                uploadToDatabase.moveCurrentToPast();

                String date = uploadToDatabase.getFormattedDate();

                try {
                    XMLCreator.saveFile(date);
                } catch (TransformerException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    XMLCreator.uploadToFirebaseStorage();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                XMLCreator.resetXML();


            }

        });

        /*
        mSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthUI.getInstance()
                        .signOut(getApplicationContext())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                // user is now signed out

                                Intent openSignIn = new Intent(getApplicationContext(), SignInActivity.class);
                                getApplicationContext().startActivity(openSignIn);
                                finish();
                            }
                        });
            }
        });

        mViewFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });
        */

        // TODO: Turn this into push notification
        ref.child("friend_requests").child(uid).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                String theirID = dataSnapshot.getKey();
                String requestType = "";

                requestIDs.add(theirID);

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    requestType = (String) child.getValue();
                }

                assert requestType != null;
                if (requestType.equals("received")) {

                    Toast.makeText(getActivity().getApplicationContext(), theirID + " wants to be your friend", Toast.LENGTH_SHORT).show();

                } else if (requestType.equals("sent")) {

                    // Do nothing

                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            int milliFirstDigit = Integer.parseInt(Integer.toString(MilliSeconds).substring(0, 1));

            mTimerText.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds));

            handler.postDelayed(this, 0);
        }

    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 200: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission enabled

                } else {

                    checkLocationPermissions();

                }
            }
        }
    }

    public void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this.getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this.getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this.getActivity(),
                    permissions,
                    permCode);

        }
    }

}