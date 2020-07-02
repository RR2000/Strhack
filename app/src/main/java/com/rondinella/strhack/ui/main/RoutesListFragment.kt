package com.rondinella.strhack.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rondinella.strhack.R
import com.rondinella.strhack.activities.CourseViewerActivity
import com.rondinella.strhack.tracker.GpxFileWriter
import com.rondinella.strhack.utils.askPermissions
import com.rondinella.strhack.utils.convertStringFilenameToStringName
import com.rondinella.strhack.utils.hasPermissions
import kotlinx.android.synthetic.main.fragment_routeslist.*
import java.io.File

/**
 * A placeholder fragment containing a simple view.
 */
class RoutesListFragment : Fragment() {

    lateinit var parentActivity: Activity
    private var routesFilenameName = Pair<ArrayList<String>, ArrayList<String>>(ArrayList(), ArrayList())
    private lateinit var onClickListenerAdapter: View.OnClickListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_routeslist, container, false)
        parentActivity = activity!!
        return root
    }

    override fun onResume() {
        refresh()
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onClickListenerAdapter = View.OnClickListener {
            val position = id_gpx_list.getChildLayoutPosition(it)
            val intent = Intent(context, CourseViewerActivity::class.java).apply {
                putExtra("filename", routesFilenameName.first[position])
            }
            startActivity(intent)
        }

        id_gpx_list.layoutManager = LinearLayoutManager(context)
        refresh()

        gpx_list_container.setOnRefreshListener {
            refresh()
            gpx_list_container.isRefreshing = false
        }
    }

    private fun refresh() {

        routesFilenameName = Pair(ArrayList(), ArrayList())

        val gpxFiles = ArrayList<File>()
        if (File(context!!.getExternalFilesDir(null).toString() + "/tracks").exists())
            gpxFiles.addAll(File(context!!.getExternalFilesDir(null).toString() + "/tracks").listFiles()!!)

        gpxFiles.sortDescending()

        if (hasPermissions(parentActivity)) {
            for (i in gpxFiles.indices) {
                if (GpxFileWriter.WrittenFilenameData.getFilename().value != null)
                    if (gpxFiles[i].name == GpxFileWriter.WrittenFilenameData.getFilename().value)
                        continue

                if (gpxFiles[i].name.endsWith(".gpx", true)) {
                    routesFilenameName.first.add(gpxFiles[i].name) //add filename
                    if (gpxFiles[i].name.endsWith(".strhack.gpx", true)) {
                        val filename = gpxFiles[i].name.replace(".strhack.gpx", "")
                        val title = filename.substringAfter(".title.")
                        if (title == "") {
                            routesFilenameName.second.add(
                                convertStringFilenameToStringName(
                                    filename.substringAfter("date.").substringBefore(".title."),
                                    getString(R.string.course_of),
                                    getString(R.string.at_time)
                                )
                            )
                        } else
                            routesFilenameName.second.add(title)
                    } else {
                        routesFilenameName.second.add(gpxFiles[i].name)//add visualized name
                    }
                }
            }
            id_gpx_list.adapter = RoutesListAdapter(context, routesFilenameName.second, onClickListenerAdapter)
        } else {
            askPermissions(parentActivity)
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