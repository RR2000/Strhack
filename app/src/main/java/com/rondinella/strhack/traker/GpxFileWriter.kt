package com.rondinella.strhack.traker

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rondinella.strhack.utils.convertLongToTime
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.lang.Exception
import java.util.*

class GpxFileWriter(context: Context, /*var*/ title: String = "") {
    private val tracksLocation = File(context.getExternalFilesDir(null).toString() + "/tracks")
    private var closed = false
    private lateinit var trackFile: File
    private lateinit var filename: String

    var line = Polyline()

    init {
        try {

            filename = "date."+ convertLongToTime(Date().time).replace(":", ".") + ".title." + title + ".strhack.gpx"
            WrittenFilenameData.setFilename(filename)

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
                <name>${title}</name>
            
            """.trimIndent()
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun addPoint(long: String, lat: String, time: String, alt: String, temp: Double?, hr: Int?) {
        if (!closed) {
            val point = GeoPoint(lat.toDouble(), long.toDouble())

            line.addPoint(point)
            WrittenPolylineData.refreshPolyline(line)

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
        WrittenFilenameData.setFilename(null)
    }

    object WrittenPolylineData: ViewModel() {

        private val polyline =  MutableLiveData<Polyline>()

        fun getPolyline(): LiveData<Polyline>{
            return polyline
        }

        fun refreshPolyline(polyline: Polyline){
            this.polyline.value = polyline
        }
    }

    object WrittenFilenameData: ViewModel(){
        private val filename = MutableLiveData<String?>()

        fun getFilename(): LiveData<String?>{
            return filename
        }

        fun setFilename(filename: String?){
            this.filename.value = filename
        }
    }
}