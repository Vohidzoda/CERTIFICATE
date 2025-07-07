package com.example.certificate.presentation.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.certificate.R
import com.example.certificate.presentation.model.CertificateKeyDetail

class CertificateDetailAdapter(
    private var items: List<CertificateKeyDetail>
) : RecyclerView.Adapter<CertificateDetailAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val titleTv: TextView = itemView.findViewById(R.id.textViewTitle)
        private val valueTv: TextView = itemView.findViewById(R.id.textViewValue)
        private val copyIv: ImageView = itemView.findViewById(R.id.ic_copy)
        private val startDateTv: TextView = itemView.findViewById(R.id.textViewStartDate)
        private val endDateTv: TextView = itemView.findViewById(R.id.textViewEndDate)

        fun bind(item: CertificateKeyDetail) {
            titleTv.text = item.title
            valueTv.text = item.value

            if (item.startDate.isNotBlank()) {
                startDateTv.visibility = View.VISIBLE
                startDateTv.text = itemView.context.getString(R.string.cert_valid_from, item.startDate)
                startDateTv.setTextColor(ContextCompat.getColor(itemView.context, R.color.start_date_color))
            } else {
                startDateTv.visibility = View.GONE
            }

            if (item.endDate.isNotBlank()) {
                endDateTv.visibility = View.VISIBLE
                endDateTv.text = itemView.context.getString(R.string.cert_valid_to, item.endDate)
                endDateTv.setTextColor(ContextCompat.getColor(itemView.context, R.color.end_date_color))
            } else {
                endDateTv.visibility = View.GONE
            }

            copyIv.setOnClickListener {
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipTitle = itemView.context.getString(R.string.label_clipboard_title)
                val clip = ClipData.newPlainText(clipTitle, item.value)
                clipboard.setPrimaryClip(clip)
                val message = itemView.context.getString(R.string.copied_toast, item.value)
                Toast.makeText(itemView.context, message, Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_certificate_detail, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun updateData(newItems: List<CertificateKeyDetail>) {
        items = newItems
        notifyDataSetChanged()
    }
}
