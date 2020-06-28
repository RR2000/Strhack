package com.rondinella.strhack.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.rondinella.strhack.R
import com.rondinella.strhack.ui.main.SectionsPagerAdapter


class MainActivity : AppCompatActivity() {

    private fun managePermissions() {
        val permissionsArray = arrayListOf<String>()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            permissionsArray.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        permissionsArray.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionsArray.add(Manifest.permission.INTERNET)
        permissionsArray.add(Manifest.permission.ACCESS_NETWORK_STATE)
        permissionsArray.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissionsArray.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            permissionsArray.add(Manifest.permission.FOREGROUND_SERVICE)

        var i = 0
        while(i < permissionsArray.size) {
            if (ActivityCompat.checkSelfPermission(this, permissionsArray[i]) == PackageManager.PERMISSION_GRANTED)
                permissionsArray.removeAt(i)

            i++
        }

        if(permissionsArray.size > 0)
            ActivityCompat.requestPermissions(this, permissionsArray.toTypedArray(), 36)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //If the user didn't gave all permissions it asks for them again
        managePermissions()

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        //create a new SectionPageAdapter for using it with viewPager
        //SectionPageAdapter is a method of mine that instantiates fragments
        val sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager, FragmentActivity().lifecycle)
        //set the adapter to the viewPage
        viewPager.adapter = sectionsPagerAdapter

        //Array of tab titles
        val titles = ArrayList<String>()
        titles.add(getString(R.string.new_course)) //Tab 0
        titles.add(getString(R.string.my_courses)) //Tab 1
        //Match each tab with a fragment
        val tabs: TabLayout = findViewById(R.id.tabs)
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = titles[position]
            viewPager.setCurrentItem(tab.position, true)
        }.attach()
        //Used to prevent blank map when go back from other fragment.
        viewPager.offscreenPageLimit = 1
    }
}