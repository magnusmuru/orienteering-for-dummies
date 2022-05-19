package ee.taltech.orienteering.track

import ee.taltech.orienteering.track.pracelable.DetailedTrackData
import ee.taltech.orienteering.track.pracelable.TrackData
import ee.taltech.orienteering.track.pracelable.TrackSyncData
import ee.taltech.orienteering.track.pracelable.loaction.Checkpoint
import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation
import ee.taltech.orienteering.track.pracelable.loaction.WayPoint
import ee.taltech.orienteering.util.TrackUtils
import ee.taltech.orienteering.util.datatype.Vector2D
import ee.taltech.orienteering.util.filter.SimpleFilter
import ee.taltech.orienteering.util.filter.SimpleFilter2D
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class Track {
    companion object {
        private const val FILTER_LENGTH = 10
    }

    private val speedFilter = SimpleFilter2D(FILTER_LENGTH)
    private val altitudeFilter = SimpleFilter(FILTER_LENGTH)

    var name: String = TrackUtils.generateNameIfNeeded("", TrackType.UNKNOWN)
    var type: TrackType = TrackType.UNKNOWN

    val track: MutableList<TrackLocation>? = Collections.synchronizedList(mutableListOf<TrackLocation>())
    val wayPoints: MutableList<WayPoint>? = Collections.synchronizedList(mutableListOf<WayPoint>())
    val checkpoints: MutableList<Checkpoint>? = Collections.synchronizedList(mutableListOf<Checkpoint>())
    val pauses = mutableListOf<Int>()

    var runningDistance = 0.0
    var runningDistanceFromLastCP = 0.0
    var runningDistanceFromLastWP = 0.0

    var elevationGained = 0.0
    var lastAltitude = 0.0

    var maxSpeed = 0.0
    var minSpeed = 0.0

    var lastLocation: TrackLocation? = null

    var startTime = 0L
    var startTimeElapsed = 0L
    var lastCPTime = 0L
    var lastWPTime = 0L
    var currentTimeElapsed = 0L
    var movingTime = 0L

    fun update(location: TrackLocation) {
        if (lastLocation == null) {
            startTime = location.timestamp
            startTimeElapsed = location.elapsedTimestamp
        } else {
            val distance = TrackLocation.calcDistanceBetween(
                lastLocation!!.latitude,
                lastLocation!!.longitude,
                location.latitude,
                location.longitude
            )
            runningDistance += distance
            runningDistanceFromLastCP += distance
            runningDistanceFromLastWP += distance

            if (location.altitude != 0.0) {
                if (lastAltitude != 0.0)
                    elevationGained += max(
                        0.0,
                        altitudeFilter.process(
                            location.altitude - lastAltitude - max(
                                location.altitudeAccuracy,
                                lastLocation?.altitudeAccuracy ?: 0f
                            ) / 2
                        )
                    )
                lastAltitude = location.altitude
            }

            // No funny stuff with pauses
            if (pauses.isEmpty() || pauses.last() != track?.size) {
                movingTime += location.elapsedTimestamp - currentTimeElapsed

                val distanceFromLast = 3.6 * 1_000_000_000 * TrackLocation.calcDistanceBetween(location, lastLocation!!)
                val bearingFromLast = TrackLocation.calcBearingBetween(lastLocation!!, location) * PI / 180.0f
                val speedVector = speedFilter.process(
                    Vector2D(
                        distanceFromLast * sin(bearingFromLast) / (location.elapsedTimestamp - currentTimeElapsed),
                        distanceFromLast * cos(bearingFromLast) / (location.elapsedTimestamp - currentTimeElapsed)
                    )
                )

                val currSpeed = speedVector.length()
                if (currSpeed > maxSpeed) {
                    maxSpeed = currSpeed
                }
                if (currSpeed < minSpeed) {
                    minSpeed = currSpeed
                }
            }
        }
        currentTimeElapsed = location.elapsedTimestamp
        lastLocation = location
        track?.add(location)
    }

    fun onPause() {
        pauses.add(track?.size ?: return)
    }

    fun addCheckpoint(trackLocation: TrackLocation) {
        checkpoints?.add(
            Checkpoint.fromLocation(
                trackLocation,
                runningDistance,
                checkpoints.lastOrNull()
            )
        )
        runningDistanceFromLastCP = 0.0
        lastCPTime = trackLocation.elapsedTimestamp
    }

    fun addWayPoint(wayPoint: WayPoint) {
        wayPoints?.add(wayPoint)
        runningDistanceFromLastWP = 0.0
        lastWPTime = wayPoint.timeAdded
    }

    fun removeWayPoint(wayPoint: WayPoint) {
        wayPoints?.find { wp -> wp == wayPoint }?.timeRemoved = wayPoint.timeRemoved
    }

    fun getTimeSinceStart(): Long {
        return currentTimeElapsed - startTimeElapsed
    }

    fun getTimeSinceLastCP(): Long {
        return currentTimeElapsed - lastCPTime
    }

    fun getTimeSinceLastWP(): Long {
        return currentTimeElapsed - lastWPTime
    }

    fun getDriftLastCP(): Float {
        if (lastLocation == null) return 0f
        val cp = checkpoints?.lastOrNull() ?: return 0f
        return TrackLocation.calcDistanceBetween(
            lastLocation!!.latitude,
            lastLocation!!.longitude,
            cp.latitude,
            cp.longitude
        )
    }

    fun getDriftToLastWP(): Float {
        if (lastLocation == null) return 0f
        val wp = wayPoints?.lastOrNull() ?: return 0f
        return TrackLocation.calcDistanceBetween(
            lastLocation!!.latitude,
            lastLocation!!.longitude,
            wp.latitude,
            wp.longitude
        )
    }

    fun getDrift(): Float {
        if (lastLocation == null) return 0f
        val start = track?.first() ?: return 0f
        return TrackLocation.calcDistanceBetween(
            lastLocation!!.latitude,
            lastLocation!!.longitude,
            start.latitude,
            start.longitude
        )
    }

    fun getTrackData(): TrackData {
        return TrackData(
            runningDistance,
            getTimeSinceStart(),
            runningDistanceFromLastCP,
            getTimeSinceLastCP(),
            runningDistanceFromLastWP,
            getTimeSinceLastWP(),
            getDriftToLastWP(),
            getDriftLastCP()
        )
    }

    fun getDetailedTrackData(): DetailedTrackData {
        val avgElevation = track?.map { p -> p.altitude }?.filter { a -> a != 0.0 }?.average() ?: 0.0
        val drift = if (track?.size ?: 0 > 1) TrackLocation.calcDistanceBetween(track!!.first(), track.last()).toDouble() else 0.0
        return DetailedTrackData(
            name,
            type.value,
            getTimeSinceStart(),
            runningDistance,
            elevationGained,
            avgElevation,
            drift,
            checkpoints?.size ?: 0
        )
    }

    fun getTrackSyncData(since: Long): TrackSyncData {
        return TrackSyncData(
            track?.filter { p -> p.elapsedTimestamp >= since } ?: listOf(),
            wayPoints?.filter { p -> p.timeAdded >= since } ?: listOf(),
            checkpoints?.filter { p -> p.elapsedTimestamp >= since } ?: listOf(),
            pauses
        )
    }
}