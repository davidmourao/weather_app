package br.com.igti.weatherapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentProviderClient
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import br.com.igti.weatherapp.models.WeatherResponse
import br.com.igti.weatherapp.utils.Constants
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val REQUEST_LOCATION_CODE = 123456
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (!isLocationEnabled()) {
            Toast.makeText(
                this@MainActivity,
                "The location is not Enabled",
                Toast.LENGTH_LONG
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_CODE && grantResults.isNotEmpty()) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
            requestLocationData()

        } else {
            Toast.makeText(this, "The permission was not granted", Toast.LENGTH_LONG).show()

        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).build()

        mFusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {

                getLocationWeatherDetails(
                    locationResult.lastLocation?.latitude!!,
                    locationResult.lastLocation?.longitude!!
                )

            }
        }, Looper.myLooper())
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val serviceApi = retrofit.create(WeatherServiceApi::class.java)

            val call = serviceApi.getWeatherDetails(
                latitude, longitude,
                Constants.APP_ID,
                Constants.METRIC_UNIT
            )

            call.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>?,
                    response: Response<WeatherResponse>?
                ) = if (response?.isSuccessful == true) {
                    val weather = response?.body()
                    Log.d("WEATHER", weather.toString())
                    //Toast.makeText(this@MainActivity,"{weather}",Toast.LENGTH_LONG).show()

                    for (i in weather!!.weather.indices) {
                        findViewById<TextView>(R.id.text_view_sunset).text =
                            convertTime(weather.sys.sunset.toLong())
                        findViewById<TextView>(R.id.text_view_sunrise).text =
                            convertTime(weather.sys.sunrise.toLong())
                        findViewById<TextView>(R.id.text_view_status).text =
                            weather.weather!![i].description
                        findViewById<TextView>(R.id.text_view_address).text = weather.name
                        findViewById<TextView>(R.id.text_view_address).text = weather.name
                        findViewById<TextView>(R.id.text_view_temp_max).text =
                            weather.main.temp_max.toString() + " max"
                        findViewById<TextView>(R.id.text_view_temp_min).text =
                            weather.main.temp_max.toString() + " min"
                        findViewById<TextView>(R.id.text_view_temp).text =
                            weather.main.temp.toString() + "°C"
                        findViewById<TextView>(R.id.text_view_humidity).text =
                            weather.main.humidity.toString()
                        findViewById<TextView>(R.id.text_view_pressure).text =
                            weather.main.pressure.toString()
                        findViewById<TextView>(R.id.text_view_wind).text =
                            weather.wind.speed.toString()
                    }


                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "something is wrong", Toast.LENGTH_LONG
                    ).show()
                }

                override fun onFailure(call: Call<WeatherResponse>?, t: Throwable?) {
                    TODO("Not yet implemented")
                }
            })

        } else {
            Toast.makeText(this, "There's no internet connection ", Toast.LENGTH_LONG).show()
        }
    }

    private fun convertTime(time: Long): String {
        val date = Date(time * 1000L)
        val timeFormatted = SimpleDateFormat("HH:mm", Locale.JAPAN)
        timeFormatted.timeZone = TimeZone.getDefault()
        return timeFormatted.format(date)

    }


    private fun isLocationEnabled(): Boolean {

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showRequestDialog()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            showRequestDialog()//remember that this was changed!
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_CODE
            )
        }
    }

    private fun showRequestDialog() {
        AlertDialog.Builder(this)
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("CLOSE") { dialog, _ ->
                dialog.cancel()
            }.setTitle("Location permission needed")
            .setMessage("This permission is needed for accessing the location.It can enabled under the Application Settings.")
            .show()

    }
}