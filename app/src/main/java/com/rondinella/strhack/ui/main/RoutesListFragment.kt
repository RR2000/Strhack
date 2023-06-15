package com.rondinella.strhack.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rondinella.strhack.R
import com.rondinella.strhack.activities.CourseViewerActivity
import com.rondinella.strhack.databinding.FragmentNewtrackBinding
import com.rondinella.strhack.databinding.FragmentRouteslistBinding
import com.rondinella.strhack.utils.askPermissions
import com.rondinella.strhack.utils.hasPermissions
import java.io.File

/**
 * A placeholder fragment containing a simple view.
 */
class RoutesListFragment : Fragment() {

    private var _binding: FragmentRouteslistBinding? = null
    private val binding get() = _binding!!

    lateinit var parentActivity: Activity
    private var routesFilenameName = Pair<ArrayList<String>, ArrayList<String>>(ArrayList(), ArrayList())
    private lateinit var onClickListenerAdapter: View.OnClickListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteslistBinding.inflate(inflater, container, false)
        val root: View = binding.root
        parentActivity = requireActivity()
        return root
    }

    override fun onResume() {
        refresh()
        super.onResume()
    }

    var gpxFiles = ArrayList<File>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onClickListenerAdapter = View.OnClickListener {
            val position = binding.idGpxList.getChildLayoutPosition(it)
            val intent = Intent(context, CourseViewerActivity::class.java).apply {
                putExtra("filename", gpxFiles[position].name)
                putExtra("title", (binding.idGpxList.adapter as RoutesListAdapter).getCourseTitle(position))
            }
            startActivity(intent)
        }

        binding.idGpxList.layoutManager = LinearLayoutManager(context)

        refresh()

        binding.gpxListContainer.setOnRefreshListener {
            refresh()
            binding.gpxListContainer.isRefreshing = false
        }
    }

    private fun refresh() {

        gpxFiles = ArrayList()

        if (File(requireContext().filesDir.absolutePath + "/tracks").exists())
            gpxFiles.addAll(File(requireContext().filesDir.absolutePath + "/tracks").listFiles()!!)

        gpxFiles.sortDescending()

        if (hasPermissions(parentActivity)) {
            binding.idGpxList.adapter = RoutesListAdapter(context, gpxFiles, onClickListenerAdapter)
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