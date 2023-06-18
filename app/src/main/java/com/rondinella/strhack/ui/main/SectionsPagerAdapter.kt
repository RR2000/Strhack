package com.rondinella.strhack.ui.main

import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rondinella.strhack.ui.main.newTrack.NewTrackFragment
import com.rondinella.strhack.ui.main.routesList.RoutesListFragment
import java.lang.Exception


/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(fm: FragmentManager, lc: Lifecycle) : FragmentStateAdapter(fm, lc)  {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        if(position == 0)
            return NewTrackFragment.newInstance(position + 1)
        else if(position == 1)
            return RoutesListFragment.newInstance(position + 1)
        else
            throw Exception("This should not happen. Position ${position} is not valid.")
    }
}