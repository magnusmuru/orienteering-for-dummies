package ee.taltech.orienteering.track.pracelable.loaction

import android.os.Parcel
import android.os.Parcelable

class WayPoint(val latitude: Double, val longitude: Double, val timeAdded: Long) : Parcelable {
    var timeRemoved: Long? = null

    constructor(latitude: Double, longitude: Double, timeAdded: Long, timeRemoved: Long): this(latitude, longitude, timeAdded) {
        this.timeRemoved = timeRemoved
    }

    constructor(parcel: Parcel) : this(parcel.readDouble(), parcel.readDouble(), parcel.readLong()) {
        timeRemoved = parcel.readValue(Long::class.java.classLoader) as? Long
    }

    fun getDriftToWP(location: TrackLocation): Float {
        return TrackLocation.calcDistanceBetween(latitude, longitude, location.latitude, location.longitude)
    }



    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeLong(timeAdded)
        parcel.writeValue(timeRemoved)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WayPoint

        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (timeAdded != other.timeAdded) return false
        if (timeRemoved != other.timeRemoved) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + timeAdded.hashCode()
        result = 31 * result + (timeRemoved?.hashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<WayPoint> {
        override fun createFromParcel(parcel: Parcel): WayPoint {
            return WayPoint(parcel)
        }

        override fun newArray(size: Int): Array<WayPoint?> {
            return arrayOfNulls(size)
        }
    }
}