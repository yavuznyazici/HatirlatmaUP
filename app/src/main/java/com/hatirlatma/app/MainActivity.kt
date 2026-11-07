package com.hatirlatma.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ReminderAdapter
    private lateinit var tvEmpty: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            adapter.notifyDataSetChanged()
            handler.postDelayed(this, 30_000L)
        }
    }

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvEmpty = findViewById(R.id.tvEmpty)
        val rv = findViewById<RecyclerView>(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ReminderAdapter(false) { showActions(it) }
        rv.adapter = adapter

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, EditActivity::class.java))
        }
        findViewById<Button>(R.id.btnAlarm).setOnClickListener {
            startActivity(Intent(this, EditActivity::class.java).putExtra(EXTRA_ALARM, true))
        }
        findViewById<Button>(R.id.btnArchive).setOnClickListener {
            startActivity(Intent(this, ArchiveActivity::class.java))
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Alarmların kayıtlı olduğundan emin ol
        Repo.all(this).filter { !it.completed && !it.notified }
            .forEach { AlarmScheduler.schedule(this, it) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, 30_000L)
        checkFullScreenPermission()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun refresh() {
        val items = Repo.all(this)
            .filter { !it.completed }
            .sortedBy { it.effectiveTime() }
        adapter.submit(items)
        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showActions(r: Reminder) {
        MaterialAlertDialogBuilder(this)
            .setTitle(r.text.replace('\n', ' ').take(60))
            .setItems(arrayOf("✏️ Düzenle", "✅ Tamamlandı olarak işaretle")) { _, which ->
                if (which == 0) {
                    startActivity(Intent(this, EditActivity::class.java).putExtra(EXTRA_ID, r.id))
                } else {
                    Actions.complete(this, r.id)
                    refresh()
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun checkFullScreenPermission() {
        if (Build.VERSION.SDK_INT < 34) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = getSystemService(NotificationManager::class.java)
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (!nm.canUseFullScreenIntent() && !prefs.getBoolean("fsi_asked", false)) {
            prefs.edit().putBoolean("fsi_asked", true).apply()
            MaterialAlertDialogBuilder(this)
                .setTitle("Kilit ekranı izni")
                .setMessage("Flaş Alarm'ın kilitli ekranda açılabilmesi için “Tam ekran bildirimleri” iznini vermelisin.")
                .setPositiveButton("Ayarları aç") { _, _ ->
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
                .setNegativeButton("Sonra", null)
                .show()
        }
    }
}
