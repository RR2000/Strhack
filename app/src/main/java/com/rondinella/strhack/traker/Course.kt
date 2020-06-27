package com.rondinella.strhack.traker

import android.util.Log
import android.widget.ProgressBar
import com.example.strhack.AdvancedGeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

class Course(pathFile: String) {
    private var geoPoints = ArrayList<AdvancedGeoPoint>()
    private var gpxFile: File = File(pathFile)

    private var farNorthPoint = GeoPoint(-90.0, 0.0)
    private var farSouthPoint = GeoPoint(90.0,0.0)
    private var farWestPoint = GeoPoint(0.0,180.0)
    private var farEastPoint = GeoPoint(0.0, -180.0)
    private var highestPoint = GeoPoint(0.0,0.0,-5000.0)
    private var lowestPoint = GeoPoint(0.0,0.0,5000.0)
    private lateinit var centralPoint: GeoPoint

    init {
        readPoints()
    }

    fun geoPoints(): ArrayList<AdvancedGeoPoint> {
        return geoPoints
    }

    fun maxAltitude(): Double {
        return highestPoint.altitude
    }

    fun minAltitude(): Double {
        return lowestPoint.altitude
    }

    fun farNorthPoint(): GeoPoint{
        return farNorthPoint
    }

    fun farSouthPoint(): GeoPoint{
        return farSouthPoint
    }
    fun farWestPoint(): GeoPoint{
        return farWestPoint
    }
    fun farEastPoint(): GeoPoint{
        return farEastPoint
    }
    fun centerPoint(): GeoPoint{
        return centralPoint
    }

    private fun readPoints() {
        CoroutineScope(Main).launch {
            val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(gpxFile)
            xmlDoc.documentElement.normalize()

            val trackPointList: NodeList = xmlDoc.getElementsByTagName("trkpt")


            for (i in 0 until trackPointList.length) {
                val point: Node = trackPointList.item(i)

                var altitude = 0.0
                var date = Date()
                val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ITALIAN)

                for (j in 1 until point.childNodes.length) {
                    if (point.childNodes.item(j).nodeName == "ele") {
                        altitude = point.childNodes.item(j).textContent.toDouble()
                    }
                    if (point.childNodes.item(j).nodeName == "time")
                        date = formatter.parse(point.childNodes.item(j).textContent)!!
                }

                geoPoints.add(
                    AdvancedGeoPoint(
                        date,
                        point.attributes.getNamedItem("lat").nodeValue.toDouble(),
                        point.attributes.getNamedItem("lon").nodeValue.toDouble(),
                        altitude
                    )
                )

                val lastGeoPoint = geoPoints.last()

                if(lastGeoPoint.latitude > farNorthPoint.latitude)
                    farNorthPoint = lastGeoPoint
                if(lastGeoPoint.latitude < farSouthPoint.latitude)
                    farSouthPoint = lastGeoPoint
                if(lastGeoPoint.longitude > farEastPoint.longitude)
                    farEastPoint = lastGeoPoint
                if(lastGeoPoint.longitude < farWestPoint.longitude)
                    farWestPoint = lastGeoPoint
                if (lastGeoPoint.altitude < lowestPoint.altitude)
                    lowestPoint = lastGeoPoint
                if (lastGeoPoint.altitude > highestPoint.altitude)
                    highestPoint = lastGeoPoint

            }

            Log.w("NORD",farNorthPoint.toDoubleString())
            Log.w("EST",farEastPoint.toDoubleString())
            Log.w("SUD",farSouthPoint.toDoubleString())
            Log.w("OVEST",farWestPoint.toDoubleString())

            centralPoint = GeoPoint((farNorthPoint.latitude+farSouthPoint.latitude)/2.0,(farEastPoint.longitude+farWestPoint.longitude)/2.0)

            Log.w("CENTRAL", centralPoint.toDoubleString())
        }

    }
}