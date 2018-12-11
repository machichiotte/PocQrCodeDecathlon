package com.machichiotte.pocqrcodedecathlon

import com.machichiotte.pocqrcodedecathlon.pojo.Auth
import retrofit2.Call
import retrofit2.http.*

interface InterfaceApi {
    @GET("auth/{uniqueId}")
    fun getUniqueIdAuth(@Path("uniqueId", encoded = true) uniqueId: String): Call<Auth>

    @GET("auth/{login}/{password}")
    fun getLoginAuth(@Path("login", encoded = true) login: String, @Path("password", encoded = true) password: String): Call<Auth>

    @GET("scanqr/{qrcode}")
    fun getQrCodeStatus(@Path("qrcode", encoded = true) qrcode: String): Call<Auth>

}
