package com.rondinella.strhack.ui.main

import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.rondinella.strhack.tracker.TrackerService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.rondinella.strhack.R
import com.rondinella.strhack.tracker.GpxFileWriter
import com.rondinella.strhack.utils.askPermissions
import com.rondinella.strhack.utils.hasPermissions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_newtrack.*
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
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

    lateinit var fusedLocationClient: FusedLocationProviderClient
    var courseLine = Polyline()
    lateinit var parentActivity: Activity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_newtrack, container, false)
        parentActivity = activity!!
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

        var followPosition = false

        //Prevent swipe while touching the map
        id_map.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                requireActivity().view_pager.isUserInputEnabled = false
                followPosition = false
            } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                requireActivity().view_pager.isUserInputEnabled = true
            }
            false
        }

        val centerOnSuccess: OnSuccessListener<in Location>

        centerOnSuccess = OnSuccessListener { location ->
            if (location != null) {
                id_map.mapOrientation = 0.0f
                id_map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                id_map.controller.setZoom(20.0)

                id_map.visibility = View.INVISIBLE
                id_map.visibility = View.VISIBLE
            }
        }

        var isRecording = false


        start_stop_button.setOnClickListener {
            if (!isRecording) {//START RECORDING
                start_stop_button.text = getString(R.string.stop_recording)

                if (hasPermissions(parentActivity)) {
                    activity!!.startService(Intent(context, TrackerService().javaClass))

                    GpxFileWriter.WrittenPolylineData.getPolyline().observe(this, androidx.lifecycle.Observer { polyline ->
                        id_map.overlayManager.remove(courseLine)
                        courseLine = polyline
                        id_map.overlayManager.add(courseLine)

                        if(followPosition)
                            fusedLocationClient.lastLocation.addOnSuccessListener(centerOnSuccess)
                    })

                } else {
                    askPermissions(parentActivity)
                }

                isRecording = true
            } else {//STOP RECORDING
                start_stop_button.text = getString(R.string.start_recording)
                //@TODO LOCALIZE DIALOG
                AlertDialog.Builder(parentActivity)
                    .setTitle("Che fai?")
                    .setMessage("Sei sicuro di voler terminare il giro?")
                    .setPositiveButton("Termina") { dialogInterface, i ->
                        id_map.overlayManager.remove(courseLine)
                        courseLine = Polyline()
                        activity!!.stopService(Intent(context, TrackerService().javaClass))
                        isRecording = false
                    }.setNegativeButton("Continua a registrare", null).show()

            }
        }



        id_centra.setOnClickListener {
            fusedLocationClient.lastLocation.addOnSuccessListener(centerOnSuccess)
            followPosition = true
        }

        id_centra.performClick()
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