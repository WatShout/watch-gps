package com.watshout.mobile;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;

public class FinishedActivity extends AppCompatActivity{

    FirebaseUser thisUser = FirebaseAuth.getInstance().getCurrentUser();
    String uid = thisUser.getUid();
    String date;

    boolean hasStrava;
    boolean wantsToUploadStrava;

    final double KM_TO_MILE = 0.621371;

    //CheckBox stravaCheckBox;
    ImageView returnToMap;
    TextView time;
    TextView distance;
    TextView pace;
    //String mapURL;
    String displayMapURL;
    String uploadMapURL;
    ImageView mFinishedRun;
    TextView mDistLabel;
    TextView mTimeLabel;
    TextView mPaceLabel;
    TextView mStatisticsText;
    ImageView mShrinker;
    CheckBox mStrava;
    String overlayMapURL;

    RelativeLayout mBottomLayout;

    Button uploadGpx;

    boolean isExpanded = true;

    // General database reference
    DatabaseReference ref = FirebaseDatabase
            .getInstance()
            .getReference();


    void loadMapImage() {
        Picasso.get()
                .load(displayMapURL)
                .into(mFinishedRun);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finished);

        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        //stravaCheckBox = findViewById(R.id.stravaBox);
        returnToMap = findViewById(R.id.back);
        time = findViewById(R.id.time);
        distance = findViewById(R.id.distance);
        pace = findViewById(R.id.pace);
        mFinishedRun = findViewById(R.id.finishedRun);
        mDistLabel = findViewById(R.id.distLabel);
        mPaceLabel = findViewById(R.id.paceLabel);
        mTimeLabel = findViewById(R.id.timeLabel);
        mShrinker = findViewById(R.id.shrinker);
        mStatisticsText = findViewById(R.id.statText);
        uploadGpx = findViewById(R.id.uploadGpx);
        mBottomLayout = findViewById(R.id.bottomLayout);
        mStrava = findViewById(R.id.stravaBox);

        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (250 * scale + 0.5f);

        RelativeLayout.LayoutParams rel_btn = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, pixels);
        rel_btn.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mBottomLayout.setLayoutParams(rel_btn);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        //String units = settings.getString("Units", "Metric");
        String units = "Imperial";

        // Get value to determine whether or nlt to show checkbox
        hasStrava = Boolean.valueOf(getIntent().getStringExtra("STRAVA"));
        displayMapURL = getIntent().getStringExtra("DISPLAY_MAP_URL");
        uploadMapURL = getIntent().getStringExtra("UPLOAD_MAP_URL");
        overlayMapURL = getIntent().getStringExtra("OVERLAY_MAP_URL");

        // ETHAN: this is for the Instagram v0.3 update
        /*
        Target mTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Bitmap newBitMap = bitmap.copy(bitmap.getConfig(), true);
                Canvas canvas = new Canvas(newBitMap);
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setTextSize(40f);
                p.setColor(Color.BLACK);
                canvas.drawText("Testing Hello", 50, 50, p);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                newBitMap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                Log.d("MAPMAP", data.toString());

                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageReference = storage.getReference().child("users")
                        .child(uid).child("overlayImages").child(Long.toString(System.currentTimeMillis()) + ".jpg");

                UploadTask uploadTask = storageReference.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.d("MAPMAP", "FAILED");
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d("MAPMAP", "Uploaded successfully");
                    }
                });
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) { }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) { }
        };

        Picasso.get().load(overlayMapURL).into(mTarget);
        */


        loadMapImage();

        if (!hasStrava) {
            mStrava.setVisibility(View.INVISIBLE);
        }

        mStrava.setChecked(true);

        // load time and distance data
        final int min = getIntent().getIntExtra("MIN",0);
        final int sec = getIntent().getIntExtra("SEC",0);

        DecimalFormat formatter = new DecimalFormat("00");
        String formattedMin = formatter.format(min);
        String formattedSec = formatter.format(sec);

        time.setText(formattedMin + ":" + formattedSec);

        final double rawDistance = Double.valueOf(getIntent().getStringExtra("DISTANCE"));

        // Making this imperial for now
        //final PaceCalculator pc = new PaceCalculator(rawMetricDistance, min, sec, this);
        final PaceCalculator pc = new PaceCalculator(rawDistance, min, sec);

        distance.setText(pc.getDistance() + "");
        pace.setText(pc.getPace() + "");

        // load GPX from carrier class
        final XMLCreator XMLCreator = Carrier.getXMLCreator();

        mShrinker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int visible = isExpanded ? View.INVISIBLE : View.VISIBLE;
                Drawable icon = isExpanded ? ResourcesCompat.getDrawable(getResources(),
                        R.drawable.black_arrow_up, null) :
                        ResourcesCompat.getDrawable(getResources(),
                                R.drawable.black_arrow_down, null);

                int height = isExpanded ? 50 : 250;

                final float scale = getResources().getDisplayMetrics().density;
                int pixels = (int) (height * scale + 0.5f);

                RelativeLayout.LayoutParams rel_btn = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, pixels);
                rel_btn.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                mBottomLayout.setLayoutParams(rel_btn);

                mShrinker.setImageDrawable(icon);

                mDistLabel.setVisibility(visible);
                mPaceLabel.setVisibility(visible);
                mTimeLabel.setVisibility(visible);

                mStatisticsText.setVisibility(visible);

                uploadGpx.setVisibility(visible);

                distance.setVisibility(visible);
                time.setVisibility(visible);
                pace.setVisibility(visible);

                isExpanded = !isExpanded;

            }
        });

        uploadGpx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final ProgressDialog progressDialog = new ProgressDialog(FinishedActivity.this);
                progressDialog.setMessage("Uploading run data...");
                progressDialog.show();

                boolean strava = mStrava.isChecked();

                RunCompletionUploader rcu = new RunCompletionUploader(
                        FinishedActivity.this, uid, pc.getDistance(),
                        pc.getPace(), pc.getTotalSeconds(), uploadMapURL, strava);

                rcu.createActivityOnServer();

                progressDialog.dismiss();

                SharedPreferences preferences = PreferenceManager
                        .getDefaultSharedPreferences(FinishedActivity.this);
                preferences.edit().putBoolean("currentlyTracking", false).apply();

                // Redirect to MapFragment
                Intent openMain = new Intent(getApplicationContext(), MainActivity.class);
                openMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(openMain);
                finish();

            }
        });

        returnToMap.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(FinishedActivity.this);
                SharedPreferences.Editor editor = preferences.edit();

                // Ethan
                editor.putBoolean("currentlyTracking", false);
                editor.apply();

                removeCurrentEntry();

                // Redirect to MapFragment
                Intent openMain = new Intent(getApplicationContext(), MainActivity.class);
                openMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(openMain);
                finish();
            }
        });
    }

    public void removeCurrentEntry() {
        ref.child("users").child(uid).child("device").child("current").removeValue();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed(){

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(FinishedActivity.this);
        SharedPreferences.Editor editor = preferences.edit();

        // Ethan
        editor.putBoolean("currentlyTracking", false);
        editor.apply();

        Intent intent = new Intent(FinishedActivity.this,MainActivity.class);
        startActivity(intent);
        finish();
    }

}
