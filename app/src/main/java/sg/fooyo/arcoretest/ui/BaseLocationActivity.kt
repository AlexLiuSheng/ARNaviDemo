package sg.fooyo.arcoretest.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import org.jetbrains.anko.toast
import sg.fooyo.arcoretest.R

open class BaseLocationActivity : AppCompatActivity(), LocationListener {
    private lateinit var gpsDialog: AlertDialog

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }

    override fun onLocationChanged(location: Location?) {
        currentLocation = location
        onLocationGot(currentLocation)
        removeLocationListener()
    }

    private lateinit var locationManager: LocationManager
    protected var hasLocationPermission = false
    protected var currentLocation: Location? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.please_open_gps))
        builder.setPositiveButton(
            getString(R.string.confirm)
        ) { _, _ ->
            // 转到手机设置界面，用户设置GPS
            val intent = Intent(
                Settings.ACTION_LOCATION_SOURCE_SETTINGS
            )
            startActivityForResult(intent, 0) // 设置完成后返回到原来的界面
        }
        builder.setCancelable(true)
        builder.setOnCancelListener {
        }
        gpsDialog = builder.create()
    }

    @SuppressLint("CheckResult")
    protected fun requestLocationPermission() {
        RxPermissions(this)
            .request(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ).subscribe {
                if (it) {
                    hasLocationPermission = true
                    initLocation()

                } else {
                    finish()
                }
            }
    }

    open fun onLocationGot(location: Location?) {
        toast("location got:${location?.latitude},${location?.longitude}")

    }

    @SuppressLint("MissingPermission")
    private fun initLocation() {
        if (verifyLocationProviderEnabled() && hasLocationPermission) {
            toast("requesting location")
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (currentLocation == null)
                currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (currentLocation != null) {
                onLocationGot(currentLocation)
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1f, this)
        }


    }

    override fun onPause() {
        super.onPause()
        removeLocationListener()
    }

    override fun onResume() {
        super.onResume()
        initLocation()

    }

    private fun removeLocationListener() {

        locationManager.removeUpdates(this)

    }

    private fun verifyLocationProviderEnabled(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (!locationManager
                .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) && !locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            //toast("please open GPS");

            gpsDialog.show()
            false
        } else {
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            initLocation()
        }
    }


}