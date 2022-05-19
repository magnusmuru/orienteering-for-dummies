package ee.taltech.orienteering.util.filter

import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation


class KalmanTrackLocationFilter(private val qMetersPerSecond: Double) : IFilter<TrackLocation> {
    companion object {
        private const val MIN_ACCURACY = 1f
    }

    private var timestamp = 0L
    private var latitude = 0.0
    private var longitude = 0.0
    private var variance = -1.0

    fun setState(lat: Double, lng: Double, accuracy: Double, timestamp: Long) {
        latitude = lat
        longitude = lng
        variance = accuracy * accuracy
        this.timestamp = timestamp
    }

    /**
     * see: https://stackoverflow.com/a/15657798
     */
    override fun process(input: TrackLocation): TrackLocation {
        val accuracy = if (input.accuracy > MIN_ACCURACY) input.accuracy else MIN_ACCURACY
        if (variance < 0) {
            // Uninitialized, initialize with current
            timestamp = input.elapsedTimestamp
            latitude = input.latitude
            longitude = input.longitude
            variance = (accuracy * accuracy).toDouble()
        } else {
            val dt = input.elapsedTimestamp - timestamp
            if (dt > 0) {
                // Variance increases over time
                variance += dt * qMetersPerSecond * qMetersPerSecond / 1_000_000_000
                timestamp = input.elapsedTimestamp
            }
            // Kalman gain matrix K = Covarariance * Inverse(Covariance + MeasurementVariance)
            // NB: because K is dimensionless, it doesn't matter that variance has different units to lat and lng
            val k = variance / (variance + accuracy * accuracy)
            latitude += k * (input.latitude - latitude)
            longitude += k * (input.longitude - longitude)

            variance *= (1 - k)
        }
        return TrackLocation(latitude, longitude, input.altitude, input.accuracy, input.altitudeAccuracy, input.timestamp, input.elapsedTimestamp)
    }
}