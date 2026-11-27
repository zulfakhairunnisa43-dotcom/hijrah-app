package com.zulfa.hijrah.data

import com.zulfa.hijrah.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object IbadahOptions {
    val JENIS = listOf(
        "Salat Subuh", "Salat Zuhur", "Salat Asar", "Salat Magrib", "Salat Isya",
        "Salat Dhuha", "Salat Tahajud", "Puasa Senin-Kamis", "Puasa Ayyamul Bidh",
        "Tilawah Alquran", "Dzikir", "Sedekah"
    )
    val STATUS = listOf("Selesai", "Terlewat", "Qadha")

    fun statusColorRes(status: String): Int = when (status) {
        "Selesai" -> R.color.green_primary
        "Terlewat" -> R.color.error_red
        else -> R.color.gold
    }

    // Date picker menghasilkan tengah malam UTC, jadi tanggal juga diformat dengan UTC
    // supaya tanggal yang dipilih tidak bergeser sehari di zona waktu lain.
    fun formatTanggal(millis: Long): String {
        val format = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(millis)
    }
}