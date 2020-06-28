package com.rondinella.strhack.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rondinella.strhack.R
import java.util.*


open class RoutesListAdapter internal constructor(context: Context?, data: List<String>, private val listener: View.OnClickListener) : RecyclerView.Adapter<RoutesListAdapter.ViewHolder>() {
    private val mData: List<String> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.row_routeslist, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.myTextView.text = mData[position]
        holder.itemView.setOnClickListener(listener)
    }

    // total number of rows
    override fun getItemCount(): Int {
        return mData.size
    }

    // stores and recycles views as they are scrolled off screen
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var myTextView: TextView = itemView.findViewById(R.id.routeName)
    }

}
