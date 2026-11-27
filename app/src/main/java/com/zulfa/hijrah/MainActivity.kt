package com.zulfa.hijrah

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zulfa.hijrah.data.IbadahRecord
import com.zulfa.hijrah.databinding.ActivityMainBinding
import com.zulfa.hijrah.ui.auth.LoginActivity
import com.zulfa.hijrah.ui.ibadah.FormActivity
import com.zulfa.hijrah.ui.ibadah.IbadahAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = IbadahAdapter()
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        binding.tvWelcome.text = "Assalamu'alaikum, ${user?.displayName ?: user?.email}"

        binding.rvIbadah.layoutManager = LinearLayoutManager(this)
        binding.rvIbadah.adapter = adapter

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, FormActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            goToLogin()
        }
    }

    // Mulai mendengarkan data saat layar terlihat, berhenti saat layar tidak terlihat
    override fun onStart() {
        super.onStart()
        startListening()
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
    }

    private fun startListening() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            goToLogin()
            return
        }

        binding.progress.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        listener = FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("ibadah")
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                binding.progress.visibility = View.GONE

                if (error != null) {
                    showMessage(getString(R.string.err_load))
                    return@addSnapshotListener
                }

                val list = snapshot?.toObjects(IbadahRecord::class.java).orEmpty()
                adapter.submitList(list)

                if (list.isEmpty()) {
                    showMessage(getString(R.string.empty_list))
                } else {
                    binding.tvEmpty.visibility = View.GONE
                }
            }
    }

    private fun showMessage(text: String) {
        binding.tvEmpty.text = text
        binding.tvEmpty.visibility = View.VISIBLE
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}