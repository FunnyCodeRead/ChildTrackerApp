package com.example.childtrackerapp.child.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.child.data.ChildRepository

import com.example.childtrackerapp.child.helper.GeoFenceHelper
import com.example.childtrackerapp.service.LocationService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChildViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ChildRepository("child1")
    val isSharingLocation = MutableLiveData(false)

    init {
        // Khởi tạo TTS
        GeoFenceHelper.init(app.applicationContext)

        // Lắng nghe tin nhắn từ cha
        repository.startListeningFromParent()
        viewModelScope.launch {
            repository.voiceMessageFromParent.collectLatest { message ->
                message?.let {
                    android.util.Log.d("ChildViewModel", "Nhận message từ cha: $it")
                    GeoFenceHelper.voiceAlert?.speak(it)
                }
            }
        }
    }

    // Bắt đầu chia sẻ vị trí (foreground service)
    fun startSharing(context: Context, childId: String) {
        val intent = Intent(context, LocationService::class.java)
        intent.putExtra("childId", childId)
        ContextCompat.startForegroundService(context, intent)
        isSharingLocation.value = true
    }

    // Dừng chia sẻ vị trí
    fun stopSharing(context: Context) {
        val intent = Intent(context, LocationService::class.java)
        context.stopService(intent)
        isSharingLocation.value = false
    }

    // Gửi vị trí lên Firebase
    fun sendLocation(context: Context, location: Location) {
        repository.sendLocation("child1", location)
        GeoFenceHelper.checkDangerZone(context, location)
    }

    // Gửi tin nhắn từ con → cha
    fun sendVoiceToParent(message: String) {
        viewModelScope.launch {
            repository.sendVoiceMessageToParent(message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        GeoFenceHelper.release()
    }
}

