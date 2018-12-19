package com.machichiotte.pocqrcodedecathlon

import com.machichiotte.pocqrcodedecathlon.pojo.ApiJerem
import retrofit2.Call
import retrofit2.http.*

interface InterfaceApi {
    @GET("auth/{uniqueId}")
    fun getUniqueIdAuth(@Path("uniqueId", encoded = true) uniqueId: String?): Call<ApiJerem>

    @GET("auth/{login}/{password}")
    fun getLoginAuth(@Path("login", encoded = true) login: String, @Path("password", encoded = true) password: String): Call<ApiJerem>

    @GET("scanqr/{token}/{qrcode}")
    fun getQrCodeStatus(@Path("token", encoded = true) token: String, @Path("qrcode", encoded = true) qrcode: String): Call<ApiJerem>

}
