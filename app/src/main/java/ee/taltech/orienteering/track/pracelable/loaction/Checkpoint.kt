package ee.taltech.orienteering.track.pracelable.loaction

import android.os.Parcel
import android.os.Parcelable

class Checkpoint : Parcelable {
    val latitude: Double
    val longitude: Double
    val altitude: Double
    val accuracy: Double
    val altitudeAccuracy: Double
    val timestamp: Long
    val elapsedTimestamp: Long
    val driftFromLastCP: Float
    val timeSinceLastCP: Long
    val distanceFromLastCP: Double

    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readFloat(),
        parcel.readLong(),
        parcel.readDouble()
    ) {
    }

    constructor(location: TrackLocation, distanceFromLastCP: Double, lastCP: Checkpoint?) {
        this.distanceFromLastCP = distanceFromLastCP
        this.latitude = location.latitude
        this.longitude = location.longitude
        this.altitude = location.altitude
        this.accuracy = location.accuracy.toDouble()
        this.altitudeAccuracy = location.altitudeAccuracy.toDouble()
        this.timestamp = location.timestamp
        this.elapsedTimestamp = location.elapsedTimestamp
        if (lastCP != null) {
            driftFromLastCP = TrackLocation.calcDistanceBetween(
                latitude,
                longitude,
                lastCP.latitude,
                lastCP.longitude
            )
            timeSinceLastCP = lastCP.elapsedTimestamp - location.elapsedTimestamp
        } else {
            driftFromLastCP = 0f
            timeSinceLastCP = 0
        }
    }

    constructor(
        latitude: Double,
        longitude: Double,
        altitude: Double,
        accuracy: Double,
        altitudeAccuracy: Double,
        timestamp: Long,
        elapsedTimestamp: Long,
        driftFromLastCP: Float,
        timeSinceLastCP: Long,
        distanceFromLastCP: Double
    ) {
        this.latitude = latitude
        this.longitude = longitude
        this.altitude = altitude
        this.accuracy = accuracy
        this.altitudeAccuracy = altitudeAccuracy
        this.timestamp = timestamp
        this.elapsedTimestamp = elapsedTimestamp
        this.driftFromLastCP = driftFromLastCP
        this.timeSinceLastCP = timeSinceLastCP
        this.distanceFromLastCP = distanceFromLastCP
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeDouble(altitude)
        parcel.writeDouble(accuracy)
        parcel.writeDouble(altitudeAccuracy)
        parcel.writeLong(timestamp)
        parcel.writeLong(elapsedTimestamp)
        parcel.writeFloat(driftFromLastCP)
        parcel.writeLong(timeSinceLastCP)
        parcel.writeDouble(distanceFromLastCP)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Checkpoint> {
        override fun createFromParcel(parcel: Parcel): Checkpoint {
            return Checkpoint(parcel)
        }

        override fun newArray(size: Int): Array<Checkpoint?> {
            return arrayOfNulls(size)
        }

        fun fromLocation(location: TrackLocation, distance: Double, lastCP: Checkpoint?): Checkpoint {
            return Checkpoint(location, distance, lastCP)
        }
    }

}