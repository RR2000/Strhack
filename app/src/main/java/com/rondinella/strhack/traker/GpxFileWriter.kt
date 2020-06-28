package com.rondinella.strhack.traker

import android.content.Context
import android.util.Log
import com.example.strhack.convertLongToTime
import com.rondinella.strhack.livedata.currentTrackPositionData
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.lang.Exception
import java.util.*

class GpxFileWriter(context: Context) {
    private val tracksLocation = File(context.getExternalFilesDir(null).toString() + "/tracks")
    private var closed = false
    private lateinit var trackFile: File
    private lateinit var filename: String

    var line = Polyline()

    init {
        try {
            filename = convertLongToTime(Date().time).replace(":", ".") + ".gpx"
            Log.w("FILENAME", filename)

            while (!tracksLocation.exists()) { //SECONDO ME CI VA UN IF
                tracksLocation.mkdir()
            }

            File(tracksLocation, filename).createNewFile()
            trackFile = File(tracksLocation, filename) //VORREI FARE UN FILE PARZIALE

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

    fun addPoint(long: String, lat: String, time: String, alt: String, temp: Double?, hr: Int?) {
        if (!closed) {
            val point = GeoPoint(lat.toDouble(), long.toDouble())

            currentTrackPositionData.changeCurrentPosition(point)
            line.addPoint(point)


            trackFile.appendText(
                """
            <trkpt lat="$lat" lon="$long">
                <ele>${alt}</ele>
                <time>${convertLongToTime(time.toLong())}</time>
                
                """.trimIndent()
            )

            if(temp != null || hr != null)
            {
                trackFile.appendText("<extensions>\n\t\t<gpxtpx:TrackPointExtension>\n")
                if(temp != null)
                    trackFile.appendText("\t\t\t<gpxtpx:atemp>$temp</gpxtpx:atemp>\n")
                if(hr != null)
                    trackFile.appendText("\t\t\t<gpxtpx:hr>$hr</gpxtpx:hr>\n")

                trackFile.appendText("\t\t</gpxtpx:TrackPointExtension>\n\t</extensions>")
            }

            trackFile.appendText("\n</trkpt>\n")
        }
    }

    fun close() {
        closed = true
        //map_view.overlayManager.remove(line)
        trackFile.appendText("</trk>\n</gpx>")
    }
}