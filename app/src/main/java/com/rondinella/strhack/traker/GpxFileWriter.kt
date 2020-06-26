package com.example.strhack

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import com.rondinella.strhack.livedata.currentTrackPositionData
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.lang.Exception
import java.util.*

class GpxFileWriter(context: Context) {
    var TRACKS_LOCATION = File(context.getExternalFilesDir(null).toString() + "/tracks")
    var closed = false
    lateinit var trackFile: File
    lateinit var filename: String

    var line = Polyline()

    init {
        try {
           filename = convertLongToTime(Date().time) + ".gpx"

            while(!TRACKS_LOCATION.exists()){ //SECONDO ME CI VA UN IF
                TRACKS_LOCATION.mkdir()
            }
            File(TRACKS_LOCATION, filename).createNewFile()
            trackFile = File(TRACKS_LOCATION, filename) //VORREI FARE UN FILE PARZIALE

            trackFile.appendText(
                """
            <gpx xmlns="http://www.topografix.com/GPX/1/1" xmlns:gpxx="http://www.garmin.com/xmlschemas/GpxExtensions/v3" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1" creator="StrHack" version="1.1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd">
            <metadata>
                <time>${convertLongToTime(Date().time)}</time>
            </metadata>
            <trk>
                <name>StrHack Track</name>
            
            """.trimIndent()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun addPoint(long: String, lat: String, time: String, alt: String){
        if(!closed) {
            val point = GeoPoint(lat.toDouble(),long.toDouble())

            currentTrackPositionData.changeCurrentPosition(point)
            //map_view.overlayManager.remove(line)

            line.addPoint(point)

            //map_view.overlayManager.add(line)

            trackFile.appendText(
                """
            <trkpt lat="${lat}" lon="${long}">
                <ele>${alt}</ele>
                <time>${convertLongToTime(time.toLong())}</time>
            </trkpt>
            
        """.trimIndent()
            )
        }
    }

    fun close(){
        closed = true
        //map_view.overlayManager.remove(line)
        trackFile.appendText("</trk>\n</gpx>")
    }
}