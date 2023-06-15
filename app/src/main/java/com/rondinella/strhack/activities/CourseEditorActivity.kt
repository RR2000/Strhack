package com.rondinella.strhack.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.rondinella.strhack.tracker.AdvancedGeoPoint
import com.rondinella.strhack.R
import com.rondinella.strhack.databinding.ActivityCourseEditorBinding
import com.rondinella.strhack.tracker.Course
import com.rondinella.strhack.utils.convertLongToTime
import com.rondinella.strhack.utils.writePointsOnFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.ArrayList

class CourseEditorActivity : AppCompatActivity() {

    private var _binding: ActivityCourseEditorBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityCourseEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pathFile = applicationContext.filesDir.absolutePath + "/tracks/" + intent.getStringExtra("filename")!!

        val title = pathFile.substringAfter(".title.").substringBefore(".strhack.gpx")

        val courseFile = File(pathFile)

        binding.titleEdit.setText(title)

        binding.buttonConfirmEdit.setOnClickListener {
            val editedTitle: String = binding.titleEdit.text.toString()
            val newFilename: String = courseFile.name.replace(".title.$title", ".title.$editedTitle")

            val newFile = File(applicationContext.filesDir.absolutePath + "/tracks/" + newFilename)

            if (!newFile.exists()) {
                newFile.createNewFile()
                if (binding.checkDeletePastrocchi.isChecked) {
                    initDeletePastrocchi(courseFile, 1)
                }

                courseFile.renameTo(newFile)
                //courseFile.copyTo(newFile)//TODO tester, i don't want to lose files

                setResult(0)
                finishActivity(42)
                finish()
            } else {
                //TODO localize, should never happens but who knows
                Toast.makeText(this, "Name already in use", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initDeletePastrocchi(courseFile: File, precision: Int) {
        CoroutineScope(IO).launch {
            val course = Course(courseFile)
            delay(5000)
            deletePastrocchi(course.geoPoints(), precision)
        }
    }

    private fun deletePastrocchi(geoPoints: ArrayList<AdvancedGeoPoint>, precision: Int) {

        val initialSize = geoPoints.size

        CoroutineScope(IO).launch {

            Log.w("SIZE AT BEGIN", geoPoints.size.toString())

            var centralPoint = precision

            while (centralPoint < geoPoints.size) {
                var merge = true
                for (i in centralPoint - precision until centralPoint + precision) {
                    if (i + precision < geoPoints.size - 1) {
                        if (geoPoints[centralPoint].distanceToAsDouble(geoPoints[i]) >= 2.5) {
                            merge = false
                            break
                        }
                    }
                }
                if (merge) {
                    for (i in centralPoint - precision until centralPoint + precision) {
                        if (i + precision < geoPoints.size - 1)
                            if (i != precision)
                                geoPoints.removeAt(i)
                    }
                    centralPoint += precision
                } else {
                    centralPoint++
                }

            }

            if (initialSize == geoPoints.size) {
                writePointsOnFile(
                    geoPoints,
                    applicationContext.applicationContext.filesDir.absolutePath + "/tracks/date.${
                        convertLongToTime(geoPoints[0].date.time).replace(
                            ":",
                            "."
                        )
                    }.title.opt.strhack.gpx"
                )
                Log.w("FINISHED", geoPoints.size.toString())
            } else
                deletePastrocchi(geoPoints, precision)

            Log.w("SIZE AT FINISH", geoPoints.size.toString())
        }
    }
}
