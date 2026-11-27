package com.zulfa.hijrah.ui.ibadah

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zulfa.hijrah.R
import com.zulfa.hijrah.data.IbadahOptions
import com.zulfa.hijrah.data.IbadahRecord
import com.zulfa.hijrah.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
    }

    private lateinit var binding: ActivityDetailBinding
    private var docRef: DocumentReference? = null
    private var listener: ListenerRegistration? = null
    private var current: IbadahRecord? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getStringExtra(EXTRA_ID)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (id == null || uid == null) {
            finish()
            return
        }

        docRef = FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("ibadah").document(id)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnEdit.setOnClickListener { openEdit() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    // Data dimuat ulang setiap layar terlihat, jadi hasil edit langsung tampil
    override fun onStart() {
        super.onStart()
        listener = docRef?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(this, R.string.err_load, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            val record = snapshot?.toObject(IbadahRecord::class.java)?.copy(id = snapshot.id)
            if (record == null) { // dokumen sudah dihapus
                finish()
                return@addSnapshotListener
            }
            current = record
            bind(record)
        }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
    }

    private fun bind(record: IbadahRecord) {
        binding.tvJenis.text = record.jenis
        binding.tvStatus.text = record.status
        binding.tvStatus.setTextColor(
            ContextCompat.getColor(this, IbadahOptions.statusColorRes(record.status))
        )
        binding.tvTanggal.text = IbadahOptions.formatTanggal(record.tanggal.toDate().time)
        binding.tvCatatan.text =
            record.catatan.ifBlank { getString(R.string.no_note) }
    }

    private fun openEdit() {
        val record = current ?: return
        startActivity(
            Intent(this, FormActivity::class.java).apply {
                putExtra(FormActivity.EXTRA_ID, record.id)
                putExtra(FormActivity.EXTRA_JENIS, record.jenis)
                putExtra(FormActivity.EXTRA_TANGGAL, record.tanggal.toDate().time)
                putExtra(FormActivity.EXTRA_STATUS, record.status)
                putExtra(FormActivity.EXTRA_CATATAN, record.catatan)
            }
        )
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_message)
            .setPositiveButton(R.string.btn_delete) { _, _ -> deleteRecord() }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun deleteRecord() {
        docRef?.delete()
            ?.addOnSuccessListener {
                Toast.makeText(this, R.string.delete_success, Toast.LENGTH_SHORT).show()
                finish()
            }
            ?.addOnFailureListener {
                Toast.makeText(this, R.string.err_delete, Toast.LENGTH_LONG).show()
            }
    }
}