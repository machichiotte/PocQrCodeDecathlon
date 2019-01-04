package com.machichiotte.pocqrcodedecathlon

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.FloatingActionButton
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
import com.machichiotte.pocqrcodedecathlon.NetworkChangeReceiver.NETWORK_SWITCH_FILTER
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
    private var successMsg: String? = null
    private lateinit var listId: ArrayList<String>

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_scan)

        listId = ArrayList()

        token = getSharedPreferences(PREFS_ID, MODE_PRIVATE).getString(USER_TOKEN, "")

        mScannerView = findViewById(R.id.scanner)
        toolbar = findViewById(R.id.toolbar)

        prepareToolbar(-1, null)
    }

    private fun prepareToolbar(zone: Int, description: String?) {
        if (zone > 0) {
            toolbar.title = getString(R.string.zone_space) + zone
            toolbar.subtitle = description
        } else {
            toolbar.title = getString(R.string.unknown_zone)
            toolbar.subtitle = getString(R.string.unknown_zone_subtitle)
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
        return when (item.itemId) {
            R.id.action_timer -> {
                openTimerSetting()
                true
            }
            else -> super.onOptionsItemSelected(item)
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

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        dialogBuilder.setTitle(getString(R.string.modify_scan_timing_title))
        dialogBuilder.setPositiveButton(getString(R.string.validate)) { _, _ ->
            timing = tvTiming.text.toString().toLong()

        }
        dialogBuilder.setNegativeButton(getString(R.string.cancel)) { _, _ ->

        }
        val b = dialogBuilder.create()
        b.show()

    }

    private var netSwitchReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val isConnectionAvailable = intent.extras.getBoolean("is_connected")

            if (isConnectionAvailable) {

                if (listId.size > 0) {
                    for (id: String in listId) {
                        sendQrCode(id)
                        listId.remove(id)
                    }
                }

                Utils.showSnackBar("SUCCESS: CONNECTIVITY", true, activity_scan_layout)
                Toast.makeText(
                    this@SimpleScannerActivity,
                    "Yep",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Utils.showSnackBar("ERROR:NO CONNECTIVITY", false, activity_scan_layout)
                Toast.makeText(
                    this@SimpleScannerActivity,
                    "NOPE!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(netSwitchReceiver)
        mScannerView!!.stopCamera()
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(netSwitchReceiver, IntentFilter(NETWORK_SWITCH_FILTER))
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
                            Utils.showSnackBar("SUCCESS:$qrCode", true, activity_scan_layout)

                            if (null != it.number_zone) {
                                prepareToolbar(it.number_zone, it.description_zone)
                            }

                            if (it.success_message != null && (successMsg == null || successMsg != it.success_message))
                                if (null != it.quantity && it.quantity > 1) {
                                    successMsg = it.success_message
//add module
                                    showDialogColis(it.quantity)
                                }

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
                                    Utils.showSnackBar("ERROR:" + it.error_message, false, activity_scan_layout)
                                }

                            }

                        }
                    }
                }

                override fun onFailure(call: Call<ApiJerem>, t: Throwable) {
                    listId.add(qrCode)

                    Toast.makeText(
                        this@SimpleScannerActivity,
                        "Problème de connexion, votre QrCode sera renvoyé plus tard !",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    private fun showDialogColis(nbColis: Int) {
        AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.colis_dialog_title, nbColis))
            .setMessage(getString(R.string.scan_again_for_confirmation))
            .setPositiveButton(getString(R.string.close)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }


    private fun restartCamera() {
        Handler().postDelayed({ mScannerView!!.resumeCameraPreview(this) }, timing)
    }

    private fun playNotif(isSuccess: Boolean) = try {
        val mPlayer: MediaPlayer = if (isSuccess)
            MediaPlayer.create(this@SimpleScannerActivity, R.raw.plucky)
        else
            MediaPlayer.create(this@SimpleScannerActivity, R.raw.error)

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