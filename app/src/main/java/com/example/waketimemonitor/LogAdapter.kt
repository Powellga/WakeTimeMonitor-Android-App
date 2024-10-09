package com.example.waketimemonitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LogAdapter(private val logList: List<String>) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.logTextView.text = logList[position]
    }

    override fun getItemCount(): Int {
        return logList.size
    }

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logTextView: TextView = view.findViewById(android.R.id.text1)
    }
}
