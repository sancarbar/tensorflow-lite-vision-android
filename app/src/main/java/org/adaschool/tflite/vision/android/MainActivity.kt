package org.adaschool.tflite.vision.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.adaschool.tflite.vision.android.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        checkPermissions()
        setClickListeners()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {

    }

    private fun setClickListeners() {
        binding.capturePhotoButton.setOnClickListener { takePhoto() }
        binding.newPhotoButton.setOnClickListener { newPhoto() }
    }

    private fun takePhoto() {

    }

    private fun newPhoto() {
        startCamera()
        binding.photoImageView.visibility = View.GONE
        binding.newPhotoButton.visibility = View.GONE
        binding.cameraPreviewView.visibility = View.VISIBLE
        binding.capturePhotoButton.visibility = View.VISIBLE
    }


}