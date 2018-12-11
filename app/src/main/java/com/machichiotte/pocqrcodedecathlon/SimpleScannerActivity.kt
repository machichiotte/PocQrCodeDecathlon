package com.machichiotte.pocqrcodedecathlon

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.zxing.Result
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.BASE_URL
import com.machichiotte.pocqrcodedecathlon.pojo.Auth
import me.dm7.barcodescanner.zxing.ZXingScannerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SimpleScannerActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private var mScannerView: ZXingScannerView? = null

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        mScannerView = ZXingScannerView(this@SimpleScannerActivity)
        setContentView(mScannerView)

        mScannerView!!.startCamera()
        mScannerView!!.setResultHandler(this@SimpleScannerActivity)
    }

    public override fun onPause() {
        super.onPause()
        mScannerView!!.stopCamera()
    }

    override fun handleResult(rawResult: Result) {
        val qrCode = (rawResult.text.substring(rawResult.text.lastIndexOf("scanqr/") + 7)).trim()
        sendQrCode(qrCode)

        // If you would like to resume scanning, call this method below:
        Handler().postDelayed({ mScannerView!!.resumeCameraPreview(this) }, 700)
    }

    private fun sendQrCode(qrCode: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(InterfaceApi::class.java)

        val call: Call<Auth> = service.getQrCodeStatus(qrCode)

        call.enqueue(object : Callback<Auth> {
            override fun onResponse(call: Call<Auth>, response: Response<Auth>) {
                response.body()?.let {
                    if (it.status == "SUCCESS") {
                        playNotif(true)
                        Toast.makeText(this@SimpleScannerActivity, "SUCCESS:$qrCode", Toast.LENGTH_LONG).show()
                    } else {
                        playNotif(false)
                        Toast.makeText(this@SimpleScannerActivity, "ERROR:$it.error_message", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<Auth>, t: Throwable) {
                Toast.makeText(
                    this@SimpleScannerActivity,
                    "Something went wrong...Please try later!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun playNotif(isSuccess: Boolean) = try {
        var mPlayer : MediaPlayer
        if (isSuccess)
            mPlayer = MediaPlayer.create(this@SimpleScannerActivity, R.raw.plucky)
        else
            mPlayer = MediaPlayer.create(this@SimpleScannerActivity, R.raw.error)

        mPlayer.start()

    } catch (e: Exception) {
        e.printStackTrace()
    }
}