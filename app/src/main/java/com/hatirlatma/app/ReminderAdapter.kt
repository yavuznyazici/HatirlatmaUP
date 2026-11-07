package com.hatirlatma.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReminderAdapter(
    private val archive: Boolean,
    private val onClick: (Reminder) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.VH>() {

    private var items: List<Reminder> = emptyList()

    fun submit(list: List<Reminder>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvTitle)
        val sub: TextView = v.findViewById(R.id.tvSub)
        val titleColor: Int = title.currentTextColor
        val subColor: Int = sub.currentTextColor
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_reminder, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val r = items[position]
        val prefix = (if (r.isAlarm) "⏰ " else "") + (if (r.repeatType != 0) "🔁 " else "")
        h.title.text = prefix + r.text.replace('\n', ' ')

        if (archive) {
            h.sub.text = "Bitti: ${Fmt.shortDateTime(r.completedAt)} • Görev: ${Fmt.shortDateTime(r.dueMillis)}"
            h.title.setTextColor(h.titleColor)
            h.sub.setTextColor(h.subColor)
        } else {
            val now = System.currentTimeMillis()
            val t = r.effectiveTime()
            h.sub.text = "${Fmt.remaining(t, now)} • ${Fmt.dateTime(t)}"
            if (t < now) {
                val red = Color.parseColor("#D32F2F")
                h.title.setTextColor(red)
                h.sub.setTextColor(red)
            } else {
                h.title.setTextColor(h.titleColor)
                h.sub.setTextColor(h.subColor)
            }
        }
        h.itemView.setOnClickListener { onClick(r) }
    }
}
