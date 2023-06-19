package com.rondinella.strhack.ui.viewer

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.rondinella.strhack.R
import com.rondinella.strhack.databinding.ActivityCourseViewerBinding
import com.rondinella.strhack.tracker.AdvancedGeoPoint
import com.rondinella.strhack.tracker.Course
import com.rondinella.strhack.ui.editor.CourseEditorActivity
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.io.*


class CourseViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseViewerBinding
    private val courseViewModel: CourseViewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCourseViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTheme(R.style.AppTheme_NoActionBar)
        setupToolbar()
        setupCourseLiveDataObserver()
        setupSegmentsLiveDataObserver()
        setupButtons()
        loadCourseOnStart()
    }

    private fun setupToolbar() {
        binding.toolbarCourseViewer.apply {
            setTitleTextColor(Color.WHITE)
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener { onBackPressed() }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.button_remove_course -> confirmDeleteCourse()
                    R.id.button_edit_course -> openCourseEditor()
                    R.id.button_correct_altitude -> {
                        binding.idMapGpxViewer.visibility = View.INVISIBLE
                        binding.loadingCourseCircle.visibility = View.VISIBLE
                        courseViewModel.correctAltitude()
                    }
                }
                true
            }
        }
    }

    private fun confirmDeleteCourse() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_title))
            .setMessage(getString(R.string.delete_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                val courseFile = File(applicationContext.filesDir.absolutePath + "/tracks/" + intent.getStringExtra("filename"))
                if (courseFile.exists()) courseFile.delete()
                finish()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun openCourseEditor() {
        val intentEditor = Intent(this, CourseEditorActivity::class.java)
        intentEditor.putExtra("filename", intent.getStringExtra("filename"))
        startActivityForResult(intentEditor, 42)
    }

    private fun setupCourseLiveDataObserver() {
        courseViewModel.course.observe(this) { course ->
            setupCourseDetails(course)
            courseViewModel.drawBlankMap()
        }
    }

    private fun setupSegmentsLiveDataObserver() {
        courseViewModel.segments.observe(this) { segments ->
            loadSegmentsToMap(segments)
        }
    }

    private fun setupButtons() {
        binding.buttonBlankMap.setOnClickListener {
            courseViewModel.drawBlankMap()
        }
        binding.buttonSlope.setOnClickListener {
            courseViewModel.drawSlopeMap()
        }
        binding.buttonAltitudeDifference.setOnClickListener {
            courseViewModel.drawAltitudeDifferenceMap()
        }
        binding.buttonSpeed.setOnClickListener {
            courseViewModel.drawSpeedMap()
        }
    }

    private fun loadCourseOnStart() {
        val courseFile = File(applicationContext.filesDir.absolutePath + "/tracks/" + intent.getStringExtra("filename"))
        courseViewModel.loadCourse(courseFile)
    }

    @SuppressLint("SetTextI18n")
    private fun setupCourseDetails(course: Course) {
        binding.toolbarCourseViewer.title = intent.getStringExtra("title")
        setupMapView(course)
        binding.textDistance.text = course.getDistance().toString() + " km"
        binding.averageSpeed.text = course.getAverageSpeed().toString() + " km/h"
        binding.totalTime.text = course.getTotalTime()
        binding.elevationGain.text = course.getTotalElevationGain().toString() + " m"
    }

    private fun setupMapView(course: Course) {
        binding.idMapGpxViewer.apply {
            setBuiltInZoomControls(false)
            setMultiTouchControls(true)
            val overlayRotation = RotationGestureOverlay(this).apply { isEnabled = true }
            overlays.add(overlayRotation)

            // Get controller of the map
            controller.setZoom(15.0)
            controller.animateTo(course.centralPoint())
            zoomToBoundingBox(course.boundingBox(), true)

            setOnTouchListener(getCustomTouchListener())
        }
    }

    private fun getCustomTouchListener(): View.OnTouchListener {
        return View.OnTouchListener { v, event ->
            val viewPager = binding.scrollviewCourseViewer
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    viewPager.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP -> {
                    viewPager.requestDisallowInterceptTouchEvent(false)
                    v.performClick()
                }
                MotionEvent.ACTION_CANCEL -> {
                    viewPager.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    private fun loadSegmentsToMap(segments: List<Polyline>) {
        binding.idMapGpxViewer.apply {
            visibility = View.INVISIBLE
            binding.loadingCourseCircle.visibility = View.VISIBLE

            overlayManager.apply {
                removeAll(overlays)
                addAll(segments)
            }

            visibility = View.VISIBLE
            binding.loadingCourseCircle.visibility = View.INVISIBLE
        }
    }

}
