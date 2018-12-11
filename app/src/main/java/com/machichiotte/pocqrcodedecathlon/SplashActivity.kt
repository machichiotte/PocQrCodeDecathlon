package com.machichiotte.pocqrcodedecathlon

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.BASE_URL
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.REQUEST_CODE_QR_SCAN
import com.machichiotte.pocqrcodedecathlon.pojo.Auth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        checkMacAddress()
    }

    private fun checkMacAddress() {
        val uniqueID = UUID.randomUUID().toString()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(InterfaceApi::class.java)

        val call: Call<Auth> = service.getUniqueIdAuth(uniqueID)

        call.enqueue(object : Callback<Auth> {
            override fun onResponse(call: Call<Auth>, response: Response<Auth>) {
                // generateAuth(response.body().getEmployeeArrayList())
                Handler().postDelayed({
                response.body()?.let {
                    if (it.status == "SUCCESS") {
                        val i = Intent(this@SplashActivity, SimpleScannerActivity::class.java)
                        startActivityForResult(i, REQUEST_CODE_QR_SCAN)
                        finish()
                    } else {
                        val i = Intent(this@SplashActivity, LoginActivity::class.java)
                        startActivity(i)
                        finish()
                    }
                }}, 1500)
            }

            override fun onFailure(call: Call<Auth>, t: Throwable) {
                Toast.makeText(this@SplashActivity, "Something went wrong...Please try later!", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }
}