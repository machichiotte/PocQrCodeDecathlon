package com.machichiotte.pocqrcodedecathlon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkChangeReceiver extends BroadcastReceiver {


    public static final String NETWORK_SWITCH_FILTER = "com.devglan.broadcastreceiver.NETWORK_SWITCH_FILTER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase("android.net.conn.CONNECTIVITY_CHANGE")) {

            NetworkInfo networkInfo =
                    intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                Intent intnt = new Intent(NETWORK_SWITCH_FILTER);
                intnt.putExtra("is_connected", true);
                context.sendBroadcast(intnt);
            } else {

                Intent intnt = new Intent(NETWORK_SWITCH_FILTER);
                intnt.putExtra("is_connected", false);
                context.sendBroadcast(intnt);
            }


        }
    }

}