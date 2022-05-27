package ee.taltech.orienteering.db.domain

import android.os.Parcel
import android.os.Parcelable

class TrackSummary(
    val trackId: Long,
    var name: String,
    var type: Int,
    val startTimestamp: Long,
    val startTimeElapsed: Long,
    val endTimestamp: Long,
    val endTimesElapsed: Long,
    val durationMoving: Long,
    val distance: Double,
    val drift: Double,
    val elevationGained: Double,
    val maxSpeed: Double,
    val minSpeed: Double
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(trackId)
        parcel.writeString(name)
        parcel.writeInt(type)
        parcel.writeLong(startTimestamp)
        parcel.writeLong(startTimeElapsed)
        parcel.writeLong(endTimestamp)
        parcel.writeLong(endTimesElapsed)
        parcel.writeLong(durationMoving)
        parcel.writeDouble(distance)
        parcel.writeDouble(drift)
        parcel.writeDouble(elevationGained)
        parcel.writeDouble(maxSpeed)
        parcel.writeDouble(minSpeed)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TrackSummary> {
        override fun createFromParcel(parcel: Parcel): TrackSummary {
            return TrackSummary(parcel)
        }

        override fun newArray(size: Int): Array<TrackSummary?> {
            return arrayOfNulls(size)
        }
    }

}