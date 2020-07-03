package com.rondinella.strhack.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.rondinella.strhack.R
import kotlinx.android.synthetic.main.activity_course_editor.*
import java.io.File

class CourseEditorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_editor)

        val pathFile = getExternalFilesDir(null).toString() + "/tracks/" + intent.getStringExtra("filename")!!

        val title = pathFile.substringAfter(".title.").substringBefore(".strhack.gpx")

        val courseFile = File(pathFile)

        title_edit.setText(title)

        button_confirm_edit.setOnClickListener {
            val editedTitle = title_edit.text.toString()
            val newFilename = courseFile.name.replace(".title.$title", ".title.$editedTitle")

            val newFile = File(getExternalFilesDir(null).toString() + "/tracks/" + newFilename)

            if(!newFile.exists())
                newFile.createNewFile()

            courseFile.renameTo(newFile)

            setResult(0)
            finishActivity(42)
            finish()
        }
    }
}
