package com.zulfa.hijrah.data

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Bentuk data sesuai JSON dari Aladhan API
data class PrayerResponse(val data: PrayerData?)

data class PrayerData(val timings: Timings?, val date: DateInfo?)

data class Timings(
    @SerializedName("Fajr") val fajr: String?,
    @SerializedName("Dhuhr") val dhuhr: String?,
    @SerializedName("Asr") val asr: String?,
    @SerializedName("Maghrib") val maghrib: String?,
    @SerializedName("Isha") val isha: String?
)

data class DateInfo(val readable: String?, val hijri: HijriInfo?)

data class HijriInfo(val day: String?, val month: HijriMonth?, val year: String?)

data class HijriMonth(val en: String?)

interface PrayerApi {
    @GET("timingsByCity")
    fun getTimings(
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int
    ): Call<PrayerResponse>
}

object ApiClient {
    val prayerApi: PrayerApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PrayerApi::class.java)
    }
}