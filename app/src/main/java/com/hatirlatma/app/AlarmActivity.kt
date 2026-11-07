package com.hatirlatma.app

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class AlarmActivity : AppCompatActivity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var reminderId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_alarm)

        findViewById<android.view.View>(R.id.btnAlarmDone).setOnClickListener {
            Actions.complete(this, reminderId)
            finish()
        }
        findViewById<android.view.View>(R.id.btnAlarmSnooze).setOnClickListener {
            showSnoozeDialog(this, reminderId, onSnoozed = { finish() }, onCancel = {})
        }
        load(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        load(intent)
    }

    private fun load(i: Intent) {
        stopAlert()
        reminderId = i.getIntExtra(EXTRA_ID, -1)
        val r = Repo.get(this, reminderId)
        if (r == null || r.completed) {
            finish()
            return
        }
        findViewById<TextView>(R.id.tvAlarmText).text = r.text
        findViewById<TextView>(R.id.tvAlarmTime).text = Fmt.dateTime(r.effectiveTime())
        // Bildirimi kaldır (bildirim sesi susar), sesi bu ekran çalar
        NotificationManagerCompat.from(this).cancel(reminderId)
        startAlert()
    }

    @Suppress("DEPRECATION")
    private fun startAlert() {
        try {
            var uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(this, uri)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val v = getSystemService(Vibrator::class.java)
            v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
            vibrator = v
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlert() {
        try {
            player?.stop()
        } catch (e: Exception) {
        }
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        stopAlert()
        super.onDestroy()
    }
}
