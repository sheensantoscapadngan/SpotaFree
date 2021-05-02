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
import android.provider.SyncStateContract;
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
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistsItemsRequest;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import org.apache.hc.core5.http.ParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.cert.TrustAnchor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private String accesstoken = "";
    private SpotifyApi spotifyApi;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_search, container, false);
        this.view = view;
        try {
            setupSpotifyAccess();
        } catch (ParseException | SpotifyWebApiException | IOException e) {
            e.printStackTrace();
        }

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
        link = view.findViewById(R.id.editTextSearch);
        save = view.findViewById(R.id.buttonSearchSave);
        progressBar = view.findViewById(R.id.progressBarSearch);
        textProgress = view.findViewById(R.id.textViewSearchProgressText);
        instructions = view.findViewById(R.id.textViewSearchInstructionsLink);
        clipboard = view.findViewById(R.id.imageViewSearchClipboard);
        updates = view.findViewById(R.id.textViewSearchUpdates);
        viewSourceCode = view.findViewById(R.id.textViewSearchViewSourceCode);

    }

    private void activateListeners() {

        save.setOnClickListener(view -> {

            link.clearFocus();
            process_link(0, 0, null, null);

        });

        instructions.setOnClickListener(v -> {

            instructionsLayout.setVisibility(View.VISIBLE);
            SharedVariables.instructionState = 1;

        });

        clipboard.setOnClickListener(v -> {

            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            String pasteData = "";
            if (!(clipboard.hasPrimaryClip())) {

            } else {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                pasteData = item.getText().toString();
            }
            link.setText(pasteData);

        });

        updates.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/u/3/folders/15DAXWER8lTAxioY8cE7uGUMOjdSjB6TJ"));
            startActivity(intent);

        });

        viewSourceCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sheensantoscapadngan/SpotaFree/tree/master"));
                startActivity(intent);

            }
        });


    }

    private void setupSpotifyAccess() throws ParseException, SpotifyWebApiException, IOException {
        new Thread(() -> {
            spotifyApi = new SpotifyApi.Builder().setClientId(getString(R.string.clientId)).setClientSecret(getString(R.string.clientSecret)).build();
            ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
            try {
                ClientCredentials clientCredentials = clientCredentialsRequest.execute();
                spotifyApi.setAccessToken(clientCredentials.getAccessToken());
                accesstoken = spotifyApi.getAccessToken();
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                e.printStackTrace();
            }

        }).start();

    }

    private void process_link(final int type, final int index, String playlistUrl, final String playlistFolder) {
        
        if(isDownloading){
            Toast.makeText(getActivity().getApplicationContext(), "Cannot start download when another is in progress.", Toast.LENGTH_SHORT).show();
            return;
        }

        isDownloading = true;
        if (playlistUrl == null){
            playlistUrl  = link.getText().toString();
        }
        String finalPlaylistUrl = playlistUrl;
        new Thread(() ->{
            String playlistId = finalPlaylistUrl.split("playlist/")[1].split("\\?")[0];
            GetPlaylistsItemsRequest getPlaylistsItemsRequests = spotifyApi.getPlaylistsItems(playlistId).build();
            Paging<PlaylistTrack> playlistTrackPaging = null;
            try {
                playlistTrackPaging = getPlaylistsItemsRequests.execute();
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                e.printStackTrace();
            }

            ArrayList arrayList = new ArrayList();

            for (int i=0;i<=playlistTrackPaging.getTotal()/100;i++) {
                try {
                    playlistTrackPaging = spotifyApi.getPlaylistsItems(playlistId).offset(i * 100).build().execute();
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    e.printStackTrace();
                }
                for (int j=0;j<playlistTrackPaging.getItems().length;j++){
                    arrayList.add(playlistTrackPaging.getItems()[j].getTrack().getName());
                    System.out.println(playlistTrackPaging.getItems()[j].getTrack().getName());
                }
            }
            try {
                downloadQueries(arrayList, playlistFolder, index, finalPlaylistUrl);
            } catch (YoutubeDLException | InterruptedException | IOException e) {
                e.printStackTrace();
            }

        }).start();

    }

    private void downloadQueries(final ArrayList<String> queries, final String existingPlaylistFolder, final int lastIndex, final String playlistUrl) throws YoutubeDLException, InterruptedException, IOException {
        System.out.println(queries.size());
        getActivity().runOnUiThread(()->{
        save.setClickable(false);
        textProgress.setText("Downloading " + lastIndex + "/" + queries.size());
        textProgress.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        itemNumber = lastIndex;
        globalUrlCopy = playlistUrl;
        float progress = ((float) itemNumber / (float) queries.size()) * 100;
        progressBar.setProgress((int) progress);
        });

        final Thread thread = new Thread(() -> {

            //String extStorageDirectory = getActivity().getExternalFilesDirs("/")[0].toString();
            String extStorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
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
                System.out.println( "DOWNLOADING QUERY:" + query);
                YoutubeDLRequest request = new YoutubeDLRequest("ytsearch1:" + query);
                request.addOption("-ciw");
                request.addOption("--extract-audio");
                request.addOption("-o", playlistFolderDir + "/%(title)s.&(ext)s");
                request.addOption("--audio-format", "mp3");
                try {
                    YoutubeDL.getInstance().execute(request, (progress1, etaInSeconds) -> System.out.println(progress1 + "% (ETA " + etaInSeconds + " seconds)"));

                } catch (YoutubeDLException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                itemNumber = x;
                getActivity().runOnUiThread(() -> {
                    float progress1 = ((float) itemNumber / (float) queries.size()) * 100;
                    progressBar.setProgress((int) progress1);
                    textProgress.setText("Downloading " + (itemNumber + 1) + "/" + queries.size());

                });
                saveLastIndexToDB();
                System.out.println( "DONE DOWNLOADING " + query);

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

