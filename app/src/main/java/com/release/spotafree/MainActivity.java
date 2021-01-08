package com.release.spotafree;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLOptions;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ViewPager viewPager;
    private ImageView youtube;
    private Button downloads,search;
    private MainFragmentPagerAdapter viewPagerAdapter;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);
        setupExposureWorkAround();
        setupToolbar();
        setupViewPager();
        initViews();
        activateListeners();
        setupYoutubeDL();

    }

    private void setupExposureWorkAround() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    private void setupYoutubeDL() {
        try{
            YoutubeDL.getInstance().init(getApplication());
            updateYoutubeDL();
            FFmpeg.getInstance().init(getApplication());
            Log.d("MAIN_LOG","Youtube DL initialized");

        }catch(YoutubeDLException e){
            Log.d("MAIN_LOG",e.toString());
        }
    }

    private void updateYoutubeDL() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(getApplicationContext());
                } catch (YoutubeDLException e) {
                    Log.d("MAIN_LOG",e.toString());
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
    private void setupViewPager() {

        viewPager = (ViewPager) findViewById(R.id.viewPagerMain);
        viewPagerAdapter = new MainFragmentPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position == 0){
                    search.setBackgroundResource(R.drawable.search_active);
                    downloads.setBackgroundResource(R.drawable.downloads_unactive);
                }else{
                    downloads.setBackgroundResource(R.drawable.downloads_active);
                    search.setBackgroundResource(R.drawable.search_unactive);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    private void activateListeners() {
        youtube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/channel/UCn4-zUWWsbrO7Kaxi1RsL8w"));
                startActivity(intent);
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                search.setBackgroundResource(R.drawable.search_active);
                downloads.setBackgroundResource(R.drawable.downloads_unactive);
                viewPager.setCurrentItem(0);

            }
        });

        downloads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                downloads.setBackgroundResource(R.drawable.downloads_active);
                search.setBackgroundResource(R.drawable.search_unactive);
                viewPager.setCurrentItem(1);

            }
        });

    }

    private void initViews() {
        youtube = (ImageView) toolbar.findViewById(R.id.imageViewMainApplicationBarYoutube);
        downloads = (Button) findViewById(R.id.buttonMainDownloads);
        search = (Button) findViewById(R.id.buttonMainSearch);

    }

    private void setupToolbar() {
        toolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Constants.AUTH_REQUEST_CODE){
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode,data);
            switch (response.getType()){
                case TOKEN:
                    String token = response.getAccessToken();
                    Log.d("AUTH_CHECK","ACCESS TOKEN IS "+ token);
                    Constants.ACCESS_TOKEN = token;
                    break;
                case ERROR:
                    Log.d("AUTH_CHECK","ERROR: " + response.getError());
                    break;
            }
        }

    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );

        }
    }

    @Override
    public void onBackPressed() {

        if(SharedVariables.instructionState == 1) {
            Intent intent = new Intent("back-event");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            SharedVariables.instructionState = 0;
        }else{
            super.onBackPressed();
        }

    }
}
