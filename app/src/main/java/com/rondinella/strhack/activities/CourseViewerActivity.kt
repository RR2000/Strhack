package com.rondinella.strhack.activities

import android.animation.ArgbEvaluator
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.children
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.location.LocationServices
import com.rondinella.strhack.R
import com.rondinella.strhack.databinding.ActivityCourseViewerBinding
import com.rondinella.strhack.tracker.AdvancedGeoPoint
import com.rondinella.strhack.tracker.Course
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.NonCancellable.cancel
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.*
import kotlin.math.abs
import kotlin.math.round


class CourseViewerActivity : AppCompatActivity() {

    private var _binding: ActivityCourseViewerBinding? = null
    private val binding get() = _binding!!

    fun getFileFromPath(file: File): ByteArray {
        val size = file.length().toInt()
        val bytes = ByteArray(size)
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bytes
    }


    private fun handleReceiveGpx(intent: Intent): String {
        val bytes: ByteArray

        val gpxUri: Uri = intent.data!!
        val file = File(cacheDir, "gpx")
        val inputStream: InputStream? = contentResolver.openInputStream(gpxUri)
        try {
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (inputStream?.read(buffer).also { read = it!! } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        } finally {
            inputStream?.close()
            bytes = getFileFromPath(file)
        }

        return String(bytes)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 42 && resultCode == 0) finish()
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        _binding = ActivityCourseViewerBinding.inflate(layoutInflater)
        setTheme(R.style.AppTheme_NoActionBar)
        setContentView(R.layout.activity_course_viewer)
        binding.toolbarCourseViewer.setTitleTextColor(Color.WHITE)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()


        Configuration.getInstance().load(baseContext, PreferenceManager.getDefaultSharedPreferences(baseContext))

        lateinit var course: Course
        lateinit var limitedGeoPoints: ArrayList<AdvancedGeoPoint>

        if (intent.action == Intent.ACTION_VIEW) {
            course = Course(handleReceiveGpx(intent))
            title = course.geoPoints()[0].date.toString()
        }

        CoroutineScope(Main).launch {
            if (intent.action == Intent.ACTION_VIEW) {
                course = Course(handleReceiveGpx(intent))
                title = course.geoPoints()[0].date.toString()
            } else {
                course = Course(File(applicationContext.filesDir.absolutePath + "/tracks/" + intent.getStringExtra("filename")))
                binding.toolbarCourseViewer.title = intent.getStringExtra("title")
            }
            delay(1000)//TODO implement MVC
            binding.idMapGpxViewer.setBuiltInZoomControls(false)
            binding.idMapGpxViewer.setMultiTouchControls(true)
            val overlayRotation = RotationGestureOverlay(binding.idMapGpxViewer).apply { isEnabled = true }
            binding.idMapGpxViewer.overlays.add(overlayRotation)

            //Get controller of the map
            binding.idMapGpxViewer.controller.setZoom(15.0)
            binding.idMapGpxViewer.controller.animateTo(course.centralPoint())
            binding.idMapGpxViewer.zoomToBoundingBox(course.boundingBox(), true)

            binding.idMapGpxViewer.setOnTouchListener { v, event ->
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

            withContext(IO) {
                limitedGeoPoints = course.getPointEveryMeters(0.00001)
            }
            //Log.d("DEBUG", "Central Point: ${course.centralPoint()}")
            //Log.d("DEBUG", "Bounding Box: ${course.boundingBox()}")

            binding.buttonBlankMap.performClick()

            binding.textDistance.text = course.getDistance().toString() + " km"
            binding.averageSpeed.text = course.getAverageSpeed().toString() + " km/h"
            binding.totalTime.text = course.getTotalTime()
            binding.elevationGain.text = course.getTotalElevationGain().toString() + " m"

        }

        binding.toolbarCourseViewer.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.toolbarCourseViewer.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.button_remove_course -> {
                    AlertDialog.Builder(this).setTitle(getString(R.string.delete_title)).setMessage(getString(R.string.delete_message))
                        .setPositiveButton(getString(R.string.delete)) { dialogInterface, i ->
                            File(applicationContext.filesDir.absolutePath + "/tracks/" + intent.getStringExtra("filename")).delete()
                            finish()//startActivity(Intent(this, MainActivity::class.java))
                        }.setNegativeButton(getString(R.string.cancel), null).show()
                }
                R.id.button_edit_course -> {
                    val intentEditor = Intent(this, CourseEditorActivity::class.java)
                    intentEditor.putExtra("filename", intent.getStringExtra("filename"))
                    startActivityForResult(intentEditor, 42)
                }
            }
            true
        }
        binding.buttonBlankMap.setOnClickListener {
            CoroutineScope(Main).launch {
                drawBlankMap(limitedGeoPoints, binding.idMapGpxViewer, binding.loadingCourseCircle)
            }
        }

        binding.buttonSlope.setOnClickListener {
            CoroutineScope(Main).launch {
                drawSlopeMap(limitedGeoPoints, binding.idMapGpxViewer, binding.loadingCourseCircle)
            }
        }

        binding.buttonAltitudeDifference.setOnClickListener {
            CoroutineScope(Main).launch {
                drawAltitudeDifferenceMap(course, limitedGeoPoints, binding.idMapGpxViewer, binding.loadingCourseCircle)
            }
        }

    }

    private suspend fun drawBlankMap(geoPoints: ArrayList<AdvancedGeoPoint>, map: MapView, loadingCourseCircle: ProgressBar) {
        Log.d("DEBUG", "drawBlankMap started")
        map.overlayManager.removeAll(map.overlays)

        map.visibility = View.INVISIBLE
        loadingCourseCircle.visibility = View.VISIBLE

        val segments = withContext(Dispatchers.IO) {
            geoPoints.windowed(2, 1).map { (start, end) ->
                val seg = Polyline()
                seg.addPoint(start)
                seg.addPoint(end)

                seg.outlinePaint.strokeCap = Paint.Cap.ROUND
                seg
            }
        }
        withContext(Dispatchers.Main) {
            map.overlayManager.addAll(segments)
            map.visibility = View.VISIBLE
            loadingCourseCircle.visibility = View.INVISIBLE
        }
    }


    private suspend fun drawAltitudeDifferenceMap(course: Course, geoPoints: ArrayList<AdvancedGeoPoint>, map: MapView, loadingCourseCircle: ProgressBar) {
        Log.d("DEBUG", "drawAltitudeDifferenceMap started")
        map.overlayManager.removeAll(map.overlays)

        val maxAltitude = course.geoPoints().maxBy { it.altitude }.altitude
        val minAltitude = course.geoPoints().minBy { it.altitude }.altitude


        map.visibility = View.INVISIBLE
        loadingCourseCircle.visibility = View.VISIBLE

        // Create a color interpolator for smoother transitions
        val colorInterpolator = ArgbEvaluator()

        val segments = withContext(Dispatchers.IO) {
            geoPoints.windowed(2, 1).map { (start, end) ->
                val seg = Polyline()
                seg.addPoint(start)
                seg.addPoint(end)

                var altitudeRatio: Float = ((end.altitude - minAltitude) / (maxAltitude - minAltitude)).toFloat()

                // Clamp ratio to 0..1 range just in case
                altitudeRatio = altitudeRatio.coerceIn(0f, 1f)
                val color = colorInterpolator.evaluate(altitudeRatio, Color.GREEN, Color.BLACK) as Int

                seg.outlinePaint.color = color
                seg.outlinePaint.strokeCap = Paint.Cap.ROUND
                seg
            }
        }

        withContext(Dispatchers.Main) {
            map.overlayManager.addAll(segments)
            map.visibility = View.VISIBLE
            loadingCourseCircle.visibility = View.INVISIBLE
        }
    }


    private suspend fun drawSlopeMap(geoPoints: ArrayList<AdvancedGeoPoint>, map: MapView, loadingCourseCircle: ProgressBar) {
        Log.d("DEBUG", "drawSlopeMap started")
        map.overlayManager.removeAll(map.overlays)

        map.visibility = View.INVISIBLE
        loadingCourseCircle.visibility = View.VISIBLE

        // Create two color interpolators for uphill and downhill
        val uphillSlopeColorInterpolator = ArgbEvaluator()
        val downhillSlopeColorInterpolator = ArgbEvaluator()

        // The size of the window used for moving average
        val windowSize = 10

        val slopes = geoPoints.windowed(2, 1) { (start, end) ->
            val distance = end.distanceToAsDouble(start)
            val altitude = end.altitude - start.altitude
            (altitude / distance) / 0.13  //13% slope is the darkest color
        }

        // Apply moving average to the slopes
        val smoothedSlopes = slopes.windowed(windowSize, 1, partialWindows = true) { it.average() }

        val segments = withContext(Dispatchers.IO) {
            geoPoints.zip(smoothedSlopes).windowed(2, 1) { (pointWithSlope1, pointWithSlope2) ->
                val (point1, slope1) = pointWithSlope1
                val (point2, _) = pointWithSlope2

                val seg = Polyline()

                seg.addPoint(point1)
                seg.addPoint(point2)

                val color = when {
                    slope1 > 0 -> {  // Uphill
                        val slopeRatio = slope1.coerceIn(0.0, 1.0).toFloat()
                        uphillSlopeColorInterpolator.evaluate(slopeRatio, Color.GREEN, Color.RED) as Int
                    }
                    else -> {  // Downhill
                        val slopeRatio = (-slope1).coerceIn(0.0, 1.0).toFloat()
                        downhillSlopeColorInterpolator.evaluate(slopeRatio, Color.GREEN, Color.BLUE) as Int
                    }
                }

                seg.outlinePaint.color = color
                seg.outlinePaint.strokeCap = Paint.Cap.ROUND
                seg
            }
        }

        withContext(Dispatchers.Main) {
            map.overlayManager.addAll(segments)
            map.refreshDrawableState()
            loadingCourseCircle.visibility = View.INVISIBLE
            map.visibility = View.VISIBLE
        }
    }

}
