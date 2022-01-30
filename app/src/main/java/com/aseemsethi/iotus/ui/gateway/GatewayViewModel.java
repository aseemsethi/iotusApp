package com.aseemsethi.iotus.ui.gateway;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GatewayViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public GatewayViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gateway fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}