package com.machichiotte.pocqrcodedecathlon.pojo

import android.support.annotation.Nullable
import com.google.gson.annotations.SerializedName

data class ApiJerem(
    @SerializedName("status")
    val status: String,
    @Nullable
    @SerializedName("error_num")
    val error_num: Int,
    @Nullable
    @SerializedName("error_message")
    val error_message: String,
    @SerializedName("method")
    val method: String,
    @Nullable
    @SerializedName("success_message")
    val success_message: String,
    @Nullable
    @SerializedName("token")
    val token: String
)