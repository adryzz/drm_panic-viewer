package gay.nihil.lena.drm_panic_viewer

import android.Manifest
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executors
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CaptureRequest
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toPointF
import androidx.lifecycle.LifecycleOwner
import gay.nihil.lena.drm_panic_viewer.databinding.ActivityCameraViewBinding
import java.io.ByteArrayOutputStream
import java.io.File
import zxingcpp.BarcodeReader
import zxingcpp.BarcodeReader.Format.*
import kotlin.jvm.java
import androidx.core.net.toUri

class CameraViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraViewBinding
    private val executor = Executors.newSingleThreadExecutor()
    private val permissions = mutableListOf(Manifest.permission.CAMERA)
    private val permissionsRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera_view)
        binding = ActivityCameraViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun bindCameraUseCases() = binding.viewFinder.post {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            // Set up the view finder use case to display camera preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

            // Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9) // -> 1280x720
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val readerCpp = BarcodeReader()


            // Create a new camera selector each time, enforcing lens facing
            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            // Apply declared configs to CameraX using the same lifecycle owner
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, preview, imageAnalysis
            )

            // Reduce exposure time to decrease effect of motion blur
            val camera2 = Camera2CameraControl.from(camera.cameraControl)
            camera2.captureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, 1600)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, -8)
                .build()

            // Use the camera object to link our preview use case with the view
            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->

                    readerCpp.options.apply {
                        formats = setOf(QR_CODE, MICRO_QR_CODE, RMQR_CODE)
                        tryHarder = true
                        tryRotate = true
                        tryInvert = true
                        tryDownscale = true
                        maxNumberOfSymbols = 1
                    }

                    val resultText = try {
                        val results = image.use {
                            readerCpp.read(it)
                        }

                        if (results.isEmpty()) {
                            null
                        } else {
                            results[0].text
                        }


                    } catch (e: Throwable) {
                        e.message ?: "Error"
                    }

                   if (resultText != null) {
                       val uri = resultText.toUri()
                       val data = Intent()
                       data.setData(uri)
                       setResult(RESULT_OK, data);
                       finish()
                   }
            })

        }, ContextCompat.getMainExecutor(this))
    }

    private fun showResult(
        resultText: String,
        fpsText: String?,
        points: List<List<PointF>>,
        image: ImageProxy
    ) =
        binding.viewFinder.post {
            binding.overlay.update(binding.viewFinder, image, points)
        }


    override fun onResume() {
        super.onResume()

        // Request permissions each time the app resumes, since they can be revoked at any time
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                permissionsRequestCode
            )
        } else {
            bindCameraUseCases()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            bindCameraUseCases()
        } else {
            finish() // If we don't have the required permissions, we can't run
        }
    }

    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}