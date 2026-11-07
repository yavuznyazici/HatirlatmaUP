package com.hatirlatma.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SnoozeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(EXTRA_ID, -1)
        if (Repo.get(this, id) == null) {
            finish()
            return
        }
        showSnoozeDialog(
            this, id,
            onSnoozed = {
                Toast.makeText(this, "Ertelendi", Toast.LENGTH_SHORT).show()
                finish()
            },
            onCancel = { finish() }
        )
    }
}
