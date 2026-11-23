package com.zulfa.hijrah.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.zulfa.hijrah.MainActivity
import com.zulfa.hijrah.R
import com.zulfa.hijrah.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvGoLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirm.text.toString()

        if (!validate(name, email, password, confirm)) return

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Simpan nama ke profil pengguna
                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    task.result.user?.updateProfile(profile)?.addOnCompleteListener {
                        setLoading(false)
                        Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finishAffinity() // tutup semua layar login/registrasi
                    }
                } else {
                    setLoading(false)
                    Toast.makeText(this, errorMessage(task.exception), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validate(name: String, email: String, pass: String, confirm: String): Boolean {
        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirm.error = null
        var valid = true

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.err_name_empty)
            valid = false
        }
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.err_email_empty)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.err_email_invalid)
            valid = false
        }
        if (pass.length < 6) { // Firebase mewajibkan minimal 6 karakter
            binding.tilPassword.error = getString(R.string.err_password_short)
            valid = false
        }
        if (confirm != pass) {
            binding.tilConfirm.error = getString(R.string.err_password_mismatch)
            valid = false
        }
        return valid
    }

    private fun errorMessage(e: Exception?): String {
        android.util.Log.e("HijrahAuth", "Auth gagal", e)
        return when (e) {
            is FirebaseNetworkException -> getString(R.string.err_network)
            is FirebaseAuthUserCollisionException -> getString(R.string.err_email_taken)
            else -> getString(R.string.err_unknown)
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }
}