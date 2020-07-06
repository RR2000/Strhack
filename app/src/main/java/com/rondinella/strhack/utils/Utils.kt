package com.rondinella.strhack.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.strhack.AdvancedGeoPoint
import com.rondinella.strhack.R
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


private fun permissionList(): ArrayList<String> {
    val permissionsArray = arrayListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        permissionsArray.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    permissionsArray.add(Manifest.permission.ACCESS_FINE_LOCATION)
    permissionsArray.add(Manifest.permission.INTERNET)
    permissionsArray.add(Manifest.permission.ACCESS_NETWORK_STATE)
    permissionsArray.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    permissionsArray.add(Manifest.permission.READ_EXTERNAL_STORAGE)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        permissionsArray.add(Manifest.permission.FOREGROUND_SERVICE)

    return permissionsArray
}

fun hasPermissions(activity: Activity): Boolean {
    val permissionsArray = permissionList()

    for (i in 0 until permissionsArray.size) {
        if (ActivityCompat.checkSelfPermission(activity, permissionsArray[i]) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

    }

    return true
}

fun askPermissions(activity: Activity) {
    val permissionsArray = permissionList()

    var i = 0
    while (i < permissionsArray.size) {
        if (ActivityCompat.checkSelfPermission(activity, permissionsArray[i]) == PackageManager.PERMISSION_GRANTED)
            permissionsArray.remove(permissionsArray[i])
        i++
    }

    if (permissionsArray.size > 0)
        ActivityCompat.requestPermissions(activity, permissionsArray.toTypedArray(), 36)

    if (!hasPermissions(activity)) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(Intent(intent))
        Toast.makeText(activity.applicationContext, activity.getString(R.string.permits_error), Toast.LENGTH_LONG).show()
    }
}


fun convertLongToTime(time: Long): String {
    val date = Date(time)
    val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ITALIAN)
    return formatter.format(date)
}

fun convertStringFilenameDateToTitle(date: String, course_of: String, at_time: String): String {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss'Z'", Locale.ITALIAN)
    val formatter = SimpleDateFormat("'${course_of} 'dd MMMM yyyy '${at_time}' HH:mm", Locale.ITALIAN)
    return formatter.format(parser.parse(date)!!)
}

fun convertStringFilenameDateToDate(date: String): String {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss'Z'", Locale.ITALIAN)
    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ITALIAN)
    return formatter.format(parser.parse(date)!!)
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

        if (point.childNodes.item(1).nodeName == "ele")
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

fun writePointsOnFile(points: ArrayList<AdvancedGeoPoint>, filePath: String){

    val file = File(filePath)

    if (!file.exists())
        file.createNewFile()
    else{
        file.delete()
        file.createNewFile()
    }

    file.appendText(
        """
            <gpx xmlns="http://www.topografix.com/GPX/1/1" xmlns:gpxx="http://www.garmin.com/xmlschemas/GpxExtensions/v3" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1" creator="StrHack" version="1.1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd">
            <metadata>
                <time>${convertLongToTime(Date().time)}</time>
            </metadata>
            <trk>
                <name>Test</name>
            
            """.trimIndent()
    )

    for(point in points) {

        file.appendText(
            """
            <trkpt lat="${point.latitude}" lon="${point.longitude}">
                <ele>${point.altitude}</ele>
                <time>${convertLongToTime(point.date.time)}</time>
                
                """.trimIndent()
        )

        file.appendText("\n</trkpt>\n")
    }

    file.appendText("</trk>\n</gpx>")
}

//It converts a GPX file into a list of AdvancedGeoPoints
fun readAdvencedGeoPoints(file: File): ArrayList<AdvancedGeoPoint> {
    val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    xmlDoc.documentElement.normalize()

    val trackPointList: NodeList = xmlDoc.getElementsByTagName("trkpt")
    val mMap = ArrayList<AdvancedGeoPoint>()

    for (i in 0 until trackPointList.length) {
        val point: Node = trackPointList.item(i)

        var altitude = 1500.0
        var date = Date()
        val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ITALIAN)

        for (j in 1 until point.childNodes.length) {
            if (point.childNodes.item(j).nodeName == "ele")
                altitude = point.childNodes.item(j).textContent.toDouble()
            if (point.childNodes.item(j).nodeName == "time")
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