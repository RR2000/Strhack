package com.rondinella.strhack.ui.viewer

import android.animation.ArgbEvaluator
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rondinella.strhack.tracker.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.views.overlay.Polyline
import java.io.*

class CourseViewerViewModel : ViewModel() {
    // LiveData for Course
    private val _course = MutableLiveData<Course>()
    val course: LiveData<Course> = _course

    // LiveData for segments
    private val _segments = MutableLiveData<List<Polyline>>()
    val segments: LiveData<List<Polyline>> = _segments

    fun loadCourse(file: File) {
        viewModelScope.launch {
            val course = Course()
            course.initializeWithFile(file)
            _course.postValue(course)
        }
    }

    fun correctAltitude() {
        viewModelScope.launch(Dispatchers.IO) {
            val course = _course.value ?: return@launch
            course.correctElevation()
            _course.postValue(course)
        }
    }

    fun drawBlankMap() {
        Log.d("DEBUG", "drawBlankMap started")

        viewModelScope.launch(Dispatchers.IO) {
            val segments = course.value?.getPoints()?.windowed(2, 1)?.map { (start, end) ->
                val seg = Polyline().apply {
                    addPoint(start)
                    addPoint(end)
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
                seg
            } ?: emptyList()

            withContext(Dispatchers.Main) {
                _segments.postValue(segments)
            }
        }
    }

    fun drawAltitudeDifferenceMap() {
        viewModelScope.launch(Dispatchers.IO) {
            val course = _course.value ?: return@launch
            val maxAltitude = course.getPoints().maxByOrNull { it.altitude }?.altitude ?: 0.0
            val minAltitude = course.getPoints().minByOrNull { it.altitude }?.altitude ?: 0.0

            val colorInterpolator = ArgbEvaluator()

            val segments = course.getPoints().windowed(2, 1).mapNotNull { (start, end) ->
                val altitudeRatio = ((end.altitude - minAltitude) / (maxAltitude - minAltitude)).toFloat().coerceIn(0f, 1f)
                val color = colorInterpolator.evaluate(altitudeRatio, Color.GREEN, Color.BLACK) as Int

                Polyline().apply {
                    addPoint(start)
                    addPoint(end)
                    outlinePaint.color = color
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
            }

            _segments.postValue(segments)
        }
    }

    fun drawSlopeMap() {
        viewModelScope.launch(Dispatchers.IO) {
            val course = _course.value ?: return@launch
            val uphillColorInterpolator = ArgbEvaluator()
            val downhillColorInterpolator = ArgbEvaluator()

            val slopes = course.getPoints().windowed(2, 1).map { (start, end) ->
                val distance = end.distanceToAsDouble(start)
                val altitude = end.altitude - start.altitude
                altitude / distance / 0.13
            }

            val smoothedSlopes = slopes.windowed(10, 1, true) { it.average() }

            val segments = course.getPoints().zip(smoothedSlopes).windowed(2, 1).mapNotNull { (pointWithSlope1, pointWithSlope2) ->
                val (point1, slope1) = pointWithSlope1
                val (point2, _) = pointWithSlope2

                val color = when {
                    slope1 > 0 -> {  // Uphill
                        val slopeRatio = slope1.coerceIn(0.0, 1.0).toFloat()
                        uphillColorInterpolator.evaluate(slopeRatio, Color.GREEN, Color.RED) as Int
                    }
                    else -> {  // Downhill
                        val slopeRatio = (-slope1).coerceIn(0.0, 1.0).toFloat()
                        downhillColorInterpolator.evaluate(slopeRatio, Color.GREEN, Color.BLUE) as Int
                    }
                }

                Polyline().apply {
                    addPoint(point1)
                    addPoint(point2)
                    outlinePaint.color = color
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
            }

            _segments.postValue(segments)
        }
    }

    fun drawSpeedMap() {
        viewModelScope.launch(Dispatchers.IO) {
            val course = _course.value ?: return@launch
            val speedColorInterpolator = ArgbEvaluator()

            val speeds = course.getPoints().windowed(2, 1).map { (start, end) ->
                val distance = end.distanceToAsDouble(start)
                val time = (end.date.time - start.date.time) / 1000
                distance / time  // Assuming time is in seconds and distance in meters
            }

            val smoothedSpeeds = speeds.windowed(10, 1, true) { it.average() }

            val maxSpeed = smoothedSpeeds.max()
            val minSpeed = smoothedSpeeds.minOrNull() ?: 0.0

            val segments = course.getPoints().zip(smoothedSpeeds).windowed(2, 1).map { (pointWithSpeed1, pointWithSpeed2) ->
                val (point1, speed1) = pointWithSpeed1
                val (point2, _) = pointWithSpeed2

                val speedRatio = ((speed1 - minSpeed) / (maxSpeed - minSpeed)).toFloat().coerceIn(0f, 1f)
                val color = speedColorInterpolator.evaluate(speedRatio, Color.WHITE, Color.RED) as Int

                Polyline().apply {
                    addPoint(point1)
                    addPoint(point2)
                    outlinePaint.color = color
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
            }
            _segments.postValue(segments)
        }
    }

}