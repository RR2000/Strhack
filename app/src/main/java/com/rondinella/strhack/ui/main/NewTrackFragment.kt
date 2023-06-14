package com.rondinella.strhack.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2

import com.rondinella.strhack.tracker.TrackerService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.rondinella.strhack.R
import com.rondinella.strhack.databinding.FragmentNewtrackBinding
import com.rondinella.strhack.tracker.GpxFileWriter
import com.rondinella.strhack.utils.askPermissions
import com.rondinella.strhack.utils.hasPermissions
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * A placeholder fragment containing a simple view.
 */
@Suppress("DEPRECATION")
class NewTrackFragment : Fragment() {

    private var _binding: FragmentNewtrackBinding? = null
    private val binding get() = _binding!!

    lateinit var fusedLocationClient: FusedLocationProviderClient
    var courseLine = Polyline()
    lateinit var parentActivity: Activity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewtrackBinding.inflate(inflater, container, false)
        val root: View = binding.root
        parentActivity = requireActivity()
        return root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //Set configuration for using OSMDroid
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        //Create a new location client. It needs this in order to get position
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        binding.idMap.setTileSource(TileSourceFactory.MAPNIK)

        //Create a new overlay with my position that has to be placed on the map
        val overlayLocation = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.idMap)
        overlayLocation.enableMyLocation()
        binding.idMap.overlays.add(overlayLocation)

        //It removes standard button in order to use touch controls
        binding.idMap.setBuiltInZoomControls(false)
        binding.idMap.setMultiTouchControls(true)
        val overlayRotation = RotationGestureOverlay(requireContext(), binding.idMap).apply { isEnabled = true }
        binding.idMap.overlays.add(overlayRotation)

        //Get controller of the map
        val mapController: IMapController = binding.idMap.controller
        //Set zoom to 8.0
        mapController.setZoom(8.0)
        //Set max zoom out
        binding.idMap.minZoomLevel = 8.0

        var followPosition = false


        val centerOnSuccess: OnSuccessListener<in Location>

        centerOnSuccess = OnSuccessListener { location ->
            if (location != null) {
                binding.idMap.mapOrientation = 0.0f
                binding.idMap.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                binding.idMap.controller.setZoom(20.0)

                binding.idMap.visibility = View.INVISIBLE
                binding.idMap.visibility = View.VISIBLE
            }
        }

        var isRecording = false
        binding.idMap.setOnTouchListener { _, motionEvent ->
            val viewPager = requireActivity().findViewById<ViewPager2>(R.id.view_pager)
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    viewPager.isUserInputEnabled = false
                    followPosition = false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    viewPager.isUserInputEnabled = true
                }
            }
            false
        }


        binding.startStopButton.setOnClickListener {
            if (!isRecording) {//START RECORDING
                binding.startStopButton.text = getString(R.string.stop_recording)

                if (hasPermissions(parentActivity)) {
                    requireActivity().startService(Intent(context, TrackerService().javaClass))

                    GpxFileWriter.WrittenPolylineData.getPolyline().observe(viewLifecycleOwner) { polyline ->
                        binding.idMap.overlayManager.remove(courseLine)
                        courseLine = polyline
                        binding.idMap.overlayManager.add(courseLine)

                        if (followPosition)
                            fusedLocationClient.lastLocation.addOnSuccessListener(centerOnSuccess)
                    }

                } else {
                    askPermissions(parentActivity)
                }

                isRecording = true
            } else {//STOP RECORDING
                binding.startStopButton.text = getString(R.string.start_recording)
                AlertDialog.Builder(parentActivity)
                    .setTitle(getString(R.string.stop_recording_title))
                    .setMessage(getString(R.string.stop_recording_message))
                    .setPositiveButton(getString(R.string.stop_recording)) { dialogInterface, i ->
                        binding.idMap.overlayManager.remove(courseLine)
                        courseLine = Polyline()
                        requireActivity().stopService(Intent(context, TrackerService().javaClass))
                        isRecording = false
                    }.setNegativeButton(getString(R.string.cancel), null).show()

            }
        }



        binding.idCentra.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@setOnClickListener
            }
            fusedLocationClient.lastLocation.addOnSuccessListener(centerOnSuccess)
            followPosition = true
        }

        binding.idCentra.performClick()
    }


    override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        binding.idMap.overlayManager.remove(courseLine)
        binding.idMap.onResume()

    }

    override fun onPause() {
        super.onPause()
        Configuration.getInstance().save(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
        binding.idMap.onPause()
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