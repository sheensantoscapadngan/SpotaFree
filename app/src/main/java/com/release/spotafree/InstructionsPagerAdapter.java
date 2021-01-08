package com.release.spotafree;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class InstructionsPagerAdapter extends FragmentPagerAdapter {

    public InstructionsPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if(position == 0) return new FirstInstructionFragment();
        else if(position == 1) return new SecondInstruction();
        else return new ThirdInstruction();
    }

    @Override
    public int getCount() {
        return 3;
    }
}
