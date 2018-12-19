package com.machichiotte.pocqrcodedecathlon

class Constants {

    companion object Constants {
        val BASE_URL = "http://api.easylog.w-dev.fr/api/"

        val USER_TOKEN = "USER_TOKEN"
        val USER_BASE_URL = "USER_BASE_URL"

        val REQUEST_CODE_QR_SCAN = 101

        val REQUEST_PHONE_STATE = 10
        val REQUEST_READ_CONTACTS = 20
        val REQUEST_CAMERA_ACCESS = 30

        val PREFS_ID = "PREFS_ID"

        val BASE_TIMER = 1500
        val BASE_URL_MANUAL = "BASE_URL_MANUAL"

        val ERROR_SAME_ZONE = "TRACKING NUMBER HAS BEEN ALREADY SCAN IN THE SAME ZONE"
        val ERROR_MISSING_ZONE = "ZONE HAS NOT BEEN SELECTED"
        val ERROR_MISSING_TRACKING = "TRACKING NUMBER DON'T EXIST"
        val ERROR_EXPIRED_TOKEN = "TOKEN ERROR"

        val SUCCESS = "SUCCESS"
    }
}