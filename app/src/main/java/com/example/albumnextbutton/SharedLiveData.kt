package com.example.albumnextbutton

import android.provider.MediaStore.Audio.Albums
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedLiveData : ViewModel() {
    val sharedMutableData: MutableLiveData<catfact> = MutableLiveData()
}