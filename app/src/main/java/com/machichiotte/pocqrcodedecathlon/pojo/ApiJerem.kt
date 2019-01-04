package com.machichiotte.pocqrcodedecathlon.pojo

import android.support.annotation.Nullable
import com.google.gson.annotations.SerializedName

data class ApiJerem(
    @SerializedName("status")
    val status: String,
    @Nullable
    @SerializedName("error_num")
    val error_num: Int?,
    @Nullable
    @SerializedName("error_message")
    val error_message: String?,
    @SerializedName("method")
    val method: String?,
    @Nullable
    @SerializedName("success_message")
    val success_message: String?,
    @Nullable
    @SerializedName("token")
    val token: String?,
    @Nullable
    @SerializedName("quantity")
    val quantity: Int?,
    @Nullable
    @SerializedName("number_zone")
    val number_zone: Int?,
    @Nullable
    @SerializedName("description_zone")
    val description_zone: String?,
    @Nullable
    @SerializedName("type")
    val type: String?,
    @Nullable
    @SerializedName("option1")
    val option1: Boolean?,
    @Nullable
    @SerializedName("option2")
    val option2: Boolean?
)