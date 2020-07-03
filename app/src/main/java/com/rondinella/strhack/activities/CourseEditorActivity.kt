package com.rondinella.strhack.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.rondinella.strhack.R
import java.io.File

class CourseEditorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_editor)

        //val pathFile = getExternalFilesDir(null).toString() + "/tracks/" + intent.getStringExtra("filename")!!

        //val title = "Bel giro"

        //val courseFile = File(pathFile)

        //val editedFile = File(pathFile.replace(".title.", ".title.$title"))
        //if(!editedFile.exists())
            //editedFile.createNewFile()

        //val bool = courseFile.renameTo(editedFile)

        //Toast.makeText(this, bool.toString(), Toast.LENGTH_LONG).show()
    }
}
