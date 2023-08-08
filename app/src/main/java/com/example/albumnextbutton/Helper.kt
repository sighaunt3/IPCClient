package com.example.albumnextbutton

import android.app.Application
import androidx.lifecycle.ViewModelProvider

class Helper: Application() {
    val sharedViewModel: SharedLiveData by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this).create(SharedLiveData::class.java)
    }

    val sharedButtonListener: ButtonLiveData by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this).create(ButtonLiveData::class.java)
    }

    val serverprop: ServerLiveData by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(this).create(ServerLiveData::class.java)
    }
}