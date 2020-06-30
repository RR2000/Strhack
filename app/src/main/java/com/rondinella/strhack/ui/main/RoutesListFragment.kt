package com.rondinella.strhack.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rondinella.strhack.R
import com.rondinella.strhack.activities.CourseViewerActivity
import com.rondinella.strhack.utils.convertStringFilenameToStringName
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

        var routesFilenameName = refreshRoutesFilenameName()

        val onClickListenerAdapter = View.OnClickListener {
            val position = id_gpx_list.getChildLayoutPosition(it)
            val intent = Intent(context, CourseViewerActivity::class.java).apply {
                putExtra("filename", routesFilenameName.first[position])
            }
            startActivity(intent)
        }

        val adapter = RoutesListAdapter(context, routesFilenameName.second, onClickListenerAdapter)
        id_gpx_list.adapter = adapter
        id_gpx_list.layoutManager = LinearLayoutManager(context)

        gpx_list_container.setOnRefreshListener {
            routesFilenameName = refreshRoutesFilenameName()
            id_gpx_list.adapter = RoutesListAdapter(context, routesFilenameName.second, onClickListenerAdapter)
            gpx_list_container.isRefreshing = false
        }
    }

    private fun refreshRoutesFilenameName(): Pair<ArrayList<String>, ArrayList<String>> {
        val gpxFiles = ArrayList<File>()
        if (File(context!!.getExternalFilesDir(null).toString() + "/tracks").exists())
            gpxFiles.addAll(File(context!!.getExternalFilesDir(null).toString() + "/tracks").listFiles()!!)

        gpxFiles.sort()

        val routesFilenameName = Pair<ArrayList<String>, ArrayList<String>>(ArrayList(), ArrayList())

        for (i in gpxFiles.indices) {
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

        return routesFilenameName
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