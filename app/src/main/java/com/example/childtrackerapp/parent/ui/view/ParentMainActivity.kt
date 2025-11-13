package com.example.childtrackerapp.parent.ui.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.childtrackerapp.R
import com.example.childtrackerapp.model.ChildLocation
import com.example.childtrackerapp.parent.ui.viewmodel.ParentViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ParentMainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private val viewModel: ParentViewModel by viewModels()
    private val markers = mutableMapOf<String, Marker>()
    private lateinit var btnMic: ImageButton
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false

    private val REQUEST_MIC = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_parent_main)

        mapView = findViewById(R.id.map)
        mapView.setMultiTouchControls(true)
        btnMic = findViewById(R.id.btnMic)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        checkMicPermission()
        setupMicButton()
        observeLocations()
    }

    /** Quan sát vị trí con trên bản đồ */
    private fun observeLocations() {
        lifecycleScope.launch {
            viewModel.childLocations.collectLatest { locations ->
                updateMarkers(locations)
            }
        }
    }

    private fun updateMarkers(locations: Map<String, ChildLocation>) {
        for ((id, loc) in locations) {
            val pos = GeoPoint(loc.lat, loc.lng)
            val marker = markers[id]
            if (marker == null) {
                val m = Marker(mapView)
                m.position = pos
                m.title = "Con: $id"
                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(m)
                markers[id] = m
                if (markers.size == 1) {
                    mapView.controller.setZoom(15.0)
                    mapView.controller.setCenter(pos)
                }
            } else marker.position = pos
        }
        mapView.invalidate()
    }

    /** Kiểm tra quyền mic */
    private fun checkMicPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MIC
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền mic đã được cấp", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Không thể sử dụng mic vì quyền bị từ chối", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Cấu hình nút mic */
    private fun setupMicButton() {
        btnMic.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                checkMicPermission()
                return@setOnClickListener
            }

            if (!isListening) startListening() else stopListening()
        }
    }

    /** Bắt đầu ghi âm */
    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
        }

        isListening = true
        btnMic.setImageResource(R.drawable.ic_mic_on)

        speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val message = texts?.firstOrNull()?.trim()
                if (!message.isNullOrEmpty()) {
                    viewModel.sendVoiceMessage("child1", message)
                    Toast.makeText(this@ParentMainActivity, "Đã gửi: $message", Toast.LENGTH_SHORT).show()
                }
                stopListening()
            }

            override fun onError(error: Int) {
                Toast.makeText(this@ParentMainActivity, "Lỗi khi nhận giọng nói", Toast.LENGTH_SHORT).show()
                stopListening()
            }

            override fun onReadyForSpeech(p0: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    /** Dừng ghi âm */
    private fun stopListening() {
        if (!isListening) return
        isListening = false
        btnMic.setImageResource(R.drawable.ic_mic)
        btnMic.postDelayed({ speechRecognizer.stopListening() }, 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
