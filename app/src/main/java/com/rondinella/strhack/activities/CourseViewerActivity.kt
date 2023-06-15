package com.rondinella.strhack.activities

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
import com.google.android.gms.location.LocationServices
import com.rondinella.strhack.R
import com.rondinella.strhack.databinding.ActivityCourseViewerBinding
import com.rondinella.strhack.tracker.Course
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
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


    @Throws(IOException::class)
    fun handleReceiveGpx(intent: Intent): String {
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
            val bytes: ByteArray = getFileFromPath(file)

            return String(bytes)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 42 && resultCode == 0)
            finish()
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
        lateinit var limitedGeoPoints: ArrayList<GeoPoint>

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
            binding.idMapGpxViewer.controller.setZoom(15.0)
            binding.idMapGpxViewer.controller.animateTo(course.centralPoint())
            binding.idMapGpxViewer.zoomToBoundingBox(course.boundingBox(), true)

            limitedGeoPoints = course.getPointEvery(1)//TODO cambiare con 4
            //Log.d("DEBUG", "Central Point: ${course.centralPoint()}")
            //Log.d("DEBUG", "Bounding Box: ${course.boundingBox()}")

            binding.buttonBlankMap.performClick()

            binding.textDistance.text = (round(course.distance * 100) / 100).toString()

        }

        binding.toolbarCourseViewer.setOnMenuItemClickListener {
            if (it.itemId == R.id.button_remove_course) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_title))
                    .setMessage(getString(R.string.delete_message))
                    .setPositiveButton(getString(R.string.delete)) { dialogInterface, i ->
                        File(applicationContext.filesDir.absolutePath + "/tracks/" + intent.getStringExtra("filename")).delete()
                        finish()//startActivity(Intent(this, MainActivity::class.java))
                    }.setNegativeButton(getString(R.string.cancel), null).show()
            } else if (it.itemId == R.id.button_edit_course) {
                val intentEditor = Intent(this, CourseEditorActivity::class.java)
                intentEditor.putExtra("filename", intent.getStringExtra("filename"))
                startActivityForResult(intentEditor, 42)
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

        binding.idMapGpxViewer.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> binding.scrollviewCourseViewer.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP -> binding.scrollviewCourseViewer.requestDisallowInterceptTouchEvent(false)
            }
            view.onTouchEvent(motionEvent)
        }
    }

    private suspend fun drawBlankMap(geoPoints: ArrayList<GeoPoint>, map: MapView, loadingCourseCircle: ProgressBar) {
        Log.d("DEBUG", "drawBlankMap started")
        map.overlayManager.removeAll(map.overlays)

        map.visibility = View.INVISIBLE
        loadingCourseCircle.visibility = View.VISIBLE

        withContext(Dispatchers.IO) {
            try {
                for (i in 1 until geoPoints.size) {
                    val seg = Polyline()
                    seg.addPoint(geoPoints[i - 1])
                    seg.addPoint(geoPoints[i])

                    seg.outlinePaint.strokeCap = Paint.Cap.ROUND
                    withContext(Dispatchers.Main) {
                        map.overlayManager.add(seg)
                    }
                }
            } catch (e: Exception) {
                Log.e("DEBUG", "Exception in coroutine", e)
            }
        }

        Log.d("DEBUG", "drawBlankMap about to complete")
        map.visibility = View.VISIBLE
        loadingCourseCircle.visibility = View.INVISIBLE
        Log.d("DEBUG", "drawBlankMap completed")

        map.visibility = View.VISIBLE
        map.invalidate()
        loadingCourseCircle.visibility = View.INVISIBLE
        loadingCourseCircle.invalidate()
    }


    private suspend fun drawAltitudeDifferenceMap(course: Course, geoPoints: ArrayList<GeoPoint>, map: MapView, loadingCourseCircle: ProgressBar) {
        map.overlayManager.clear()

        val maxAltitude = course.maxAltitude()
        val minAltitude = course.minAltitude()

        map.visibility = View.INVISIBLE
        loadingCourseCircle.visibility = View.VISIBLE

        withContext(Default) {
            for (i in 1 until geoPoints.size) {
                val seg = Polyline()
                seg.addPoint(geoPoints[i - 1])
                seg.addPoint(geoPoints[i])

                var colorModifier: Int = (((geoPoints[i].altitude - minAltitude) / (maxAltitude - minAltitude)) * 255.0).toInt()

                if (colorModifier > 255)
                    colorModifier = 255

                seg.outlinePaint.color = Color.rgb(colorModifier, 255 - colorModifier, 0)
                seg.outlinePaint.strokeCap = Paint.Cap.ROUND
                withContext(Main) {
                    map.overlayManager.add(seg)
                }
            }
        }

        map.visibility = View.VISIBLE
        loadingCourseCircle.visibility = View.INVISIBLE
    }

    private suspend fun drawSlopeMap(geoPoints: ArrayList<GeoPoint>, map: MapView, loadingCourseCircle: ProgressBar) {
        map.overlayManager.clear()

        map.visibility = View.INVISIBLE
        loadingCourseCircle.visibility = View.VISIBLE

        withContext(Default) {
            for (i in 0 until geoPoints.size - 1) {
                val seg = Polyline()

                seg.addPoint(geoPoints[i])
                seg.addPoint(geoPoints[i + 1])

                val distance = seg.actualPoints.last().distanceToAsDouble(seg.actualPoints.first())
                val altitude = seg.actualPoints.last().altitude - seg.actualPoints.first().altitude
                val slope = altitude / distance * 100

                var colorModifier = if (slope > 0)
                    ((slope / 25.0) * 255.0).toInt()
                else
                    ((slope / 40.0) * 255.0).toInt()

                if (colorModifier < -255)
                    colorModifier = -255
                if (colorModifier > 255)
                    colorModifier = 255

                if (slope >= 0)
                    seg.outlinePaint.color = Color.rgb(colorModifier, 255 - colorModifier, 0)
                else {
                    colorModifier = abs(colorModifier)
                    seg.outlinePaint.color = Color.rgb(0, 255 - colorModifier, colorModifier)
                }


                seg.outlinePaint.strokeCap = Paint.Cap.ROUND
                withContext(Main) {
                    map.overlayManager.add(seg)
                }
            }
        }

        map.visibility = View.VISIBLE
        loadingCourseCircle.visibility = View.INVISIBLE
    }
}
