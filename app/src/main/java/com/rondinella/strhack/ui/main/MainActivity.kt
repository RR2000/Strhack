package com.rondinella.strhack.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.rondinella.strhack.R
import com.rondinella.strhack.utils.askPermissions
import com.rondinella.strhack.utils.disableSSLCertificateChecking
import com.rondinella.strhack.utils.hasPermissions
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        disableSSLCertificateChecking()
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasPermissions(this)) {
            askPermissions(this)
        }

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager, this.lifecycle)
        viewPager.adapter = sectionsPagerAdapter

        val titles = arrayListOf(
            getString(R.string.new_course),  //Tab 0
            getString(R.string.my_courses)   //Tab 1
        )

        val tabs: TabLayout = findViewById(R.id.tabs)
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = titles[position]
            viewPager.setCurrentItem(tab.position, true)
        }.attach()

        viewPager.offscreenPageLimit = 1

        val configuration = Configuration.getInstance()
        val path: File = applicationContext.filesDir
        val osmdroidBasePath = File(path, "osmdroid").apply { mkdirs() }
        val osmdroidTilePath = File(osmdroidBasePath, "tiles").apply { mkdirs() }

        configuration.apply {
            osmdroidTileCache = osmdroidTilePath
            load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
            userAgentValue = packageName
        }
    }
}
