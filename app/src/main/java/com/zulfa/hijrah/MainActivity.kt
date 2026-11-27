package com.zulfa.hijrah

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zulfa.hijrah.data.IbadahOptions
import com.zulfa.hijrah.data.IbadahRecord
import com.zulfa.hijrah.databinding.ActivityMainBinding
import com.zulfa.hijrah.ui.auth.LoginActivity
import com.zulfa.hijrah.ui.ibadah.DetailActivity
import com.zulfa.hijrah.ui.ibadah.FormActivity
import com.zulfa.hijrah.ui.ibadah.IbadahAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = IbadahAdapter { record -> openDetail(record) }
    private var listener: ListenerRegistration? = null

    // Semua data dari Firestore disimpan di sini, filter dijalankan di aplikasi
    private var allRecords: List<IbadahRecord> = emptyList()
    private var filterJenis = ""   // kosong = semua jenis
    private var filterStatus = ""  // kosong = semua status

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        binding.tvWelcome.text = "Assalamu'alaikum, ${user?.displayName ?: user?.email}"

        binding.rvIbadah.layoutManager = LinearLayoutManager(this)
        binding.rvIbadah.adapter = adapter

        setupFilters()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, FormActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            goToLogin()
        }
    }

    private fun setupFilters() {
        val allJenis = getString(R.string.filter_all_jenis)
        val items = listOf(allJenis) + IbadahOptions.JENIS
        binding.actFilterJenis.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        )
        binding.actFilterJenis.setText(allJenis, false)
        binding.actFilterJenis.setOnItemClickListener { _, _, position, _ ->
            filterJenis = if (position == 0) "" else IbadahOptions.JENIS[position - 1]
            applyFilter()
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            filterStatus = when (checkedIds.firstOrNull()) {
                R.id.chipSelesai -> IbadahOptions.STATUS[0]
                R.id.chipTerlewat -> IbadahOptions.STATUS[1]
                R.id.chipQadha -> IbadahOptions.STATUS[2]
                else -> ""
            }
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = allRecords.filter { record ->
            (filterJenis.isEmpty() || record.jenis == filterJenis) &&
                    (filterStatus.isEmpty() || record.status == filterStatus)
        }
        adapter.submitList(filtered)

        when {
            allRecords.isEmpty() -> showMessage(getString(R.string.empty_list))
            filtered.isEmpty() -> showMessage(getString(R.string.empty_filter))
            else -> binding.tvEmpty.visibility = View.GONE
        }
    }

    // Mulai mendengarkan data saat layar terlihat, berhenti saat tidak terlihat
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

                // copy(id = doc.id) memastikan id dokumen pasti terisi
                allRecords = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject(IbadahRecord::class.java)?.copy(id = doc.id)
                }
                applyFilter()
            }
    }

    private fun openDetail(record: IbadahRecord) {
        startActivity(
            Intent(this, DetailActivity::class.java)
                .putExtra(DetailActivity.EXTRA_ID, record.id)
        )
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