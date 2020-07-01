package com.rondinella.strhack.activities

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.rondinella.strhack.R
import com.rondinella.strhack.ui.main.SectionsPagerAdapter
import com.rondinella.strhack.utils.askPermissions
import com.rondinella.strhack.utils.hasPermissions


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //If the user didn't gave all permissions it asks for them again
        (!hasPermissions(this))
            askPermissions(this)

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