package com.aseemsethi.iotus.ui.temp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TempViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public TempViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is temp fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}