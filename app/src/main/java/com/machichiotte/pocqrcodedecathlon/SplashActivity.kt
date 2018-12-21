package com.machichiotte.pocqrcodedecathlon

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.telephony.TelephonyManager
import android.widget.Toast
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.BASE_URL
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.PREFS_ID
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.REQUEST_CODE_QR_SCAN
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.REQUEST_PHONE_STATE
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.SUCCESS
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.USER_BASE_URL
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.USER_TOKEN
import com.machichiotte.pocqrcodedecathlon.pojo.ApiJerem
import kotlinx.android.synthetic.main.activity_splash.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SplashActivity : AppCompatActivity() {

    private var deviceId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        checkBaseUrl()
        getDeviceId(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PHONE_STATE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getDeviceId(this)
                } else {
                    goToLogin()

                }
                return
            }
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission()
        } else {
            deviceId = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).deviceId

            if (deviceId == "")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    deviceId = Build.getSerial()
                } else {
                    deviceId = Build.SERIAL
                }

            checkMacAddress()
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_PHONE_STATE),
                REQUEST_PHONE_STATE
            )
        }
    }


    private fun checkMacAddress() {
        if (checkConnectivity()) {
            val uniqueID = deviceId

            val retrofit = Retrofit.Builder()
                .baseUrl(checkBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(InterfaceApi::class.java)

            val call: Call<ApiJerem> = service.getUniqueIdAuth(uniqueID)

            call.enqueue(object : Callback<ApiJerem> {
                override fun onResponse(call: Call<ApiJerem>, response: Response<ApiJerem>) {
                    // generateAuth(response.body().getEmployeeArrayList())
                    Handler().postDelayed({
                        response.body()?.let {
                            if (it.status == SUCCESS) {

                                saveToken(it.token)

                                val i = Intent(this@SplashActivity, SimpleScannerActivity::class.java)
                                startActivityForResult(i, REQUEST_CODE_QR_SCAN)
                                finish()
                            } else {
                                goToLogin()
                            }
                        }
                    }, 1500)
                }

                override fun onFailure(call: Call<ApiJerem>, t: Throwable) {
                    Toast.makeText(this@SplashActivity, "Something went wrong...Please try later!", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        } else {
            Utils.showSnackBar("ERROR:NO CONNECTIVITY", false, activity_splash_layout)
        }
    }

    private fun saveToken(token: String?) {
        token?.let {
            val sharedPref = this.getSharedPreferences(PREFS_ID, MODE_PRIVATE) ?: return
            with(sharedPref.edit()) {
                putString(USER_TOKEN, it)
                apply()
            }
        }
    }

    private fun checkBaseUrl(): String {
        var baseUrl = BASE_URL

        getSharedPreferences(PREFS_ID, MODE_PRIVATE).getString(USER_BASE_URL, null)?.let {
            baseUrl = it
        }

        return baseUrl
    }

    private fun goToLogin() {
        val i = Intent(this@SplashActivity, LoginActivity::class.java)
        startActivity(i)
        finish()
    }


    private fun checkConnectivity(): Boolean {
        return Utils.isInternetconnected(this)
    }
}