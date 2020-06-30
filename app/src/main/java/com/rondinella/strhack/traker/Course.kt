package com.rondinella.strhack.traker

import android.util.Log
import android.widget.Toast
import com.example.strhack.AdvancedGeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import java.io.File
import java.io.StringReader
import java.sql.Time
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

class Course() {
    private var geoPoints = ArrayList<AdvancedGeoPoint>()
    private lateinit var gpxFile: File
    private var courseName: String = ""

    private var farNorthPoint = -90.0
    private var farSouthPoint = 90.0
    private var farWestPoint = 180.0
    private var farEastPoint = -180.0
    private var highestPoint = GeoPoint(0.0, 0.0, -5000.0)
    private var lowestPoint = GeoPoint(0.0, 0.0, 5000.0)
    private lateinit var centralPoint: GeoPoint

    constructor(text: String) : this() {
        readPoints(text)
    }

    constructor(gpxFile: File) : this() {
        this.gpxFile = gpxFile
        readPoints(gpxFile)
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

    fun centralPoint(): GeoPoint {
        return centralPoint
    }

    fun courseName(): String {
        return courseName
    }

    fun boundingBox(): BoundingBox{
        val padding = 0.0007
        return BoundingBox(farNorthPoint + padding, farEastPoint + padding, farSouthPoint - padding, farWestPoint - padding)
    }

    private fun readPoints(string: String){
        readPoints(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(string))))
    }

    private fun readPoints(file: File){
        try {
            readPoints(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file))
        }catch (e: SAXParseException){
            Log.w("MALE", "Qualcosa è storto")
        }
    }

    private fun readPoints(xmlDoc: Document) {
        xmlDoc.documentElement.normalize()

        CoroutineScope(Main).launch {
            val trackPointList: NodeList = xmlDoc.getElementsByTagName("trkpt")

            for (i in 0 until trackPointList.length) {
                val point: Node = trackPointList.item(i)

                var altitude = 0.0
                var date = Date()
                var formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ITALIAN)

                for (j in 1 until point.childNodes.length) {
                    if (point.childNodes.item(j).nodeName == "ele") {
                        altitude = point.childNodes.item(j).textContent.toDouble()
                    }
                    if (point.childNodes.item(j).nodeName == "time")
                        try {
                            date = formatter.parse(point.childNodes.item(j).textContent)!!
                        }catch (e: ParseException){
                            formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ITALIAN)
                            date = formatter.parse(point.childNodes.item(j).textContent)!!
                        }

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

                farNorthPoint = if(lastGeoPoint.latitude > farNorthPoint) lastGeoPoint.latitude else farNorthPoint
                farSouthPoint = if(lastGeoPoint.latitude < farSouthPoint) lastGeoPoint.latitude else farSouthPoint
                farEastPoint = if(lastGeoPoint.longitude > farEastPoint) lastGeoPoint.longitude else farEastPoint
                farWestPoint = if(lastGeoPoint.longitude < farWestPoint) lastGeoPoint.longitude else farWestPoint

                if (lastGeoPoint.altitude < lowestPoint.altitude)
                    lowestPoint = lastGeoPoint
                if (lastGeoPoint.altitude > highestPoint.altitude)
                    highestPoint = lastGeoPoint
            }
        }.invokeOnCompletion {
            val latitudeMid = (farNorthPoint + farSouthPoint) / 2.0
            val longitudeMid = (farEastPoint + farWestPoint) / 2.0
            centralPoint = GeoPoint(latitudeMid, longitudeMid)

            courseName = if (xmlDoc.getElementsByTagName("name").length > 0)
                xmlDoc.getElementsByTagName("name").item(0).textContent.toString()
            else
                DateFormat.getDateInstance(DateFormat.FULL).format(geoPoints.first().date)
        }

    }

}
