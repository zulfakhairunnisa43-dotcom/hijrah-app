package com.zulfa.hijrah.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.zulfa.hijrah.MainActivity
import com.zulfa.hijrah.R
import com.zulfa.hijrah.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase menyimpan sesi login. Kalau currentUser tidak null,
        // pengguna sudah pernah login, jadi langsung ke layar utama.
        if (auth.currentUser != null) {
            goToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (!validate(email, password)) return

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    goToMain()
                } else {
                    Toast.makeText(this, errorMessage(task.exception), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validate(email: String, password: String): Boolean {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        var valid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.err_email_empty)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.err_email_invalid)
            valid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.err_password_empty)
            valid = false
        }
        return valid
    }

    private fun errorMessage(e: Exception?): String = when (e) {
        is FirebaseNetworkException -> getString(R.string.err_network)
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> getString(R.string.err_wrong_credentials)
        else -> getString(R.string.err_unknown)
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() // supaya tombol Back tidak kembali ke layar login
    }
}