package com.hatirlatma.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class ArchiveActivity : AppCompatActivity() {

    private lateinit var adapter: ReminderAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var tvStats: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive)

        tvEmpty = findViewById(R.id.tvEmpty)
        tvStats = findViewById(R.id.tvStats)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.archive_menu)
        toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_delete_all) {
                confirmDeleteAll()
                true
            } else false
        }

        val rv = findViewById<RecyclerView>(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ReminderAdapter(true) { showActions(it) }
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val done = Repo.all(this)
            .filter { it.completed }
            .sortedByDescending { it.completedAt }
        adapter.submit(done)
        tvEmpty.visibility = if (done.isEmpty()) View.VISIBLE else View.GONE

        val now = Calendar.getInstance()
        val thisMonth = done.count {
            val c = Calendar.getInstance()
            c.timeInMillis = it.completedAt
            c.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                c.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        }
        tvStats.text = "Toplam tamamlanan görev: ${done.size}\nBu ay tamamlanan: $thisMonth"
    }

    private fun showActions(r: Reminder) {
        MaterialAlertDialogBuilder(this)
            .setTitle(r.text.replace('\n', ' ').take(60))
            .setItems(arrayOf("↩️ Ana sayfaya geri al", "🗑 Sil")) { _, which ->
                if (which == 0) {
                    Actions.restore(this, r.id)
                    Toast.makeText(this, "Ana sayfaya geri alındı", Toast.LENGTH_SHORT).show()
                } else {
                    Repo.delete(this, r.id)
                    Toast.makeText(this, "Silindi", Toast.LENGTH_SHORT).show()
                }
                refresh()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun confirmDeleteAll() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Arşivi temizle")
            .setMessage("Arşivdeki tüm görevler kalıcı olarak silinsin mi?")
            .setPositiveButton("Sil") { _, _ ->
                Repo.deleteCompleted(this)
                refresh()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }
}
