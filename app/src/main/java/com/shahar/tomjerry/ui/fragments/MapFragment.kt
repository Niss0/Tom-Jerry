package com.shahar.tomjerry.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.shahar.tomjerry.R
import com.shahar.tomjerry.viewmodel.ScoreViewModel


class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val scoreViewModel: ScoreViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        scoreViewModel.selectedScore.observe(viewLifecycleOwner) { score ->
            // The 'it' here is the Score object. It can be null.
            // We use score?.let { ... } to safely execute code only if the score is not null.
            score?.let {
                updateMapLocation(it.latitude, it.longitude, "Score: ${it.score}")
            }
        }
    }

    /**
     * This callback is triggered when the map is ready to be used.
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // For now, let's just move the camera to a default location.
        // We'll add markers for the scores later.
        // Using an approximate LatLng for Ramat Gan based on your location.
        val defaultLocation = LatLng(32.0853, 34.7818)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
    }

    private fun updateMapLocation(latitude: Double, longitude: Double, title: String) {
        if (!::googleMap.isInitialized) return

        val location = LatLng(latitude, longitude)
        googleMap.clear() // Remove previous markers
        googleMap.addMarker(MarkerOptions().position(location).title(title))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

}
