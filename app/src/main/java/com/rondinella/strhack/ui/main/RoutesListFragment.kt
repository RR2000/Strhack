package com.rondinella.strhack.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.example.strhack.AdvancedGeoPoint
import com.rondinella.strhack.R
import com.rondinella.strhack.activities.CourseViewerActivity
import com.rondinella.strhack.traker.Course
import kotlinx.android.synthetic.main.fragment_routeslist.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
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

        val gpxFiles = ArrayList<File>()
            if(File(context!!.getExternalFilesDir(null).toString() + "/tracks").exists())
                gpxFiles.addAll(File(context!!.getExternalFilesDir(null).toString() + "/tracks").listFiles()!!)

        gpxFiles.sort()

        val routeNames = ArrayList<String>()

        for (i in gpxFiles.indices)
            if(gpxFiles[i].name.endsWith(".gpx",true))
                routeNames.add(gpxFiles[i].name)

        val adapter = ArrayAdapter(activity!!.applicationContext, android.R.layout.simple_list_item_1, routeNames)
        id_gpx_list.adapter = adapter

        id_gpx_list.setOnItemClickListener { adapterView, v, i, l ->
            val intent = Intent(context, CourseViewerActivity::class.java).apply {
                putExtra("filename", routeNames[i])
            }
            startActivity(intent)

        }
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