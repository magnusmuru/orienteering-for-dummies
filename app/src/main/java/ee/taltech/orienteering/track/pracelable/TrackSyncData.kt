package ee.taltech.orienteering.track.pracelable

import android.os.Parcel
import android.os.Parcelable
import ee.taltech.orienteering.track.pracelable.loaction.Checkpoint
import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation
import ee.taltech.orienteering.track.pracelable.loaction.WayPoint

class TrackSyncData(val track: List<TrackLocation>, val wayPoints: List<WayPoint>, val checkpoints: List<Checkpoint>, val pauses: List<Int>): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createTypedArrayList(TrackLocation)!!,
        parcel.createTypedArrayList(WayPoint)!!,
        parcel.createTypedArrayList(Checkpoint)!!,
        mutableListOf()
    ) {
        parcel.readList(pauses, ArrayList::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(track)
        parcel.writeTypedList(wayPoints)
        parcel.writeTypedList(checkpoints)
        parcel.writeList(pauses)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TrackSyncData> {
        override fun createFromParcel(parcel: Parcel): TrackSyncData {
            return TrackSyncData(parcel)
        }

        override fun newArray(size: Int): Array<TrackSyncData?> {
            return arrayOfNulls(size)
        }
    }

}