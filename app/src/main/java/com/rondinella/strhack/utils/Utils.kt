package com.example.strhack

import org.osmdroid.util.GeoPoint
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory


fun convertLongToTime(time: Long): String {
    val date = Date(time)
    val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ITALIAN)
    return formatter.format(date)
}

//It converts a GPX file into a list of GeoPoints
fun readGeoPoints(file: File): List<GeoPoint> {
    val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    xmlDoc.documentElement.normalize()

    val trackPointList: NodeList = xmlDoc.getElementsByTagName("trkpt")
    val mMap = ArrayList<GeoPoint>()

    for (i in 0 until trackPointList.length) {
        val point: Node = trackPointList.item(i)

        var altitude = 1500.0

        if(point.childNodes.item(1).nodeName == "ele")
            altitude = point.childNodes.item(1).textContent.toDouble()

        mMap.add(
            GeoPoint(
                point.attributes.getNamedItem("lat").nodeValue.toDouble(),
                point.attributes.getNamedItem("lon").nodeValue.toDouble(),
                altitude
            )
        )
    }
    return mMap
}

//It converts a GPX file into a list of AdvancedGeoPoints
fun readAdvencedGeoPoints(file: File): List<AdvancedGeoPoint> {
    val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    xmlDoc.documentElement.normalize()

    val trackPointList: NodeList = xmlDoc.getElementsByTagName("trkpt")
    val mMap = ArrayList<AdvancedGeoPoint>()

    for (i in 0 until trackPointList.length) {
        val point: Node = trackPointList.item(i)

        var altitude = 1500.0
        var date  = Date()
        val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ITALIAN)

        for (j in 1 until point.childNodes.length){
            if(point.childNodes.item(j).nodeName == "ele")
                altitude = point.childNodes.item(j).textContent.toDouble()
            if(point.childNodes.item(j).nodeName == "time")
                date = formatter.parse(point.childNodes.item(j).textContent)!!
        }


        mMap.add(
            AdvancedGeoPoint(
                date,
                point.attributes.getNamedItem("lat").nodeValue.toDouble(),
                point.attributes.getNamedItem("lon").nodeValue.toDouble(),
                altitude
            )
        )
    }
    return mMap
}