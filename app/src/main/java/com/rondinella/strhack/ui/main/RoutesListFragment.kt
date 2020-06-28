package com.rondinella.strhack.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rondinella.strhack.R
import com.rondinella.strhack.activities.CourseViewerActivity
import kotlinx.android.synthetic.main.fragment_routeslist.*
import java.io.File

/**
 * A placeholder fragment containing a simple view.
 */
class RoutesListFragment : Fragment(){

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

        id_gpx_list.layoutManager = LinearLayoutManager(context)
        val adapter = RoutesListAdapter(context, routeNames, View.OnClickListener {
            val position = id_gpx_list.getChildLayoutPosition(it)
            val intent = Intent(context, CourseViewerActivity::class.java).apply {
                putExtra("filename", routeNames[position])
            }
            startActivity(intent)
        })

        id_gpx_list.adapter = adapter

        /*
        id_gpx_list.setOnClickListener {
            Toast.makeText(context, "Cliccato", Toast.LENGTH_SHORT).show()


        }

        id_gpx_list.setOnClickListener { adapterView, v, i, l ->
            val intent = Intent(context, CourseViewerActivity::class.java).apply {
                putExtra("filename", routeNames[i])
            }
            startActivity(intent)
        }*/
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