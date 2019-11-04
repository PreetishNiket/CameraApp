package com.example.camerax

import android.content.pm.PackageManager
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(),Executor {
    var lastStamp=0L

    override fun execute(command: Runnable) {
        command.run()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)
        {
                textureView.post {
                    startCamera()
                }
        }
        else{
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.CAMERA),123)
        }
    }

    private fun startCamera() {
//            val imageCaptureConfig=ImageCaptureConfig.Builder().apply {
//                setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
//            }.build()
//        val imageCapture=ImageCapture(imageCaptureConfig)
//        captureImage.setOnClickListener {
//            val file = File(externalMediaDirs.first(),"${System.currentTimeMillis()}")
//            imageCapture.takePicture(file,this,object :ImageCapture.OnImageSavedListener{
//                override fun onImageSaved(file: File) {
//                }
//                override fun onError(imageCaptureError: ImageCapture.ImageCaptureError, message: String, cause: Throwable?){
//                }
//            })
//        }
             val analyzerConfig=ImageAnalysisConfig.Builder().apply {
                 val thread = HandlerThread("Label").apply {
                     start()
                 }
                 setCallbackHandler(
                     Handler(thread.looper)
                 )
                 setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
             }.build()
        val analyzerCase=ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(LabelAnalyzer())
        }

        val PreviewConfig=PreviewConfig.Builder().apply{
               // setTargetResolution(Size(1080,1080))
            setTargetAspectRatio(Rational(1,1))
            setLensFacing(CameraX.LensFacing.BACK)

        }.build()

        val preview=Preview(PreviewConfig)
        preview.setOnPreviewOutputUpdateListener{
            val parent=textureView.parent as ViewGroup
            parent.removeView(textureView)
            parent.addView(textureView,0)
            updateTransform()
            textureView.surfaceTexture=it.surfaceTexture
        }
        CameraX.bindToLifecycle(this,preview,analyzerCase)

    }
    inner class LabelAnalyzer:ImageAnalysis.Analyzer{

        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            val cureentStampt = System.currentTimeMillis()
            if(cureentStampt-lastStamp>=TimeUnit.SECONDS.toMillis(2))
            {
                lastStamp=cureentStampt
                val x=image.planes[0]
                val y=image.planes[1]
                val z= image.planes[2]


                val xb=x.buffer.remaining()
                val yb=y.buffer.remaining()
                val zb=z.buffer.remaining()


                val data=ByteArray(xb+yb+zb)


                val result: Int = when (rotationDegrees) {
                    0 -> FirebaseVisionImageMetadata.ROTATION_0
                    90 -> FirebaseVisionImageMetadata.ROTATION_90
                    180 -> FirebaseVisionImageMetadata.ROTATION_180
                    270 -> FirebaseVisionImageMetadata.ROTATION_270
                    else -> FirebaseVisionImageMetadata.ROTATION_0

                }
                val metadata=FirebaseVisionImageMetadata.Builder()
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
                    .setHeight(image.height)
                    .setWidth(image.width)
                    .setRotation(result)
                    .build()
                val labelImage=FirebaseVisionImage.fromByteArray(data,metadata)

                FirebaseVision.getInstance().getOnDeviceImageLabeler()
                    .processImage(labelImage)
                    .addOnSuccessListener {
                        if(it.isNotEmpty())
                        {
                            label.text=it[0].text+" "+it[0].confidence
                        }
                    }
            }//end of if
        }

    }
    private fun updateTransform() {
        val matrix = Matrix()

        val centerX = textureView.width / 2f
        val centerY = textureView.height / 2f

        val rotationDegress = when (textureView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }

        matrix.postRotate(-rotationDegress.toFloat(), centerX, centerY)

        textureView.setTransform(matrix)
    }


}

