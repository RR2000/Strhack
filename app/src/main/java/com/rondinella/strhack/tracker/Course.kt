package com.rondinella.strhack.tracker

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import org.json.JSONObject
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.StringReader
import java.lang.Double.min
import java.lang.Integer.max
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList
import kotlin.math.*

@Suppress("UNCHECKED_CAST")
class Course() {
    private var geoPoints = ArrayList<AdvancedGeoPoint>()
    private lateinit var gpxFile: File

    private var farNorthPoint = -90.0
    private var farSouthPoint = 90.0
    private var farWestPoint = 180.0
    private var farEastPoint = -180.0
    private var highestPoint = GeoPoint(0.0, 0.0, -5000.0)
    private var lowestPoint = GeoPoint(0.0, 0.0, 5000.0)
    private var centralPoint: GeoPoint = GeoPoint(0.0, 0.0, 0.0)

    var distance: Double = 0.0

    init {

    }

    suspend fun initializeWithText(text: String) = withContext(Dispatchers.IO) {
        readPoints(text)
    }

    suspend fun initializeWithFile(gpxFile: File) = withContext(Dispatchers.IO) {
        readPoints(gpxFile)
    }

    fun geoPoints(): ArrayList<AdvancedGeoPoint> {
        return geoPoints
    }

    suspend fun getElevationFromAPI(points: List<GeoPoint>): List<Double> = withContext(Dispatchers.IO) {
        val batchSize = 100
        val elevations = mutableListOf<Double>()

        for (i in points.indices step batchSize) {
            val end = min(i + batchSize, points.size)
            val batch = points.subList(i, end)

            val locations = batch.joinToString("|") { "${it.latitude},${it.longitude}" }
            val url = URL("https://api.opentopodata.org/v1/eudem25m?locations=$locations")
            val connection = url.openConnection() as HttpURLConnection

            try {
                BufferedReader(InputStreamReader(connection.inputStream)).use {
                    val response = it.readText()
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "OK") {
                        val results = jsonObject.getJSONArray("results")
                        for (j in 0 until results.length()) {
                            val result = results.getJSONObject(j)
                            (if (result.isNull("elevation")) null else result.getDouble("elevation"))?.let { it1 -> elevations.add(it1) }
                        }
                    } else {
                        throw RuntimeException("API response status not OK")
                    }
                }
            } finally {
                connection.disconnect()
            }
        }

        return@withContext elevations
    }

    suspend fun ArrayList<AdvancedGeoPoint>.correctAltitude(): ArrayList<AdvancedGeoPoint> = withContext(Dispatchers.IO) {
        val deferred = async(Dispatchers.IO) {
            getElevationFromAPI(this@correctAltitude)
        }

        val correctedAltitudes = deferred.await()
        ArrayList(this@correctAltitude.zip(correctedAltitudes) { point, altitude ->
            AdvancedGeoPoint(point.date, point.latitude, point.longitude, altitude)
        })
    }

    suspend fun getPointEveryMeters(distanceMeters: Double): ArrayList<AdvancedGeoPoint> {
        if (geoPoints.isEmpty()) {
            return arrayListOf()
        }

        val geoPointsResult = ArrayList<AdvancedGeoPoint>()
        var totalDistance = 0.0

        geoPointsResult.add(geoPoints.first()) // add the first point

        for (i in 1 until geoPoints.size) {
            totalDistance += geoPoints[i].distanceToAsDouble(geoPoints[i - 1])

            if (totalDistance >= distanceMeters) {
                geoPointsResult.add(geoPoints[i])
                totalDistance = 0.0
            }
        }

        Log.w("ORIGINAL SIZE", geoPoints.size.toString())
        Log.w("FINAL SIZE", geoPointsResult.size.toString())

        return geoPointsResult.correctAltitude()
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

    fun getDistance(precision: Int = 2): Double {
        val precisionDouble = 10.0.pow(precision.toDouble())
        return round((distance / 1000) * precisionDouble) / precisionDouble
    }

    fun getAverageSpeed(precision: Int = 2): Double {
        val precisionDouble = 10.0.pow(precision.toDouble())
        val firstPoint: Date = geoPoints.first().date
        val lastPoint: Date = geoPoints.last().date

        val totalTime = ((lastPoint.time - firstPoint.time) / 1000.0) / 3600.0 // Time difference in hours
        val totalDistance = distance / 1000.0 // Convert distance to kilometers

        return if (totalTime != 0.0) {
            round((totalDistance / totalTime) * precisionDouble) / precisionDouble
        } else {
            0.0
        }
    }

    suspend fun correctAltitude() = withContext(Dispatchers.IO) {
        geoPoints = geoPoints.correctAltitude()
        processGeoPoints()
    }

    fun getTotalElevationGain(precision: Int = 2): Double {
        var totalElevationGain = 0.0
        for (i in 1 until geoPoints.size) {
            val altitudeDifference = geoPoints[i].altitude - geoPoints[i - 1].altitude
            if (altitudeDifference > 0) {
                totalElevationGain += altitudeDifference
            }
        }

        val precisionDouble = 10.0.pow(precision.toDouble())
        return round(totalElevationGain * precisionDouble) / precisionDouble
    }

    fun getTotalTime(): String {
        val firstPointTime: Date = geoPoints.first().date
        val lastPointTime: Date = geoPoints.last().date

        val totalTimeMillis = lastPointTime.time - firstPointTime.time

        val hours = TimeUnit.MILLISECONDS.toHours(totalTimeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTimeMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(totalTimeMillis) % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }


    fun boundingBox(): BoundingBox {
        val padding = 0.0007
        return BoundingBox(farNorthPoint + padding, farEastPoint + padding, farSouthPoint - padding, farWestPoint - padding)
    }

    private suspend fun readPoints(file: File) = withContext(Dispatchers.IO) {
        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            readPoints(doc)
        } catch (e: SAXParseException) {
            // Handle error appropriately
            e.printStackTrace()
        }
    }

    private suspend fun readPoints(string: String) = withContext(Dispatchers.IO) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(string)))
        readPoints(doc)
    }

    private suspend fun readPoints(xmlDoc: Document) = withContext(Dispatchers.IO) {
        xmlDoc.documentElement.normalize()

        val trackPointList: NodeList = xmlDoc.getElementsByTagName("trkpt")
        val formats = listOf("yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        val formatter = SimpleDateFormat("", Locale.ITALIAN)

        for (i in 0 until trackPointList.length) {
            val point: Node = trackPointList.item(i)
            val children = point.childNodes
            var altitude = 0.0
            var date = Date()

            for (j in 1 until children.length) {
                when (children.item(j).nodeName) {
                    "ele" -> altitude = children.item(j).textContent.toDouble()
                    "time" -> {
                        for (format in formats) {
                            formatter.applyPattern(format)
                            try {
                                date = formatter.parse(children.item(j).textContent)!!
                                break
                            } catch (e: ParseException) {
                                continue
                            }
                        }
                    }
                }
            }

            val newPoint = AdvancedGeoPoint(
                date,
                point.attributes.getNamedItem("lat").nodeValue.toDouble(),
                point.attributes.getNamedItem("lon").nodeValue.toDouble(),
                altitude
            )
            geoPoints.add(newPoint)
        }
        // After parsing the XML, process the points
        processGeoPoints()
    }


    private fun processGeoPoints() {
        geoPoints.forEach { point ->
            distance += if (geoPoints.last() != point)
                point.distanceToAsDouble(geoPoints[geoPoints.indexOf(point) + 1])
            else 0.0

            farNorthPoint = max(farNorthPoint, point.latitude)
            farSouthPoint = min(farSouthPoint, point.latitude)
            farEastPoint = max(farEastPoint, point.longitude)
            farWestPoint = min(farWestPoint, point.longitude)

            if (point.altitude < lowestPoint.altitude) lowestPoint = point
            if (point.altitude > highestPoint.altitude) highestPoint = point
        }

        val latitudeMid = (farNorthPoint + farSouthPoint) / 2.0
        val longitudeMid = (farEastPoint + farWestPoint) / 2.0
        centralPoint = GeoPoint(latitudeMid, longitudeMid)
    }


}
