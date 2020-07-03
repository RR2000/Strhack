package com.rondinella.strhack.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.rondinella.strhack.R
import com.rondinella.strhack.tracker.GpxFileWriter
import com.rondinella.strhack.utils.convertStringFilenameDateToDate
import com.rondinella.strhack.utils.convertStringFilenameDateToTitle
import java.io.File

open class RoutesListAdapter internal constructor(context: Context?, data: List<File>, private val listener: View.OnClickListener) :
    RecyclerView.Adapter<RoutesListAdapter.ViewHolder>() {
    private val routesFile: List<File> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val context = context!!

    // inflates the row layout from xml when needed
    @SuppressLint("InflateParams")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(R.layout.row_routeslist, parent, false)

        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filename = routesFile[position].name.replace(".strhack.gpx", "")
        val title = filename.substringAfter(".title.")
        val date = filename.substringAfter("date.").substringBefore(".title.")

        if (routesFile[position].name == GpxFileWriter.WrittenFilenameData.getFilename().value){
            holder.routeName.text = "Percorso di questo momento! Non puoi aprirlo!"
            holder.routeName.setTextColor(context.getColor(R.color.red))
            holder.routeDate.text = convertStringFilenameDateToDate(date)
        }
        else {

            if (title == "") {
                holder.routeName.text =
                    convertStringFilenameDateToTitle(
                        date,
                        context.getString(R.string.course_of),
                        context.getString(R.string.at_time)
                    )
            } else {
                holder.routeName.text = title
            }
            holder.routeDate.text = convertStringFilenameDateToDate(date)
            holder.itemView.setOnClickListener(listener)
        }
    }

    // total number of rows
    override fun getItemCount(): Int {
        return routesFile.size
    }

    // stores and recycles views as they are scrolled off screen
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var routeName: TextView = itemView.findViewById(R.id.routeName)
        var routeDate: TextView = itemView.findViewById(R.id.routeDate)
    }
}

/*
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
*/