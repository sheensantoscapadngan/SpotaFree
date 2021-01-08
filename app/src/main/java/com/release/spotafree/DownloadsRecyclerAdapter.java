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

    public DownloadsRecyclerAdapter(ArrayList<String> uriList, ArrayList<String> titleList, Context context) {
        this.uriList = uriList;
        this.titleList = titleList;
        this.context = context;
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

                Toast.makeText(context, "Manually navigate to the provided directory (I couldn't find a workaround lol)", Toast.LENGTH_LONG).show();

            }
        });

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("delete-event");
                intent.putExtra("name",holder.title.getText().toString());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                uriList.remove(position);
                titleList.remove(position);
                notifyDataSetChanged();

            }
        });

    }

    @Override
    public int getItemCount() {
        Log.d("DOWNLOADS_LOG","ITEM COUNT IS" + uriList.size());
        return uriList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView title,location;
        private ConstraintLayout layout;
        private ImageView delete;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.textViewDownloadsListLayoutTitle);
            layout = (ConstraintLayout) itemView.findViewById(R.id.constraintLayoutDownloadsListLayout);
            delete = (ImageView) itemView.findViewById(R.id.imageViewDownloadsListLayoutDelete);
            location = (TextView) itemView.findViewById(R.id.textViewDownloadsListUrl);

        }
    }

}
