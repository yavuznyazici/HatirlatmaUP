package com.hatirlatma.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditActivity : AppCompatActivity() {

    private val cal: Calendar = Calendar.getInstance()
    private var existing: Reminder? = null
    private var alarmMode = false

    private lateinit var etText: TextInputEditText
    private lateinit var cbRepeat: CheckBox
    private lateinit var rowDate: View
    private lateinit var btnDate: MaterialButton
    private lateinit var btnTime: MaterialButton
    private lateinit var layoutRepeat: View
    private lateinit var spFreq: Spinner
    private lateinit var spWeekday: Spinner
    private lateinit var spMonthDay: Spinner
    private lateinit var lblWeekday: View
    private lateinit var lblMonthDay: View

    private val weekdayLabels = arrayOf(
        "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"
    )
    private val weekdayValues = intArrayOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        etText = findViewById(R.id.etText)
        cbRepeat = findViewById(R.id.cbRepeat)
        rowDate = findViewById(R.id.rowDate)
        btnDate = findViewById(R.id.btnDate)
        btnTime = findViewById(R.id.btnTime)
        layoutRepeat = findViewById(R.id.layoutRepeat)
        spFreq = findViewById(R.id.spFreq)
        spWeekday = findViewById(R.id.spWeekday)
        spMonthDay = findViewById(R.id.spMonthDay)
        lblWeekday = findViewById(R.id.lblWeekday)
        lblMonthDay = findViewById(R.id.lblMonthDay)
        val tvHeader = findViewById<TextView>(R.id.tvHeader)

        val editId = intent.getIntExtra(EXTRA_ID, -1)
        existing = if (editId >= 0) Repo.get(this, editId) else null
        alarmMode = existing?.isAlarm ?: intent.getBooleanExtra(EXTRA_ALARM, false)

        spFreq.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Günlük", "Haftalık", "Aylık")
        )
        spWeekday.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, weekdayLabels
        )
        spMonthDay.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            (1..31).map { "Ayın $it. günü" }
        )

        val ex = existing
        if (ex != null) {
            etText.setText(ex.text)
            if (ex.repeatType == Recur.NONE) {
                cal.timeInMillis = ex.dueMillis
            } else {
                cal.set(Calendar.HOUR_OF_DAY, ex.hour)
                cal.set(Calendar.MINUTE, ex.minute)
                cbRepeat.isChecked = true
                spFreq.setSelection(ex.repeatType - 1)
                if (ex.repeatType == Recur.WEEKLY) {
                    spWeekday.setSelection(weekdayValues.indexOf(ex.repeatDay).coerceAtLeast(0))
                } else if (ex.repeatType == Recur.MONTHLY) {
                    spMonthDay.setSelection((ex.repeatDay - 1).coerceIn(0, 30))
                }
            }
        } else {
            cal.add(Calendar.MINUTE, 10)
        }

        tvHeader.text = when {
            alarmMode -> "⏰ Flaş Alarm"
            ex != null -> "Hatırlatmayı düzenle"
            else -> "Yeni hatırlatma"
        }
        if (alarmMode) {
            findViewById<View>(R.id.tvAlarmInfo).visibility = View.VISIBLE
            cbRepeat.visibility = View.GONE
        }

        btnDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    cal.set(y, m, d)
                    updateUi()
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        btnTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, h, min ->
                    cal.set(Calendar.HOUR_OF_DAY, h)
                    cal.set(Calendar.MINUTE, min)
                    updateUi()
                },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
            ).show()
        }
        cbRepeat.setOnCheckedChangeListener { _, _ -> updateUi() }
        spFreq.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                updateUi()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        findViewById<View>(R.id.btnSave).setOnClickListener { save() }

        updateUi()
    }

    private fun updateUi() {
        val rep = cbRepeat.isChecked && !alarmMode
        rowDate.visibility = if (rep) View.GONE else View.VISIBLE
        layoutRepeat.visibility = if (rep) View.VISIBLE else View.GONE
        val f = spFreq.selectedItemPosition
        val showWeek = rep && f == 1
        val showMonth = rep && f == 2
        lblWeekday.visibility = if (showWeek) View.VISIBLE else View.GONE
        spWeekday.visibility = if (showWeek) View.VISIBLE else View.GONE
        lblMonthDay.visibility = if (showMonth) View.VISIBLE else View.GONE
        spMonthDay.visibility = if (showMonth) View.VISIBLE else View.GONE

        btnDate.text = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR")).format(cal.time)
        btnTime.text = String.format(
            Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)
        )
    }

    private fun save() {
        val text = etText.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            Toast.makeText(this, "Lütfen hatırlatma metni yaz", Toast.LENGTH_SHORT).show()
            return
        }
        val now = System.currentTimeMillis()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val type: Int
        val day: Int
        val due: Long
        if (cbRepeat.isChecked && !alarmMode) {
            type = spFreq.selectedItemPosition + 1
            day = when (type) {
                Recur.WEEKLY -> weekdayValues[spWeekday.selectedItemPosition]
                Recur.MONTHLY -> spMonthDay.selectedItemPosition + 1
                else -> 0
            }
            due = Recur.next(type, day, hour, minute, now)
        } else {
            type = Recur.NONE
            day = 0
            val c = cal.clone() as Calendar
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            due = c.timeInMillis
        }

        val ex = existing
        val scheduleChanged = ex == null ||
            type != ex.repeatType || day != ex.repeatDay ||
            hour != ex.hour || minute != ex.minute ||
            (type == Recur.NONE && due != ex.dueMillis)

        if (type == Recur.NONE && scheduleChanged && due <= now) {
            Toast.makeText(this, "Geçmiş bir zaman seçilemez", Toast.LENGTH_SHORT).show()
            return
        }

        val saved: Reminder
        if (ex == null) {
            saved = Reminder(
                id = Repo.nextId(this),
                text = text,
                dueMillis = due,
                repeatType = type,
                repeatDay = day,
                hour = hour,
                minute = minute,
                isAlarm = alarmMode
            )
            Repo.add(this, saved)
        } else {
            ex.text = text
            if (scheduleChanged) {
                ex.dueMillis = due
                ex.repeatType = type
                ex.repeatDay = day
                ex.hour = hour
                ex.minute = minute
                ex.snoozedUntil = 0L
                ex.notified = false
            }
            Repo.update(this, ex)
            saved = ex
        }
        AlarmScheduler.schedule(this, saved)
        Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show()
        finish()
    }
}
