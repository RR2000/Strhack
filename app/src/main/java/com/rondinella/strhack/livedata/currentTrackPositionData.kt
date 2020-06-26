package com.rondinella.strhack.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.osmdroid.util.GeoPoint

object currentTrackPositionData: ViewModel() {

    private val _currentPosition = MutableLiveData<GeoPoint>()

    val currentPosition: LiveData<GeoPoint>
        get() = _currentPosition

    fun changeCurrentPosition(position: GeoPoint){
        _currentPosition.value = position
    }
}