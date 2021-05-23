package com.seif.facemaskdetector

import android.graphics.Bitmap
import android.graphics.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// we using bitmap as the machine learning for image processing
// we have to convert the image into matrix as it possible only by bitmap
typealias cameraBitmapOutputLintener = (bitmap: Bitmap) -> Unit

class MainActivity : AppCompatActivity() {
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // this variabel will used for turning the camera from front to rare
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private var camera: Camera? = null

    // it will be using for executing the service for camera in android device
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

      //  setUpML()
        setUpCameraThread()
        setUpCameraControllers()

//        // check if camera permission is granted or not
//        if (!allPermissionsGranted) {
//        //    requireCameraPermission()
//        } else {
//         //   setUpCamera()
//        }


    }

    private fun setUpCameraControllers() {
        // we have to use the button for length facing
        fun setLensButtonIcon() {
            btn_camera_lens_face.setImageDrawable(
                AppCompatResources.getDrawable(
                    applicationContext,
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                        R.drawable.icon_camera_rear
                    else
                        R.drawable.icon_camera_front
                )
            )

        }
        setLensButtonIcon()
        btn_camera_lens_face.setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT== lensFacing){
                CameraSelector.LENS_FACING_BACK
            }
            else{
                CameraSelector.LENS_FACING_FRONT
            }
            setLensButtonIcon()
       //     setupCmaeraUseCases()
        }
        try {
          //  btn_camera_lens_face.isEnabled = hasBackCamera && hasFrontCamera

        }
        catch (exception:CameraInfoUnavailableException){
            btn_camera_lens_face.isEnabled = false
        }
    }



    // this function to get the service of camera
    private fun setUpCameraThread() {
        cameraExecutor = Executors.newSingleThreadExecutor()

    }
}