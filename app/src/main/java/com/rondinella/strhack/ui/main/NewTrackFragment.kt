package com.rondinella.strhack.ui.main

import android.app.ActivityManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.example.strhack.AdvancedGeoPoint
import com.example.strhack.TrackerService
import com.example.strhack.readAdvencedGeoPoints
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.rondinella.strhack.R
import com.rondinella.strhack.livedata.currentTrackPositionData
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.fragment_newtrack.*
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*
import kotlin.reflect.typeOf

/**
 * A placeholder fragment containing a simple view.
 */
@Suppress("DEPRECATION")
class NewTrackFragment : Fragment() {

    lateinit var fusedLocationClient: FusedLocationProviderClient
    var courseLine = Polyline()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_newtrack, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //Set configuration for using OSMDroid
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        //Create a new location client. It needs this in order to get position
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        //Create a new overlay with my position that has to be placed on the map
        val overlayLocation = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), id_map)
        overlayLocation.enableMyLocation()
        id_map.overlays.add(overlayLocation)

        //It removes standard button in order to use touch controls
        id_map.setBuiltInZoomControls(false)
        id_map.setMultiTouchControls(true)
        val overlayRotation = RotationGestureOverlay(requireContext(), id_map).apply { isEnabled = true }
        id_map.overlays.add(overlayRotation)

        //Get controller of the map
        val mapController: IMapController = id_map.controller
        //Set zoom to 8.0
        mapController.setZoom(8.0)
        //Set max zoom out
        id_map.minZoomLevel = 8.0

        //Prevent swipe while touching the map
        id_map.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                requireActivity().view_pager.isUserInputEnabled = false
            } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                requireActivity().view_pager.isUserInputEnabled = true
            }
            false
        }

        id_start.setOnClickListener {
            activity!!.startService(Intent(context, TrackerService().javaClass))

            currentTrackPositionData.currentPosition.observe(this, androidx.lifecycle.Observer { point: GeoPoint ->
                id_map.overlayManager.remove(courseLine)
                courseLine.addPoint(point)
                id_map.overlayManager.add(courseLine)
            })

        }

        id_stop.setOnClickListener {

            id_map.overlayManager.remove(courseLine)
            courseLine = Polyline()
            activity!!.stopService(Intent(context, TrackerService().javaClass))
        }

        id_centra.setOnClickListener {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        id_map.mapOrientation = 0.0f
                        id_map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                        id_map.controller.setZoom(20.0)
                    }
                }
        }

    }

    override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        id_map.overlayManager.remove(courseLine)
        id_map.onResume()

    }

    override fun onPause() {
        super.onPause()
        Configuration.getInstance().save(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        id_map.onPause()
    }

    //I don't know what this method does... I know I shouldn't delete it
    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int): NewTrackFragment {
            return NewTrackFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}