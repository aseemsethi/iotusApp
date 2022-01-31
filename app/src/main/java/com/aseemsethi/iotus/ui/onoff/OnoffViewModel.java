package com.aseemsethi.iotus.ui.onoff;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class OnoffViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public OnoffViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gateway fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}