package sg.fooyo.arcoretest.tool

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*

object ARUtil {
    val TAG = "ARUtil"
    val MIN_OPENGL_VERSION = 3.0

    fun displayError(
        context: Context, errorMsg: String, problem: Throwable?
    ) {
        val tag = context.javaClass.simpleName
        val toastText: String
        toastText = when {
            problem?.message != null -> {
                Log.e(tag, errorMsg, problem)
                errorMsg + ": " + problem.message
            }
            problem != null -> {
                Log.e(tag, errorMsg, problem)
                errorMsg
            }
            else -> {
                Log.e(tag, errorMsg)
                errorMsg
            }
        }

        Handler(Looper.getMainLooper())
            .post {
                val toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
    }

    fun isSupportARCore(context: Context): Boolean {
        // Likely called from Activity.onCreate() of an activity with AR buttons.
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        if (availability.isTransient) {
            // re-query at 5Hz while we check compatibility.
            Handler().postDelayed({
                isSupportARCore(context)
            }, 200)
        }
        return availability.isSupported

    }

    fun handleSessionException(
        activity: android.support.v7.app.AppCompatActivity, sessionException: UnavailableException
    ) {

        val message: String
        if (sessionException is UnavailableArcoreNotInstalledException) {
            message = "Please install ARCore"
        } else if (sessionException is UnavailableApkTooOldException) {
            message = "Please update ARCore"
        } else if (sessionException is UnavailableSdkTooOldException) {
            message = "Please update this app"
        } else if (sessionException is UnavailableDeviceNotCompatibleException) {
            message = "This device does not support AR"
        } else {
            message = "Failed to create AR session"
            Log.e(TAG, "Exception: $sessionException")
        }
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    fun checkIsSupportedDeviceOrFinish(activity: android.support.v7.app.AppCompatActivity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later")
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }

    @Throws(UnavailableException::class)
    fun createArSession(activity: android.support.v7.app.AppCompatActivity, installRequested: Boolean): Session? {
        var session: Session? = null
        // if we have the camera permission, create the session
        if (hasCameraPermission(activity)) {
            when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> return null
                ArCoreApk.InstallStatus.INSTALLED -> {
                }
            }
            session = Session(activity)
            // IMPORTANT!!!  ArSceneView requires the `LATEST_CAMERA_IMAGE` non-blocking update mode.
            val config = Config(session)
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            session.configure(config)
        }
        return session
    }


    /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
    fun requestCameraPermission(activity: android.support.v7.app.AppCompatActivity, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.CAMERA), requestCode
        )
    }
    fun requestLocationPermission(activity: android.support.v7.app.AppCompatActivity, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION), requestCode
        )
    }
    /** Check to see we have the necessary permissions for this app.  */
    fun hasLoPermission(activity: android.support.v7.app.AppCompatActivity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    /** Check to see we have the necessary permissions for this app.  */
    fun hasCameraPermission(activity: android.support.v7.app.AppCompatActivity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Check to see if we need to show the rationale for this permission.  */
    fun shouldShowRequestPermissionRationale(activity: android.support.v7.app.AppCompatActivity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.CAMERA
        )
    }

    /** Launch Application Setting to grant permission.  */
    fun launchPermissionSettings(activity: android.support.v7.app.AppCompatActivity) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", activity.packageName, null)
        activity.startActivity(intent)
    }

}