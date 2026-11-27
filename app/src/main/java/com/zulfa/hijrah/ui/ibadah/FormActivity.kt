package com.zulfa.hijrah.ui.ibadah

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zulfa.hijrah.R
import com.zulfa.hijrah.data.IbadahOptions
import com.zulfa.hijrah.databinding.ActivityFormBinding
import java.util.Date

class FormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_JENIS = "extra_jenis"
        const val EXTRA_TANGGAL = "extra_tanggal"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_CATATAN = "extra_catatan"
    }

    private lateinit var binding: ActivityFormBinding
    private var selectedMillis = MaterialDatePicker.todayInUtcMilliseconds()
    private var editId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actJenis.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, IbadahOptions.JENIS)
        )
        binding.actStatus.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, IbadahOptions.STATUS)
        )

        editId = intent.getStringExtra(EXTRA_ID)
        if (editId != null) {
            // Mode ubah: isi form dengan data yang ada
            binding.tvFormTitle.setText(R.string.form_edit_title)
            binding.actJenis.setText(intent.getStringExtra(EXTRA_JENIS), false)
            binding.actStatus.setText(intent.getStringExtra(EXTRA_STATUS), false)
            binding.etCatatan.setText(intent.getStringExtra(EXTRA_CATATAN))
            selectedMillis = intent.getLongExtra(EXTRA_TANGGAL, selectedMillis)
        } else {
            binding.actStatus.setText(IbadahOptions.STATUS[0], false)
        }
        binding.etTanggal.setText(IbadahOptions.formatTanggal(selectedMillis))

        binding.etTanggal.setOnClickListener { showDatePicker() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.pick_date)
            .setSelection(selectedMillis)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            selectedMillis = millis
            binding.etTanggal.setText(IbadahOptions.formatTanggal(millis))
        }
        picker.show(supportFragmentManager, "date")
    }

    private fun validate(jenis: String, status: String): Boolean {
        binding.tilJenis.error = null
        binding.tilStatus.error = null
        var valid = true
        if (jenis.isEmpty()) {
            binding.tilJenis.error = getString(R.string.err_jenis_empty)
            valid = false
        }
        if (status.isEmpty()) {
            binding.tilStatus.error = getString(R.string.err_status_empty)
            valid = false
        }
        return valid
    }

    private fun save() {
        val jenis = binding.actJenis.text.toString().trim()
        val status = binding.actStatus.text.toString().trim()
        val catatan = binding.etCatatan.text.toString().trim()

        if (!validate(jenis, status)) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val data = hashMapOf(
            "jenis" to jenis,
            "tanggal" to Timestamp(Date(selectedMillis)),
            "status" to status,
            "catatan" to catatan
        )
        val collection = FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("ibadah")

        setLoading(true)
        val id = editId
        if (id == null) {
            collection.add(data)
                .addOnSuccessListener { onSaved(R.string.save_success) }
                .addOnFailureListener { onSaveFailed() }
        } else {
            collection.document(id).set(data)
                .addOnSuccessListener { onSaved(R.string.update_success) }
                .addOnFailureListener { onSaveFailed() }
        }
    }

    private fun onSaved(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun onSaveFailed() {
        setLoading(false)
        Toast.makeText(this, R.string.err_save, Toast.LENGTH_LONG).show()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
    }
}