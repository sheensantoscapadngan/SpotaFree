package com.release.spotafree;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class MainFragmentPagerAdapter extends FragmentPagerAdapter{

    public MainFragmentPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if(position == 0) return new SearchFragment();
        else return new DownloadsFragment();
    }

    @Override
    public int getCount() {
        return 2;
    }
}
