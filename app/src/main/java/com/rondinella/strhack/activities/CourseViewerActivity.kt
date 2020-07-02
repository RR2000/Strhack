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
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.rondinella.strhack.R
import com.rondinella.strhack.tracker.Course
import kotlinx.android.synthetic.main.activity_course_viewer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.io.*
import kotlin.math.abs


@Suppress("DEPRECATION")
class CourseViewerActivity : AppCompatActivity() {

    fun getFileFromPath(file: File): ByteArray {
        val size = file.length().toInt()
        val bytes = ByteArray(size)
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
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

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_viewer)

        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        id_map_gpxViewer.visibility = View.INVISIBLE
        loading_course_circle.visibility = View.VISIBLE

        //It removes standard button in order to use touch controls
        id_map_gpxViewer.setBuiltInZoomControls(false)
        id_map_gpxViewer.setMultiTouchControls(true)
        val overlayRotation = RotationGestureOverlay(this, id_map_gpxViewer).apply { isEnabled = true }
        id_map_gpxViewer.overlays.add(overlayRotation)

        lateinit var course: Course
        lateinit var limitedGeoPoints: ArrayList<GeoPoint>

        if (intent.action == Intent.ACTION_VIEW) {
            course = Course(handleReceiveGpx(intent))
        }

        CoroutineScope(Main).launch {
            if (intent.action != Intent.ACTION_VIEW) {
                course = Course(File(getExternalFilesDir(null).toString() + "/tracks/" + intent.getStringExtra("filename")))
            }
        }.invokeOnCompletion {
            CoroutineScope(Main).launch {
                toolbar_course_viewer.setTitleTextColor(Color.WHITE)

                Log.w("Initial size", course.geoPoints().size.toString())

            }.invokeOnCompletion {
                id_map_gpxViewer.controller.animateTo(course.centralPoint())
                id_map_gpxViewer.zoomToBoundingBox(course.boundingBox(), true)
                toolbar_course_viewer.title = course.courseName()

                limitedGeoPoints = course.getPointEvery(4)

                button_blankMap.performClick()
            }
        }

        button_blankMap.setOnClickListener {
            CoroutineScope(Main).launch {
                drawBlankMap(limitedGeoPoints, id_map_gpxViewer, loading_course_circle)
            }
        }

        button_slope.setOnClickListener {
            CoroutineScope(Main).launch {
                drawSlopeMap(limitedGeoPoints, id_map_gpxViewer, loading_course_circle)
            }
        }

        button_altitude_difference.setOnClickListener {
            CoroutineScope(Main).launch {
                drawAltitudeDifferenceMap(course, limitedGeoPoints, id_map_gpxViewer, loading_course_circle)
            }
        }

        id_map_gpxViewer.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> scrollview_course_viewer.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP -> scrollview_course_viewer.requestDisallowInterceptTouchEvent(false)
            }
            view.onTouchEvent(motionEvent)
        }

        setTheme(R.style.AppTheme)
    }

    private suspend fun drawBlankMap(geoPoints: ArrayList<GeoPoint>, map: MapView, loadingCourseCircle: ProgressBar) {
        map.overlayManager.removeAll(map.overlays)

        map.visibility = View.INVISIBLE
        loadingCourseCircle.visibility = View.VISIBLE

        withContext(Default) {
            for (i in 1 until geoPoints.size) {
                val seg = Polyline()
                seg.addPoint(geoPoints[i - 1])
                seg.addPoint(geoPoints[i])

                seg.outlinePaint.strokeCap = Paint.Cap.ROUND
                withContext(Main) {
                    map.overlayManager.add(seg)
                }
            }
        }

        map.visibility = View.VISIBLE
        loadingCourseCircle.visibility = View.INVISIBLE
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
