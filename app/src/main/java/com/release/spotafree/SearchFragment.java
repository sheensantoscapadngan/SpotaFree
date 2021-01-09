package com.release.spotafree;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.cert.TrustAnchor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.transform.sax.TransformerHandler;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;


public class SearchFragment extends Fragment {

    private View view;
    private EditText link;
    private Button save;
    private ProgressBar progressBar;
    private TextView textProgress, instructions, updates,viewSourceCode;
    private static int itemNumber = 0;
    private static String globalUrlCopy = null;
    private SQLHelper databaseHelper;
    private ViewPager instructionsViewPager;
    private ConstraintLayout instructionsLayout;
    private InstructionsPagerAdapter pagerAdapter;
    private ImageView clipboard;
    private Integer notificationID = 1000;
    private NotificationManager notificationManager;
    private Notification.Builder notificationBuilder;
    private boolean isDownloading = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);
        this.view = view;
        setupSpotifyAccess();
        initViews();
        activateListeners();
        setupDatabaseHelper();
        attachBroadcastReceivers();
        setupInstructions();
        setupNotification();
        endNotification();
        return view;

    }

    private void setupNotification() {

        notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new Notification.Builder(getActivity().getApplicationContext());
        notificationBuilder.setOngoing(true)
                .setContentTitle("SpotaFree")
                .setContentText("Download in Progress")
                .setSmallIcon(R.drawable.icon)
                .setProgress(100, 0, false)
                .setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            String channelId = "ID";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder.setChannelId(channelId);
        }

    }

    private void updateNotification(int max, int current){

        notificationBuilder.setProgress(max,current,false);
        Notification notification = notificationBuilder.build();
        notificationManager.notify(notificationID,notification);

    }

    private void endNotification(){
        notificationManager.cancelAll();
    }

    private void setupInstructions() {

        pagerAdapter = new InstructionsPagerAdapter(getActivity().getSupportFragmentManager());
        instructionsLayout = (ConstraintLayout) view.findViewById(R.id.constraintLayoutInstructions);
        instructionsViewPager = (ViewPager) view.findViewById(R.id.viewPagerInstructions);
        instructionsViewPager.setAdapter(pagerAdapter);

    }

    private void attachBroadcastReceivers() {
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(deleteReceiver, new IntentFilter("delete-event"));
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(backReceiver, new IntentFilter("back-event"));
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(downloadReceiver, new IntentFilter("download-event"));
    }

    private void setupDatabaseHelper() {

        databaseHelper = new SQLHelper(getActivity().getApplicationContext());

    }

    private void initViews() {

        link = (EditText) view.findViewById(R.id.editTextSearch);
        save = (Button) view.findViewById(R.id.buttonSearchSave);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBarSearch);
        textProgress = (TextView) view.findViewById(R.id.textViewSearchProgressText);
        instructions = (TextView) view.findViewById(R.id.textViewSearchInstructionsLink);
        clipboard = (ImageView) view.findViewById(R.id.imageViewSearchClipboard);
        updates = (TextView) view.findViewById(R.id.textViewSearchUpdates);
        viewSourceCode = (TextView) view.findViewById(R.id.textViewSearchViewSourceCode);

    }

    private void activateListeners() {

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                link.clearFocus();
                process_link(0, 0, null, null);

            }
        });

        instructions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                instructionsLayout.setVisibility(View.VISIBLE);
                SharedVariables.instructionState = 1;

            }
        });

        clipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                String pasteData = "";
                if (!(clipboard.hasPrimaryClip())) {

                } else {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    pasteData = item.getText().toString();
                }
                link.setText(pasteData);

            }
        });

        updates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/u/3/folders/15DAXWER8lTAxioY8cE7uGUMOjdSjB6TJ"));
                startActivity(intent);

            }
        });

        viewSourceCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sheensantoscapadngan/SpotaFree/tree/master"));
                startActivity(intent);

            }
        });


    }

    private void setupSpotifyAccess() {
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(Constants.CLIENT_ID, AuthenticationResponse.Type.TOKEN, Constants.REDIRECT_URI);
        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(getActivity(), Constants.AUTH_REQUEST_CODE, request);
    }

    private String extract_playlist_id(String url) {

        String[] entries = url.split("/");
        String id = entries[entries.length - 1];
        if (id.indexOf('?') == -1) return id;
        else {
            String[] finalEntries = id.split("[?]");
            Log.d("MAIN_LOG", "FINAL ID IS " + finalEntries[0]);
            return finalEntries[0];

        }

    }

    private void process_link(final int type, final int index, String playlistUrl, final String playlistFolder) {
        
        if(isDownloading){
            Toast.makeText(getActivity().getApplicationContext(), "Cannot start download when another is in progress.", Toast.LENGTH_SHORT).show();
            return;
        }

        isDownloading = true;

        String url = playlistUrl;
        if(url == null)  url = link.getText().toString();

        String playlist_id = extract_playlist_id(url);
        String request_url = "https://api.spotify.com/v1/playlists/" + playlist_id + "/tracks?market=US";

        RequestQueue queue = Volley.newRequestQueue(getActivity().getApplicationContext());
        final String finalUrl = url;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, request_url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("JSON_LOG", "RESPONDED!!");
                if (response != null) {
                    try {

                        if(type == 0) process_json_result(response, null, 0, finalUrl);
                        else process_json_result(response,playlistFolder,index,finalUrl);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("JSON_LOG", error.toString() + " " + error.networkResponse.statusCode);
                Toast.makeText(getActivity().getApplicationContext(), "Error Downloading. Try Restarting the App.", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                params.put("Authorization", "Bearer " + Constants.ACCESS_TOKEN);
                params.put("Accept", "application/json");
                return params;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                return params;
            }
        };

        queue.add(request);

    }


    private void process_json_result(JSONObject result, String existingPlaylistFolder, int lastIndex, String playlistUrl) throws JSONException, InterruptedException, YoutubeDLException, IOException {

        Log.d("MAIN_LOG", result.toString(1));
        ArrayList<String> queries = new ArrayList<>();
        Toast.makeText(getActivity().getApplicationContext(), "Downloading Playlist...", Toast.LENGTH_SHORT).show();

        JSONArray array = result.getJSONArray("items");
        for (int x = 0; x < array.length(); x++) {
            JSONObject object = array.getJSONObject(x);

            String song_title = "";
            String album_type = object.getJSONObject("track").getJSONObject("album").getString("album_type");
            if (album_type.equals("single"))
                song_title = object.getJSONObject("track").getJSONObject("album").getString("name");
            else if (album_type.equals("album")) {
                song_title = object.getJSONObject("track").getString("name");
            } else {
                continue;
            }

            String artist = object.getJSONObject("track").getJSONArray("artists").getJSONObject(0).getString("name");
            String query = song_title + " " + artist;
            Log.d("SONG_LOG",query);
            queries.add(query);

        }

        downloadQueries(queries, existingPlaylistFolder, lastIndex, playlistUrl);
    }

    private void downloadQueries(final ArrayList<String> queries, final String existingPlaylistFolder, final int lastIndex, final String playlistUrl) throws YoutubeDLException, InterruptedException, IOException {

        save.setClickable(false);
        textProgress.setText("Downloading " + lastIndex + "/" + queries.size());
        textProgress.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        itemNumber = lastIndex;
        globalUrlCopy = playlistUrl;
        float progress = ((float) itemNumber / (float) queries.size()) * 100;
        progressBar.setProgress((int) progress);


        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                //String extStorageDirectory = getActivity().getExternalFilesDirs("/")[0].toString();
                String extStorageDirectory = Environment.getExternalStorageDirectory().getPath();
                String playlistName = generatePlaylistName();

                String musicFolderDir = extStorageDirectory + File.separator + "SpotaFree Music";
                File folder = new File(musicFolderDir);
                if (!folder.exists()) {
                    folder.mkdir();
                }

                String playlistFolderDir = "";
                if(existingPlaylistFolder == null) {
                    playlistFolderDir = musicFolderDir + File.separator + playlistName;
                    folder = new File(playlistFolderDir);
                    if (!folder.exists()) {
                        folder.mkdir();
                    }
                    String duplicateDir = "/storage" + File.separator + "SpotaFree Music" + File.separator + playlistName;
                    addPlayListToDB(playlistName, playlistFolderDir, duplicateDir, playlistUrl);
                }else
                    playlistFolderDir = existingPlaylistFolder;

                for (int x = lastIndex; x < queries.size(); x++) {

                    updateNotification(queries.size(),x);

                    String query = queries.get(x);
                    Log.d("MAIN_LOG", "DOWNLOADING QUERY:" + query);
                    YoutubeDLRequest request = new YoutubeDLRequest("ytsearch1:" + query);
                    request.addOption("-ciw");
                    request.addOption("--extract-audio");
                    request.addOption("-o", playlistFolderDir + "/%(title)s.&(ext)s");
                    request.addOption("--audio-format", "mp3");
                    try {
                        YoutubeDL.getInstance().execute(request);
                    } catch (YoutubeDLException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    itemNumber = x;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            float progress = ((float) itemNumber / (float) queries.size()) * 100;
                            progressBar.setProgress((int) progress);
                            textProgress.setText("Downloading " + (itemNumber + 1) + "/" + queries.size());

                        }
                    });
                    saveLastIndexToDB();
                    Log.d("MAIN_LOG", "DONE DOWNLOADING " + query);

                }

                endNotification();
                itemNumber = 1000;
                saveLastIndexToDB();
                isDownloading = false;

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity().getApplicationContext(), "PLAYLIST DOWNLOADED", Toast.LENGTH_SHORT).show();
                        textProgress.setVisibility(View.INVISIBLE);
                        progressBar.setVisibility(View.GONE);
                        save.setClickable(true);
                        isDownloading = false;
                    }
                });

            }
        });

        thread.start();

    }

    private String generatePlaylistName() {
        int size = databaseHelper.getAllData().getCount() + 1;
        String name = "Playlist " + size;
        return name;
    }

    private void addPlayListToDB(String name, String playlistFolderDir, String duplicateDir, String playlistUrl) {

        databaseHelper.insertData(name, playlistFolderDir, duplicateDir, playlistUrl);
        Log.d("MAIN_LOG", "ADDED " + name + " TO DB");

    }

    private BroadcastReceiver deleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String name = intent.getStringExtra("name");
            databaseHelper.deleteData(name);
            Toast.makeText(getActivity().getApplicationContext(), "DELETED " + name, Toast.LENGTH_SHORT).show();
        }

    };

    private BroadcastReceiver backReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (instructionsLayout.getVisibility() == View.VISIBLE)
                instructionsLayout.setVisibility(View.GONE);
            else {

            }
        }
    };

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String playlistFolder = intent.getStringExtra("playlistFolder");
            String playlistUrl = intent.getStringExtra("playlistUrl");
            int lastIndex = intent.getIntExtra("lastIndex", 0);
            process_link(1, lastIndex, playlistUrl, playlistFolder);
            setupCurrentView();

        }
    };

    private void setupCurrentView() {
        ((MainActivity) getActivity()).getViewPager().setCurrentItem(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(deleteReceiver);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(backReceiver);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(downloadReceiver);

    }

    private void saveLastIndexToDB() {
        databaseHelper.updateLastIndex(globalUrlCopy,itemNumber+1);
    }

}

