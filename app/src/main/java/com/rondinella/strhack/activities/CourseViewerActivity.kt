package com.rondinella.strhack.activities

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import java.io.File

class CourseViewerActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_viewer)

        Configuration.getInstance().load(applicationContext,PreferenceManager.getDefaultSharedPreferences(applicationContext))

        CoroutineScope(Main).launch {
            drawMap(intent.getStringExtra("filename"))
        }
    }

    suspend fun drawMap(itemName: String?) {
        val mapController: IMapController = id_map_gpxViewer.controller
        id_map_gpxViewer.overlayManager.removeAll(id_map_gpxViewer.overlays)

        withContext(IO) {
            val geoPoints = readAdvencedGeoPoints(File(getExternalFilesDir(null).toString() + "/tracks/" + itemName))

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
                    }
                }
                withContext(Main) {
                    mapController.setZoom(20.0)
                    mapController.animateTo(geoPoints[0])
                }
            }
        }
    }
}
