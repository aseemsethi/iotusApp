package com.aseemsethi.iotus.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SettingsViewModel extends ViewModel {

    private MutableLiveData<String> mText;
    public String cid;

    public SettingsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("10000");
    }
    public void setText(String s1) { mText.setValue((s1));}

    public LiveData<String> getText() {
        return mText;
    }
}