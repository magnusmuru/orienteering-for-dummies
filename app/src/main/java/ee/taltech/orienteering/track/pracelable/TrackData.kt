package ee.taltech.orienteering.track.pracelable

import android.os.Parcel
import android.os.Parcelable

class TrackData constructor(
    val totalDistance: Double,
    val totalTime: Long,
    val distanceFromLastCP: Double,
    val timeFromLastCP: Long,
    val distanceFromLastWP: Double,
    val timeFromLastWP: Long,
    val driftLastWP: Float,
    val driftLastCP: Float
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readLong(),
        parcel.readDouble(),
        parcel.readLong(),
        parcel.readDouble(),
        parcel.readLong(),
        parcel.readFloat(),
        parcel.readFloat()
    ) {
    }

    fun getAverageSpeedFromStart(): Double {
        val speed = (totalDistance / totalTime) * 1000_000_000 * 3.6
        return if (!speed.isNaN()) speed else 0.0
    }

    fun getAverageSpeedFromLastCP(): Double {
        val speed = (distanceFromLastCP / timeFromLastCP) * 1000_000_000 * 3.6
        return if (!speed.isNaN()) speed else 0.0
    }

    fun getAverageSpeedFromLastWP(): Double {
        val speed = (distanceFromLastWP / timeFromLastWP) * 1000_000_000 * 3.6
        return if (!speed.isNaN()) speed else 0.0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(totalDistance)
        parcel.writeLong(totalTime)
        parcel.writeDouble(distanceFromLastCP)
        parcel.writeLong(timeFromLastCP)
        parcel.writeDouble(distanceFromLastWP)
        parcel.writeLong(timeFromLastWP)
        parcel.writeFloat(driftLastWP)
        parcel.writeFloat(driftLastCP)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TrackData> {
        override fun createFromParcel(parcel: Parcel): TrackData {
            return TrackData(parcel)
        }

        override fun newArray(size: Int): Array<TrackData?> {
            return arrayOfNulls(size)
        }
    }
}