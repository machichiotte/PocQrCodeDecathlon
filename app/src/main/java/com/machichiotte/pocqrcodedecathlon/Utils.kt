package com.machichiotte.pocqrcodedecathlon

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.TextView

class Utils {

    companion object {

        fun showSnackBar(content: String, isSuccess: Boolean, view: View) {
            val snackBar = Snackbar.make(
                view, // Parent view
                content, // Message to show
                Snackbar.LENGTH_LONG // How long to display the message.
            )

            // change snackbar text color
            val snackbarTextId = android.support.design.R.id.snackbar_text
            val textView = snackBar.view.findViewById(snackbarTextId) as TextView

            if (isSuccess)
                textView.setTextColor(
                    ContextCompat.getColor(view.context, android.R.color.holo_green_light)
                )
            else
                textView.setTextColor(
                    ContextCompat.getColor(view.context, android.R.color.holo_red_light)
                )

            snackBar.show()
        }

        fun isInternetconnected(ct: Context): Boolean {
            val connected: Boolean
            //get the connectivity manager object to identify the network state.
            val connectivityManager = ct.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            //Check if the manager object is NULL, this check is required. to prevent crashes in few devices.
            if (connectivityManager != null) {
                //Check Mobile data or Wifi net is present

                connected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).state ==
                        NetworkInfo.State.CONNECTED || connectivityManager.getNetworkInfo(
                    ConnectivityManager.TYPE_WIFI
                ).state == NetworkInfo.State.CONNECTED
                return connected
            } else {
                return false
            }
        }
    }
}