package com.example.childtrackerapp.child.ui.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.childtrackerapp.R
import com.example.childtrackerapp.child.viewmodel.ChildViewModel
import com.example.childtrackerapp.databinding.ActivityMainChildBinding

class MainActivity_Child : AppCompatActivity() {

    private lateinit var binding: ActivityMainChildBinding
    private val viewModel: ChildViewModel by viewModels()
    private val LOCATION_PERMISSION_REQUEST = 101
    private val REQUEST_MIC = 102

    private lateinit var btnMic: ImageButton
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainChildBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val txtStatus = binding.txtStatus
        val btnToggle = binding.btnToggle
        btnMic = binding.btnMic

        // --- Quan sát chia sẻ vị trí ---
        viewModel.isSharingLocation.observe(this) { isSharing ->
            txtStatus.text = if (isSharing) "Đang chia sẻ vị trí" else "Chưa chia sẻ vị trí"
            btnToggle.text = if (isSharing) "Dừng chia sẻ" else "Bắt đầu chia sẻ"
        }

        btnToggle.setOnClickListener {
            if (viewModel.isSharingLocation.value == true) {
                viewModel.stopSharing(this)
            } else {
                if (checkAndRequestPermissions()) {
                    viewModel.startSharing(this, "child1")
                }
            }
        }

        // --- Cấu hình mic ---
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupMicButton()
    }

    /** Nút mic — gửi voice từ con → cha */
    private fun setupMicButton() {
        btnMic.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_MIC
                )
                return@setOnClickListener
            }

            if (!isListening) startListening() else stopListening()
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
        }

        isListening = true
        btnMic.setImageResource(R.drawable.ic_mic_on)

        speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val message = texts?.firstOrNull()?.trim()
                if (!message.isNullOrEmpty()) {
                    viewModel.sendVoiceToParent(message)
                    Toast.makeText(this@MainActivity_Child, "Đã gửi voice đến cha: $message", Toast.LENGTH_SHORT).show()
                }
                stopListening()
            }

            override fun onError(error: Int) {
                Toast.makeText(this@MainActivity_Child, "Lỗi khi nhận giọng nói", Toast.LENGTH_SHORT).show()
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

    private fun stopListening() {
        if (!isListening) return
        isListening = false
        btnMic.setImageResource(R.drawable.ic_mic)
        speechRecognizer.stopListening()
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), LOCATION_PERMISSION_REQUEST)
            false
        } else true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            viewModel.startSharing(this, "child1")
        } else if (requestCode == REQUEST_MIC) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền mic", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Không có quyền mic", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
