package com.example.albumnextbutton

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ButtonLiveData : ViewModel() {
    val sharedButton: MutableLiveData<Boolean> = MutableLiveData()
}
