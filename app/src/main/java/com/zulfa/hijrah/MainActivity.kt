package com.zulfa.hijrah

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zulfa.hijrah.data.ApiClient
import com.zulfa.hijrah.data.IbadahOptions
import com.zulfa.hijrah.data.IbadahRecord
import com.zulfa.hijrah.data.PrayerResponse
import com.zulfa.hijrah.data.ReminderPrefs
import com.zulfa.hijrah.databinding.ActivityMainBinding
import com.zulfa.hijrah.notif.ReminderScheduler
import com.zulfa.hijrah.ui.auth.LoginActivity
import com.zulfa.hijrah.ui.ibadah.DetailActivity
import com.zulfa.hijrah.ui.ibadah.FormActivity
import com.zulfa.hijrah.ui.ibadah.IbadahAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    companion object {
        // Ganti sesuai kotamu kalau perlu (contoh: "Banda Aceh", "Medan")
        private const val PRAYER_CITY = "Lhokseumawe"
        private const val PRAYER_COUNTRY = "Indonesia"
        private const val PRAYER_METHOD = 20 // 20 = Kemenag RI
    }

    private lateinit var binding: ActivityMainBinding
    private val adapter = IbadahAdapter { record -> openDetail(record) }
    private var listener: ListenerRegistration? = null
    private var prayerCall: Call<PrayerResponse>? = null

    // Semua data dari Firestore disimpan di sini, filter dijalankan di aplikasi
    private var allRecords: List<IbadahRecord> = emptyList()
    private var filterJenis = ""   // kosong = semua jenis
    private var filterStatus = ""  // kosong = semua status

    // Hasil dialog izin notifikasi
    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enableReminder()
        } else {
            // Izin ditolak: aplikasi tetap jalan normal, hanya pengingat yang tidak aktif
            binding.reminderRow.switchReminder.isChecked = false
            Toast.makeText(this, R.string.err_notif_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        binding.tvWelcome.text = "Assalamu'alaikum, ${user?.displayName ?: user?.email}"

        binding.rvIbadah.layoutManager = LinearLayoutManager(this)
        binding.rvIbadah.adapter = adapter

        setupFilters()
        setupReminder()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, FormActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            // Pengingat terikat pada sesi login, jadi dimatikan saat keluar
            ReminderScheduler.cancelAll(this)
            ReminderPrefs.setEnabled(this, false)
            FirebaseAuth.getInstance().signOut()
            goToLogin()
        }
        binding.prayerCard.btnRetry.setOnClickListener { loadPrayerTimes() }

        loadPrayerTimes()
    }

    // ---------- Pengingat (notifikasi) ----------

    private fun setupReminder() {
        val row = binding.reminderRow
        // Atur status awal SEBELUM listener dipasang supaya tidak memicu dialog izin
        row.switchReminder.isChecked = ReminderPrefs.isEnabled(this)
        row.switchReminder.setOnCheckedChangeListener { _, checked ->
            if (checked) onReminderOn() else onReminderOff()
        }
        row.btnTestReminder.setOnClickListener {
            if (!ReminderPrefs.isEnabled(this)) {
                Toast.makeText(this, R.string.test_need_enable, Toast.LENGTH_SHORT).show()
            } else {
                ReminderScheduler.scheduleTest(this)
                Toast.makeText(this, R.string.test_scheduled, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hasNotifPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private fun onReminderOn() {
        if (hasNotifPermission()) {
            enableReminder()
        } else {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun onReminderOff() {
        ReminderPrefs.setEnabled(this, false)
        ReminderScheduler.cancelAll(this)
    }

    private fun enableReminder() {
        ReminderPrefs.setEnabled(this, true)
        ReminderScheduler.ensureChannel(this)

        val times = ReminderPrefs.getTimes(this)
        if (times.isEmpty()) {
            // Jadwal belum pernah dimuat; alarm dipasang otomatis setelah API berhasil
            Toast.makeText(this, R.string.reminder_no_times, Toast.LENGTH_LONG).show()
        } else {
            ReminderScheduler.scheduleAll(this, times)
            Toast.makeText(this, R.string.reminder_on, Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Jadwal salat (API) ----------

    private fun loadPrayerTimes() {
        val card = binding.prayerCard
        card.progressPrayer.visibility = View.VISIBLE
        card.tvPrayerError.visibility = View.GONE
        card.btnRetry.visibility = View.GONE

        val call = ApiClient.prayerApi.getTimings(PRAYER_CITY, PRAYER_COUNTRY, PRAYER_METHOD)
        prayerCall = call
        call.enqueue(object : Callback<PrayerResponse> {
            override fun onResponse(call: Call<PrayerResponse>, response: Response<PrayerResponse>) {
                card.progressPrayer.visibility = View.GONE
                val data = response.body()?.data
                val timings = data?.timings
                if (!response.isSuccessful || timings == null) {
                    showPrayerError()
                    return
                }

                card.tvSubuh.text = displayTime(timings.fajr)
                card.tvZuhur.text = displayTime(timings.dhuhr)
                card.tvAsar.text = displayTime(timings.asr)
                card.tvMagrib.text = displayTime(timings.maghrib)
                card.tvIsya.text = displayTime(timings.isha)

                card.tvPrayerDate.text = "$PRAYER_CITY • ${data.date?.readable.orEmpty()}"
                val hijri = data.date?.hijri
                card.tvHijri.text =
                    if (hijri != null) "${hijri.day} ${hijri.month?.en} ${hijri.year} H" else ""

                // Simpan jam salat, lalu pasang alarm kalau pengingat sedang aktif
                val times = mapOf(
                    1 to timings.fajr, 2 to timings.dhuhr, 3 to timings.asr,
                    4 to timings.maghrib, 5 to timings.isha
                ).mapNotNull { (id, value) -> value?.take(5)?.let { id to it } }.toMap()
                ReminderPrefs.saveTimes(this@MainActivity, times)
                if (ReminderPrefs.isEnabled(this@MainActivity)) {
                    ReminderScheduler.scheduleAll(this@MainActivity, times)
                }
            }

            override fun onFailure(call: Call<PrayerResponse>, t: Throwable) {
                if (call.isCanceled) return // layar ditutup, abaikan
                card.progressPrayer.visibility = View.GONE
                showPrayerError()
            }
        })
    }

    // API kadang menambah zona waktu di belakang jam (misal "04:52 (WIB)"), ambil 5 karakter pertama
    private fun displayTime(value: String?): String =
        value?.take(5) ?: getString(R.string.time_placeholder)

    private fun showPrayerError() {
        binding.prayerCard.tvPrayerError.visibility = View.VISIBLE
        binding.prayerCard.btnRetry.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        prayerCall?.cancel()
    }

    // ---------- Filter ----------

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

    // ---------- Data Firestore ----------

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