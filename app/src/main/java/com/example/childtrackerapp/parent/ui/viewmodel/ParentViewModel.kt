package com.example.childtrackerapp.parent.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.child.helper.VoiceAlertHelper
import com.example.childtrackerapp.model.ChildLocation
import com.example.childtrackerapp.parent.data.ParentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ParentViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ParentRepository()
    private val voiceAlert = VoiceAlertHelper(app.applicationContext)

    val childLocations = repository.childLocations

    init {
        viewModelScope.launch {
            repository.voiceMessageFromChild.collectLatest { msg ->
                msg?.let { voiceAlert.speak(it) }
            }
        }
    }

    fun sendVoiceMessage(childId: String, message: String) {
        viewModelScope.launch { repository.sendVoiceMessageToChild(childId, message) }
    }

    fun listenChild(childId: String) {
        repository.startListeningFromChild(childId)
    }

    override fun onCleared() {
        super.onCleared()
        voiceAlert.release()
    }
}


