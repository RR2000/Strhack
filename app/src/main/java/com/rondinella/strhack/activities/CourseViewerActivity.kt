package com.rondinella.strhack.activities

import android.graphics.Color
import android.graphics.Paint
import android.opengl.Visibility
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import androidx.preference.PreferenceManager
import com.example.strhack.readAdvencedGeoPoints
import com.rondinella.strhack.R
import kotlinx.android.synthetic.main.activity_course_viewer.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.io.File

@Suppress("DEPRECATION")
class CourseViewerActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_viewer)

        id_map_gpxViewer.visibility = View.INVISIBLE

        Configuration.getInstance().load(applicationContext,PreferenceManager.getDefaultSharedPreferences(applicationContext))

        //It removes standard button in order to use touch controls
        id_map_gpxViewer.setBuiltInZoomControls(false)
        id_map_gpxViewer.setMultiTouchControls(true)
        val overlayRotation = RotationGestureOverlay(this, id_map_gpxViewer).apply { isEnabled = true }
        id_map_gpxViewer.overlays.add(overlayRotation)

        CoroutineScope(Main).launch {
            drawMap(intent.getStringExtra("filename"), progressBar_couseViewer)
        }
    }

    suspend fun drawMap(itemName: String?, loadingProgressBar: ProgressBar) {
        val mapController: IMapController = id_map_gpxViewer.controller
        id_map_gpxViewer.overlayManager.removeAll(id_map_gpxViewer.overlays)

        withContext(IO) {
            val geoPoints = readAdvencedGeoPoints(File(getExternalFilesDir(null).toString() + "/tracks/" + itemName))

            loadingProgressBar.max = geoPoints.size

            withContext(Default) {
                for (i in 1 until geoPoints.size) {
                    val seg = Polyline()
                    seg.addPoint(geoPoints[i - 1])
                    seg.addPoint(geoPoints[i])

                    val coloreDaUsare: Int = (((255) * (geoPoints[i - 1].altitude / 1500)).toInt())

                    seg.outlinePaint.color = Color.rgb(coloreDaUsare, 255 - coloreDaUsare, 0)
                    seg.outlinePaint.strokeCap = Paint.Cap.ROUND
                    withContext(Main) {
                        id_map_gpxViewer.overlayManager.add(seg)
                        loadingProgressBar.progress = i
                    }
                }
                withContext(Main) {
                    mapController.setZoom(20.0)
                    mapController.animateTo(geoPoints[0])

                    loadingProgressBar.visibility = View.INVISIBLE
                    id_map_gpxViewer.visibility = View.VISIBLE
                }
            }
        }
    }
}
