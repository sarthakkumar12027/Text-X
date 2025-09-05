package com.example.textextracter

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var extractedTextView: TextView
    private lateinit var capturedImageView: ImageView

    private val CAMERA_REQUEST_CODE = 987
    private val CAMERA_PERMISSION_CODE = 123

    // 🔑 IMPORTANT: Replace with your actual Gemini API key.
    // This API key is a placeholder and will not work.
    private val geminiModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyA1QNhaPR52pDo7lz6gx2YYEQdVKN4dxWg"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val cameraButton = findViewById<ImageView>(R.id.btnCamera)
        val copyButton = findViewById<ImageView>(R.id.btnCopy)
        extractedTextView = findViewById(R.id.btnResult)
        capturedImageView = findViewById(R.id.cameraImageView)

        val eraseButton = findViewById<ImageView>(R.id.btnErase)
        val aiButton = findViewById<ImageView>(R.id.btnAI)

        cameraButton.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        eraseButton.setOnClickListener {
            extractedTextView.text = ""
            capturedImageView.setImageBitmap(null)
        }

        copyButton.setOnClickListener {
            val extractedText = extractedTextView.text.toString()
            if (extractedText.isNotBlank()) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Label", extractedText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Text Copied", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No text to copy", Toast.LENGTH_SHORT).show()
            }
        }

        aiButton.setOnClickListener {
            val extractedText = extractedTextView.text.toString()
            if (extractedText.isNotBlank()) {
                askAI(extractedText)
            } else {
                Toast.makeText(this, "No text to send to AI", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                capturedImageView.setImageBitmap(bitmap)
                textDetect(bitmap)
            } else {
                Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun textDetect(bitmap: Bitmap) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                capturedImageView.setImageBitmap(null)
                extractedTextView.text = visionText.text
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Something Went Wrong: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun askAI(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = geminiModel.generateContent(
                    content { text(text) }
                )
                withContext(Dispatchers.Main) {
                    extractedTextView.text = response.text ?: "AI did not return any text"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "AI Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
