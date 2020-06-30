package com.rondinella.strhack.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.rondinella.strhack.R
import com.rondinella.strhack.activities.CourseViewerActivity
import kotlinx.android.synthetic.main.fragment_routeslist.*
import java.io.File

/**
 * A placeholder fragment containing a simple view.
 */
class RoutesListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_routeslist, container, false)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var routeNames = refreshRouteNames()
        var onClickListenerAdapter = View.OnClickListener {
            val position = id_gpx_list.getChildLayoutPosition(it)
            val intent = Intent(context, CourseViewerActivity::class.java).apply {
                putExtra("filename", routeNames[position])
            }
            startActivity(intent)
        }

        id_gpx_list.layoutManager = LinearLayoutManager(context)
        val adapter = RoutesListAdapter(context, routeNames, onClickListenerAdapter)

        id_gpx_list.adapter = adapter

        gpx_list_container.setOnRefreshListener {
            id_gpx_list.adapter = RoutesListAdapter(context, routeNames, onClickListenerAdapter)
            gpx_list_container.isRefreshing = false
        }
    }

    fun refreshRouteNames(): ArrayList<String> {
        val gpxFiles = ArrayList<File>()
        if (File(context!!.getExternalFilesDir(null).toString() + "/tracks").exists())
            gpxFiles.addAll(File(context!!.getExternalFilesDir(null).toString() + "/tracks").listFiles()!!)

        gpxFiles.sort()

        val routeNames = ArrayList<String>()

        for (i in gpxFiles.indices)
            if (gpxFiles[i].name.endsWith(".gpx", true))
                routeNames.add(gpxFiles[i].name)

        return routeNames
    }

    companion object {

        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int): RoutesListFragment {
            return RoutesListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}