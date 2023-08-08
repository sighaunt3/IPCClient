package com.example.albumnextbutton


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ServerLiveData : ViewModel() {
    val serverData: MutableLiveData<Serverprop> = MutableLiveData()
}

data class Serverprop(
    val PID : String,
    val CCount : String
)
