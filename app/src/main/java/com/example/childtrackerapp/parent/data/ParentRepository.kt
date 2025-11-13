package com.example.childtrackerapp.parent.data

import com.example.childtrackerapp.model.ChildLocation
import com.example.childtrackerapp.model.VoiceMessage
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class ParentRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // 🔹 Lưu vị trí của con
    private val _childLocations = MutableStateFlow<Map<String, ChildLocation>>(emptyMap())
    val childLocations: StateFlow<Map<String, ChildLocation>> = _childLocations

    // 🔹 Lưu tin nhắn từ con
    private val _voiceMessageFromChild = MutableStateFlow<String?>(null)
    val voiceMessageFromChild: StateFlow<String?> = _voiceMessageFromChild

    init {
        db.child("locations").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, ChildLocation>()
                for (child in snapshot.children) {
                    val lat = child.child("lat").getValue(Double::class.java)
                    val lng = child.child("lng").getValue(Double::class.java)
                    if (lat != null && lng != null) {
                        map[child.key ?: "unknown"] = ChildLocation(lat, lng)
                    }
                }
                _childLocations.value = map
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔹 Cha gửi tin nhắn → Con
    suspend fun sendVoiceMessageToChild(childId: String, message: String) {
        val msg = VoiceMessage(
            id = "parent1",
            text = message,
            timestamp = System.currentTimeMillis(),
            from = "parent",
            to = childId
        )


        db.child("messages").child(childId).setValue(msg).await()
        // Xóa sau 10 giây
        GlobalScope.launch {
            delay(10_000L)
            db.child("messages").child(childId).removeValue()
        }
    }

    // 🔹 Lắng nghe tin nhắn từ con
    fun startListeningFromChild(childId: String) {
        db.child("messages").child(childId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg = snapshot.getValue(VoiceMessage::class.java)
                msg?.let {
                    if (it.from == "child") {
                        _voiceMessageFromChild.value = it.text
                        db.child("messages").child(childId).removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
