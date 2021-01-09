package com.release.spotafree;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

public class DownloadsRecyclerAdapter extends RecyclerView.Adapter<DownloadsRecyclerAdapter.ViewHolder> {

    private ArrayList<String> uriList = new ArrayList<>();
    private ArrayList<String> titleList = new ArrayList<>();
    private Context context;
    private ArrayList<String> playlistUrlList = new ArrayList<>();
    private ArrayList<Integer> lastIndexList = new ArrayList<>();
    private ArrayList<String> playlistFolderList = new ArrayList<>();

    public DownloadsRecyclerAdapter(ArrayList<String> uriList, ArrayList<String> titleList, Context context, ArrayList<String> playlistUrlList, ArrayList<Integer> lastIndexList, ArrayList<String> playlistFolderList) {
        this.uriList = uriList;
        this.titleList = titleList;
        this.context = context;
        this.playlistUrlList = playlistUrlList;
        this.lastIndexList = lastIndexList;
        this.playlistFolderList = playlistFolderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.downloads_list_layout,parent,false);
        return new DownloadsRecyclerAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        holder.title.setText(titleList.get(position));
        holder.location.setText(uriList.get(position));
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(context, "Playlist can be found at the provided directory", Toast.LENGTH_LONG).show();

            }
        });

        if(lastIndexList.get(position) == 1001){
            holder.continueDownload.setVisibility(View.GONE);
        }

        holder.continueDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent("download-event");
                intent.putExtra("playlistUrl",playlistUrlList.get(position));
                intent.putExtra("lastIndex",lastIndexList.get(position));
                intent.putExtra("playlistFolder",playlistFolderList.get(position));
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);


            }
        });

    }

    @Override
    public int getItemCount() {
        Log.d("DOWNLOADS_LOG","ITEM COUNT IS" + uriList.size());
        return uriList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView title,location,continueDownload;
        private ConstraintLayout layout;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.textViewDownloadsListLayoutTitle);
            layout = (ConstraintLayout) itemView.findViewById(R.id.constraintLayoutDownloadsListLayout);
            location = (TextView) itemView.findViewById(R.id.textViewDownloadsListUrl);
            continueDownload = (TextView) itemView.findViewById(R.id.textViewDownloadsListContinue);

        }
    }

}
