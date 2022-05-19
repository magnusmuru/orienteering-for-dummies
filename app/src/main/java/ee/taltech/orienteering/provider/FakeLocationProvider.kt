package ee.taltech.orienteering.provider

import android.location.Location
import com.google.android.libraries.maps.LocationSource
import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation

class FakeLocationProvider: LocationSource {

    private lateinit var location: Location
    private var listener: LocationSource.OnLocationChangedListener? = null

    override fun deactivate() {
    }

    override fun activate(p0: LocationSource.OnLocationChangedListener?) {
        listener = p0
    }

    fun setLocation(loc: TrackLocation) {
        location = Location("")
        location.latitude = loc.latitude
        location.longitude = loc.longitude
        location.accuracy = loc.accuracy
        location.altitude = loc.altitude
        location.time = loc.timestamp
        location.elapsedRealtimeNanos = loc.elapsedTimestamp
        listener?.onLocationChanged(location)
    }
}