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
import androidx.recyclerview.widget.RecyclerView
import com.example.certificate.R

class CertificateDetailAdapter(
    private var items: List<Pair<String, String>>
) : RecyclerView.Adapter<CertificateDetailAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val titleTv: TextView = itemView.findViewById(R.id.textViewTitle)
        private val valueTv: TextView = itemView.findViewById(R.id.textViewValue)
        private val copyIv: ImageView = itemView.findViewById(R.id.ic_copy)

        fun bind(item: Pair<String, String>) {
            titleTv.text = item.first
            valueTv.text = item.second

            copyIv.setOnClickListener {
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", item.second)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(itemView.context, "Скопировано: ${item.second}", Toast.LENGTH_SHORT).show()
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

    fun updateData(newItems: List<Pair<String, String>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
