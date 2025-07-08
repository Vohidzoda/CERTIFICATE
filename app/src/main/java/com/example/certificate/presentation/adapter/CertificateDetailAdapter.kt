package com.example.certificate.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.certificate.R
import com.example.domain.model.SSLCertificateEntry

class CertificateDetailAdapter(
    private val onCopyClick: (sha256: String) -> Unit
) : RecyclerView.Adapter<CertificateDetailAdapter.ViewHolder>() {

    private var items: List<SSLCertificateEntry> = emptyList()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleCertificateTv: TextView = itemView.findViewById(R.id.textViewTitleCertificate)
        private val valueCertificateTv: TextView = itemView.findViewById(R.id.textViewValueCertificate)
        private val titlePublisherTv: TextView = itemView.findViewById(R.id.textViewTitlePublisher)
        private val valuePublisherTv: TextView = itemView.findViewById(R.id.textViewValuePublisher)
        private val titleSha256Tv: TextView = itemView.findViewById(R.id.textViewTitleSha256)
        private val valueSha256Tv: TextView = itemView.findViewById(R.id.textViewValueSha256)
        private val startDateTv: TextView = itemView.findViewById(R.id.textViewStartDate)
        private val endDateTv: TextView = itemView.findViewById(R.id.textViewEndDate)
        private val copyIv: ImageView = itemView.findViewById(R.id.ic_copy)

        fun bind(item: SSLCertificateEntry) {
            titleCertificateTv.text = itemView.context.getString(R.string.cert_subject_label)
            titlePublisherTv.text = itemView.context.getString(R.string.cert_issuer_label)
            titleSha256Tv.text = itemView.context.getString(R.string.cert_sha256_label)

            valueCertificateTv.text = item.subject
            valuePublisherTv.text = item.issuer
            valueSha256Tv.text = item.sha256

            if (item.validFrom.isNotBlank()) {
                startDateTv.visibility = View.VISIBLE
                startDateTv.text = itemView.context.getString(R.string.cert_valid_from, item.validFrom)
                startDateTv.setTextColor(ContextCompat.getColor(itemView.context, R.color.start_date_color))
            } else {
                startDateTv.visibility = View.GONE
            }

            if (item.validTo.isNotBlank()) {
                endDateTv.visibility = View.VISIBLE
                endDateTv.text = itemView.context.getString(R.string.cert_valid_to, item.validTo)
                endDateTv.setTextColor(ContextCompat.getColor(itemView.context, R.color.end_date_color))
            } else {
                endDateTv.visibility = View.GONE
            }

            copyIv.setOnClickListener {
                onCopyClick(item.sha256)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_certificate_detail, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun updateData(newItems: List<SSLCertificateEntry>) {
        items = newItems
        notifyDataSetChanged()
    }
}

