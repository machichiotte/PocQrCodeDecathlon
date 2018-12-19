package com.machichiotte.pocqrcodedecathlon

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.Result
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.BASE_TIMER
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.BASE_URL
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.ERROR_EXPIRED_TOKEN
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.PREFS_ID
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.SUCCESS
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.USER_BASE_URL
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.USER_TOKEN
import com.machichiotte.pocqrcodedecathlon.pojo.ApiJerem
import kotlinx.android.synthetic.main.activity_scan.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class SimpleScannerActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private var mScannerView: ZXingScannerView? = null
    private lateinit var toolbar: Toolbar
    private lateinit var token: String
    private var timing: Long = BASE_TIMER.toLong()

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_scan)

        token = getSharedPreferences(PREFS_ID, MODE_PRIVATE).getString(USER_TOKEN, "")

        mScannerView = findViewById(R.id.scanner)
        toolbar = findViewById(R.id.toolbar)

        prepareToolbar(-1)
    }

    private fun prepareToolbar(zone: Int) {
        if (zone > 0) {
            toolbar.title = "Zone $zone"
            toolbar.subtitle = "Description de la zone"
        } else {
            toolbar.title = "Zone inconnue"
            toolbar.subtitle = "Veuillez scanner une zone"
        }

        setSupportActionBar(toolbar)
    }

    private fun prepareScanner() {

        mScannerView?.let {

            it.startCamera()
            it.setResultHandler(this@SimpleScannerActivity)

            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                var isFlashOn = false

                val flashButton: FloatingActionButton = findViewById(R.id.fab_flash)
                flashButton.visibility = View.VISIBLE

                flashButton.setOnClickListener {
                    mScannerView!!.flash = !isFlashOn
                    isFlashOn = !isFlashOn

                    if (isFlashOn)
                        fab_flash.setImageResource(R.drawable.ic_flash_off_black_24dp)
                    else
                        fab_flash.setImageResource(R.drawable.ic_flash_on_black_24dp)

                }

            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_action_bar_scan, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        when (item.itemId) {
            R.id.action_timer -> {
                openTimerSetting()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun openTimerSetting() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.custom_dialog_seekbar, null)
        dialogBuilder.setView(dialogView)

        val tvTiming = dialogView.findViewById(R.id.tv_timing) as TextView
        val seek = dialogView.findViewById(R.id.seekbar) as SeekBar

        seek.progress = timing.toInt()

        tvTiming.text = timing.toString()

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTiming.text = (progress).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })


        dialogBuilder.setTitle(getString(R.string.modify_scan_timing_title))
        dialogBuilder.setPositiveButton(getString(R.string.validate)) { dialog, whichButton ->
            timing = tvTiming.text.toString().toLong()

        }
        dialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, whichButton ->

        }
        val b = dialogBuilder.create()
        b.show()

    }

    public override fun onPause() {
        super.onPause()
        mScannerView!!.stopCamera()
    }

    public override fun onResume() {
        super.onResume()
        prepareScanner()
    }

    override fun handleResult(rawResult: Result) {
        val qrCode = (rawResult.text.substring(rawResult.text.lastIndexOf("/") + 1)).trim()
        sendQrCode(qrCode)
    }

    private fun sendQrCode(qrCode: String) {

        token.let {
            val retrofit = Retrofit.Builder()
                .baseUrl(checkBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(InterfaceApi::class.java)

            val call: Call<ApiJerem> = service.getQrCodeStatus(token, qrCode)

            call.enqueue(object : Callback<ApiJerem> {
                override fun onResponse(call: Call<ApiJerem>, response: Response<ApiJerem>) {
                    response.body()?.let { it ->
                        if (it.status == SUCCESS) {
                            restartCamera()
                            playNotif(true)
                            showSnackBar("SUCCESS:$qrCode", true)
                        } else {
                            when (it.error_message) {
                                ERROR_EXPIRED_TOKEN -> {
                                    val i = Intent(this@SimpleScannerActivity, SplashActivity::class.java)
                                    startActivity(i)
                                    finish()
                                }
                                else -> {
                                    restartCamera()
                                    playNotif(false)
                                    showSnackBar("ERROR:$it.error_messagee", false)
                                }

                            }

                        }
                    }
                }

                override fun onFailure(call: Call<ApiJerem>, t: Throwable) {
                    Toast.makeText(
                        this@SimpleScannerActivity,
                        "Something went wrong...Please try later!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    private fun showSnackBar(content: String, isSuccess: Boolean) {


        val snackBar = Snackbar.make(
            activity_scan_layout, // Parent view
            content, // Message to show
            Snackbar.LENGTH_LONG // How long to display the message.
        )

        // change snackbar text color
        val snackbarTextId = android.support.design.R.id.snackbar_text
        val textView = snackBar.view.findViewById(snackbarTextId) as TextView

        if (isSuccess)
            textView.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_light)
            )
        else
            textView.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_red_light)
            )

        snackBar.show()
    }

    private fun restartCamera() {
        Handler().postDelayed({ mScannerView!!.resumeCameraPreview(this) }, timing)
    }

    private fun playNotif(isSuccess: Boolean) = try {
        val mPlayer: MediaPlayer
        if (isSuccess)
            mPlayer = MediaPlayer.create(this@SimpleScannerActivity, R.raw.plucky)
        else
            mPlayer = MediaPlayer.create(this@SimpleScannerActivity, R.raw.error)

        mPlayer.start()

    } catch (e: Exception) {
        e.printStackTrace()
    }

    private fun checkBaseUrl(): String {
        var baseUrl = BASE_URL

        getSharedPreferences(PREFS_ID, MODE_PRIVATE).getString(USER_BASE_URL, null)?.let {
            baseUrl = it
        }

        return baseUrl
    }
}