package com.rondinella.strhack.activities

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.rondinella.strhack.R
import com.rondinella.strhack.traker.Course
import kotlinx.android.synthetic.main.activity_course_viewer.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import kotlin.math.abs

@Suppress("DEPRECATION")
class CourseViewerActivity : AppCompatActivity() {


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

        val pathFile = getExternalFilesDir(null).toString() + "/tracks/" + intent.getStringExtra("filename")

        id_map_gpxViewer.controller.zoomTo(10.0)

        lateinit var course: Course
        CoroutineScope(Main).launch {
            course = Course(pathFile)
        }.invokeOnCompletion {
            CoroutineScope(Main).launch {
                drawBlankMap(course, id_map_gpxViewer, loading_course_circle)
            }.invokeOnCompletion {
                val latitudeSpan = abs(course.farNorthPoint().latitude - course.farSouthPoint().latitude)
                val longitudeSpan = abs(course.farEastPoint().longitude - course.farWestPoint().longitude)
                val multiplier = 1//0.40
                id_map_gpxViewer.controller.animateTo(course.centerPoint())
                id_map_gpxViewer.controller.zoomToSpan(latitudeSpan * multiplier, longitudeSpan * multiplier)
            }
        }
        button_blankMap.setOnClickListener {
            CoroutineScope(Main).launch {
                drawBlankMap(course, id_map_gpxViewer, loading_course_circle)
            }
        }

        button_slope.setOnClickListener {
            CoroutineScope(Main).launch {
                drawSlopeMap(course, id_map_gpxViewer, loading_course_circle)
            }
        }

        button_altitude_difference.setOnClickListener {
            CoroutineScope(Main).launch {
                drawAltitudeDiffereceMap(course, id_map_gpxViewer, loading_course_circle)
            }
        }

    }

    suspend fun drawBlankMap(course: Course, map: MapView, loadingCourseCircle: ProgressBar) {
        map.overlayManager.removeAll(map.overlays)

        map.visibility = View.INVISIBLE
        loadingCourseCircle.visibility = View.VISIBLE

        withContext(Default) {
            for (i in 1 until course.geoPoints().size) {
                val seg = Polyline()
                seg.addPoint(course.geoPoints()[i - 1])
                seg.addPoint(course.geoPoints()[i])

                seg.outlinePaint.strokeCap = Paint.Cap.ROUND
                withContext(Main) {
                    map.overlayManager.add(seg)
                }
            }
        }

        map.visibility = View.VISIBLE
        loadingCourseCircle.visibility = View.INVISIBLE
    }


    suspend fun drawAltitudeDiffereceMap(course: Course, map: MapView, loadingCourseCircle: ProgressBar) {
        map.overlayManager.clear()

        val maxAltitude = course.maxAltitude()
        val minAltitude = course.minAltitude()

        map.visibility = View.INVISIBLE
        loadingCourseCircle.visibility = View.VISIBLE

        withContext(Default) {
            for (i in 1 until course.geoPoints().size) {
                val seg = Polyline()
                seg.addPoint(course.geoPoints()[i - 1])
                seg.addPoint(course.geoPoints()[i])

                var colorModifier: Int = (((course.geoPoints()[i].altitude - minAltitude) / (maxAltitude - minAltitude)) * 255.0).toInt()

                if(colorModifier>255)
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

    suspend fun drawSlopeMap(course: Course, map: MapView, loadingCourseCircle: ProgressBar) {
        map.overlayManager.clear()

        map.visibility = View.INVISIBLE
        loadingCourseCircle.visibility = View.VISIBLE

        val precision = 3

        withContext(Default) {
            for (i in 0 until course.geoPoints().size step precision) {
                val seg = Polyline()
                for (j in 0 until precision + 1)
                    if (i + j < course.geoPoints().size)
                        seg.addPoint(course.geoPoints()[i + j])

                val distance = seg.actualPoints.last().distanceToAsDouble(seg.actualPoints.first())
                val altitude = seg.actualPoints.last().altitude - seg.actualPoints.first().altitude
                val slope = altitude / distance * 100

                var colorModifier = ((slope / 100.0) * 255.0 * 3.5).toInt()

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

