package com.zulfa.hijrah.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class IbadahRecord(
    @DocumentId val id: String = "",
    val jenis: String = "",
    val tanggal: Timestamp = Timestamp.now(),
    val status: String = "",
    val catatan: String = ""
)