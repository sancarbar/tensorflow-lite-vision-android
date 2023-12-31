package org.adaschool.tflite.vision.android

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.adaschool.tflite.vision.android.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var photoUri: Uri

    private var imageCapture: ImageCapture? = null

    private val outputDirectory: File by lazy {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

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

    private fun setClickListeners() {
        binding.capturePhotoButton.setOnClickListener { takePhoto() }
        binding.newPhotoButton.setOnClickListener { newPhoto() }
        binding.findLabelsButton.setOnClickListener { detectLabels() }
        binding.extractTextButton.setOnClickListener { extractText() }
    }

    private fun extractText() {
        val image = InputImage.fromFilePath(this, photoUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                binding.labelsTextView.text = visionText.text
                binding.labelsTextView.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error extracting text: $e")
            }
    }

    private fun newPhoto() {
        startCamera()
        binding.photoImageView.visibility = View.GONE
        binding.labelsTextView.visibility = View.GONE
        binding.newPhotoButton.visibility = View.GONE
        binding.findLabelsButton.visibility = View.GONE
        binding.extractTextButton.visibility = View.GONE
        binding.cameraPreviewView.visibility = View.VISIBLE
        binding.capturePhotoButton.visibility = View.VISIBLE
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreviewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        binding.progressBar.visibility = View.VISIBLE
        binding.capturePhotoButton.visibility = View.GONE
        binding.cameraPreviewView.visibility = View.GONE

        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )
        val metadata = ImageCapture.Metadata()
        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile).setMetadata(metadata).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    stopCamera()
                    photoUri = Uri.fromFile(photoFile)
                    binding.photoImageView.setImageURI(photoUri)
                    binding.newPhotoButton.visibility = View.VISIBLE
                    binding.findLabelsButton.visibility = View.VISIBLE
                    binding.extractTextButton.visibility = View.VISIBLE
                    binding.photoImageView.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
            }
        )
    }

    private fun detectLabels() {
        binding.progressBar.visibility = View.VISIBLE
        try {
            val image = InputImage.fromFilePath(this, photoUri)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val labelsString = StringBuilder()
                    for (label in labels) {
                        labelsString.append(
                            "${label.text}  ->  ${
                                String.format(
                                    "%.1f",
                                    label.confidence * 100
                                )
                            }%\n"
                        )
                    }
                    runOnUiThread {
                        binding.labelsTextView.text = labelsString.toString()
                        binding.labelsTextView.visibility = View.VISIBLE
                        binding.newPhotoButton.visibility = View.VISIBLE
                        binding.findLabelsButton.visibility = View.VISIBLE
                        binding.extractTextButton.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE
                    }

                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error: $e")
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopCamera() {
        val cameraProvider = ProcessCameraProvider.getInstance(this@MainActivity).get()
        cameraProvider.unbindAll()
    }

    companion object {
        private const val TAG = "TensorFlow Lite Demo"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }


}