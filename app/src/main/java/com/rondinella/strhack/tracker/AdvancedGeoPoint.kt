package com.rondinella.strhack.tracker

import org.osmdroid.util.GeoPoint
import java.util.*

class AdvancedGeoPoint(var date: Date, latitude: Double, longitude: Double, altitude: Double) : GeoPoint(latitude, longitude, altitude) {
}
