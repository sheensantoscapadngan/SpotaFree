package com.release.spotafree;

import android.database.Cursor;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class DownloadsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View view;
    private ArrayList<String> titleList,uriList;
    private DownloadsRecyclerAdapter recyclerAdapter;
    private SQLHelper databaseHelper;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_downloads, container, false);
        this.view = view;

        setupRecyclerView();
        loadExistingDownloads();

        return view;
    }

    private void setupRecyclerView() {

        titleList = new ArrayList<>();
        uriList = new ArrayList<>();

        recyclerAdapter = new DownloadsRecyclerAdapter(uriList,titleList,getActivity().getApplicationContext());
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerViewDownloads);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(recyclerAdapter);
        databaseHelper = new SQLHelper(getActivity().getApplicationContext());

    }

    private void loadExistingDownloads() {

        Cursor res = databaseHelper.getAllData();
        Log.d("MAIN_LOG","GET RESULT SIZE " + res.getCount());
        while(res.moveToNext()){
            titleList.add(res.getString(1));
            uriList.add(res.getString(2));
            recyclerAdapter.notifyDataSetChanged();
        }

    }

}
