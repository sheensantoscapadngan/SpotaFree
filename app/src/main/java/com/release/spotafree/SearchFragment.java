package com.release.spotafree;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import android.os.Environment;
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

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;


public class SearchFragment extends Fragment {

    private View view;
    private EditText link;
    private Button save;
    private ProgressBar progressBar;
    private TextView textProgress,instructions;
    private static int itemNumber = 0;
    private SQLHelper databaseHelper;
    private ViewPager instructionsViewPager;
    private ConstraintLayout instructionsLayout;
    private InstructionsPagerAdapter pagerAdapter;
    private ImageView clipboard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        this.view = view;
        setupSpotifyAccess();
        initViews();
        activateListeners();
        setupDatabaseHelper();
        attachBroadcastReceivers();
        setupInstructions();
        setupBackPressedListener();
        return view;

    }

    private void setupBackPressedListener() {



    }

    private void setupInstructions() {

        pagerAdapter = new InstructionsPagerAdapter(getActivity().getSupportFragmentManager());
        instructionsLayout = (ConstraintLayout) view.findViewById(R.id.constraintLayoutInstructions);
        instructionsViewPager = (ViewPager) view.findViewById(R.id.viewPagerInstructions);
        instructionsViewPager.setAdapter(pagerAdapter);

    }

    private void attachBroadcastReceivers() {
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(deleteReceiver,new IntentFilter("delete-event"));
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(backReceiver,new IntentFilter("back-event"));
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

    }

    private void activateListeners(){

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                link.clearFocus();
                process_link();

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

                }else {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    pasteData = item.getText().toString();
                }
                link.setText(pasteData);

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

    private String extract_playlist_id(String url){

        String[] entries = url.split("/");
        String id = entries[entries.length-1];
        if(id.indexOf('?') == -1) return id;
        else{
            String[] finalEntries = id.split("[?]");
            Log.d("MAIN_LOG","FINAL ID IS " + finalEntries[0]);
            return finalEntries[0];

        }

    }

    private void process_link(){

        String url = link.getText().toString();
        String playlist_id = extract_playlist_id(url);
        Log.d("SEARCH_LOG",playlist_id);

        String request_url = "https://api.spotify.com/v1/playlists/" + playlist_id + "/tracks?market=US";
        RequestQueue queue = Volley.newRequestQueue(getActivity().getApplicationContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,request_url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("JSON_LOG","RESPONDED!!");
                if (response != null) {
                    try {
                        process_json_result(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("JSON_LOG",error.toString() + " " + error.networkResponse.statusCode);
                Toast.makeText(getActivity().getApplicationContext(), "Error! Try Restarting the App :>", Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                params.put("Authorization","Bearer " + Constants.ACCESS_TOKEN);
                params.put("Accept","application/json");
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

    private void process_json_result(JSONObject result) throws JSONException, InterruptedException, YoutubeDLException, IOException {

        Log.d("MAIN_LOG",result.toString(1));
        ArrayList<String> queries = new ArrayList<>();
        Toast.makeText(getActivity().getApplicationContext(), "Downloading Playlist...", Toast.LENGTH_SHORT).show();

        JSONArray array = result.getJSONArray("items");
        for(int x = 0; x < array.length(); x++){
            JSONObject object = array.getJSONObject(x);

            String song_title = "";
            String album_type = object.getJSONObject("track").getJSONObject("album").getString("album_type");
            if(album_type.equals("single"))
                song_title = object.getJSONObject("track").getJSONObject("album").getString("name");
            else if(album_type.equals("album")){
                song_title = object.getJSONObject("track").getString("name");
            }else{
                continue;
            }

            String artist = object.getJSONObject("track").getJSONArray("artists").getJSONObject(0).getString("name");
            String query = song_title + " " + artist;
            queries.add(query);

        }

        downloadQueries(queries);
    }

    private void downloadQueries(final ArrayList<String> queries) throws YoutubeDLException, InterruptedException, IOException {

        save.setClickable(false);
        itemNumber = 0;
        textProgress.setText("Downloading "+"0/"+queries.size());
        textProgress.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                //String extStorageDirectory = getActivity().getExternalFilesDirs("/")[0].toString();
                String extStorageDirectory = Environment.getExternalStorageDirectory().getPath();
                String playlistName = generatePlaylistName();

                String musicFolderDir = extStorageDirectory + File.separator + "SpotaFree Music";
                File folder = new File(musicFolderDir);
                if(!folder.exists()){
                    folder.mkdir();
                }

                String playlistFolderDir = musicFolderDir + File.separator + playlistName;
                folder = new File(playlistFolderDir);
                if(!folder.exists()){
                    folder.mkdir();
                }

                String duplicateDir = "/storage" + File.separator + "SpotaFree Music" + File.separator + playlistName;
                addPlayListToDB(playlistName,duplicateDir);

                for(int x = 0; x < queries.size(); x++){
                    //Put the thread to sleep after every 4 items to avoid captcha
                    String query = queries.get(x);
                    Log.d("MAIN_LOG","DOWNLOADING QUERY:" + query);
                    YoutubeDLRequest request = new YoutubeDLRequest("ytsearch1:"+query);
                    request.addOption("-ciw");
                    request.addOption("--extract-audio");
                    request.addOption("-o",playlistFolderDir+"/%(title)s.&(ext)s");
                    request.addOption("--audio-format","mp3");
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
                            float progress = ((float)itemNumber/(float)queries.size())*100;
                            progressBar.setProgress((int) progress);
                            textProgress.setText("Downloading "+(itemNumber+1)+"/"+queries.size());
                        }
                    });

                    Log.d("MAIN_LOG","DONE DOWNLOADING " + query);

                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity().getApplicationContext(), "PLAYLIST DOWNLOADED", Toast.LENGTH_SHORT).show();
                        textProgress.setVisibility(View.INVISIBLE);
                        progressBar.setVisibility(View.GONE);
                        save.setClickable(true);
                    }
                });

            }
        });

        thread.start();

    }

    private String generatePlaylistName(){
        int size = databaseHelper.getAllData().getCount()+1;
        String name = "Playlist " + size;
        return name;
    }

    private void addPlayListToDB(String name, String playlistFolderDir) {

        databaseHelper.insertData(name,playlistFolderDir);
        Log.d("MAIN_LOG","ADDED " + name + " TO DB");

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
            if(instructionsLayout.getVisibility() == View.VISIBLE)
                instructionsLayout.setVisibility(View.GONE);
            else{

            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(deleteReceiver);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(backReceiver);

    }

}
