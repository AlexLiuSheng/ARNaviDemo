package sg.fooyo.arcoretest.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformationSystem
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_base_ar.*
import sg.fooyo.arcoretest.R
import sg.fooyo.arcoretest.tool.ARUtil

open class BaseARActivity : BaseLocationActivity(), Scene.OnPeekTouchListener {


    private val TAG = this.javaClass.simpleName
    private var installRequested: Boolean = false

    private val RC_PERMISSIONS = 0X123

    protected var arSceneView: ArSceneView? = null
    protected lateinit var transformationSystem: TransformationSystem


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ARUtil.checkIsSupportedDeviceOrFinish(this))
            return
        setContentView(R.layout.activity_base_ar)
        arSceneView = ar_scene_view
        setupTransformationSystem()

//        ARUtil.requestCameraPermission(this, RC_PERMISSIONS)

        RxPermissions(this)
            .request(
                Manifest.permission.CAMERA
            ).subscribe {
                if (it) {

                } else {
                    handleFailed()
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupTransformationSystem() {
        val f = FootprintSelectionVisualizer()
        transformationSystem = TransformationSystem(resources.displayMetrics, f)

//        ModelRenderable.builder().setSource(this, com.google.ar.sceneform.ux.R.raw.sceneform_footprint).build().thenAccept { renderable ->
//            if (f.footprintRenderable == null) {
//                f.footprintRenderable = renderable
//            }
//
//        }.exceptionally {
//            val toast = Toast.makeText(this@BaseARActivity, "Unable to load footprint renderable", Toast.LENGTH_SHORT)
//            toast.setGravity(17, 0, 0)
//            toast.show()
//            null
//        }
    }

    private fun handleSuccess() {

    }

    private fun handleFailed() {
        if (!ARUtil.hasCameraPermission(this)) {
            if (!ARUtil.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARUtil.launchPermissionSettings(this)
            } else {
                Toast.makeText(
                    this, "Camera permission is needed to run this application", Toast.LENGTH_LONG
                )
                    .show()
            }
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        arSceneView?.let {
            if (it.session == null) {
                // If the session wasn't created yet, don't resume rendering.
                // This can happen if ARCore needs to be updated or permissions are not granted yet.
                try {
                    val session = ARUtil.createArSession(this, installRequested)
                    if (session == null) {
                        installRequested = ARUtil.hasCameraPermission(this)
                        return
                    } else {
                        it.setupSession(session)
                    }
                } catch (e: UnavailableException) {
                    ARUtil.handleSessionException(this, e)
                }

            }
            try {
                it.scene.addOnPeekTouchListener(this)

                it.resume()
            } catch (ex: CameraNotAvailableException) {
                ARUtil.displayError(this, "Unable to get camera", ex)
                finish()
                return
            }
        }

    }


    public override fun onPause() {
        super.onPause()
        arSceneView?.apply {
            pause()
            scene.removeOnPeekTouchListener(this@BaseARActivity)
        }

    }

    public override fun onDestroy() {
        super.onDestroy()
        arSceneView?.apply {
            destroy()
        }

    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Standard Android full-screen functionality.
            window
                .decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPeekTouch(p0: HitTestResult?, p1: MotionEvent?) {
        transformationSystem.onTouch(p0, p1)

    }


}