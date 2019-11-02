package com.example.camerax

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Rational
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

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

        val PreviewConfig=PreviewConfig.Builder().apply{
               // setTargetResolution(Size(1080,1080))
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
        }.build()

        val preview=Preview(PreviewConfig)
        preview.setOnPreviewOutputUpdateListener{
            val parent=textureView.parent as ViewGroup
            parent.removeView(textureView)
            parent.addView(textureView,0)
            textureView.surfaceTexture=it.surfaceTexture
        }
        CameraX.bindToLifecycle(this,preview)

    }
}
