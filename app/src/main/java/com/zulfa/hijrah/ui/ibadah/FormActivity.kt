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

    private lateinit var binding: ActivityFormBinding
    private var selectedMillis = MaterialDatePicker.todayInUtcMilliseconds()

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
        binding.actStatus.setText(IbadahOptions.STATUS[0], false)
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

        setLoading(true)
        FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("ibadah")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, R.string.err_save, Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
    }
}