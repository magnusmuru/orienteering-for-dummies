package ee.taltech.orienteering.util

import android.content.Context
import android.widget.Toast
import ee.taltech.orienteering.api.controller.TrackSyncController
import ee.taltech.orienteering.api.dto.GpsLocationDto
import ee.taltech.orienteering.api.dto.GpsSessionDto
import ee.taltech.orienteering.component.imageview.TrackTypeIcons
import ee.taltech.orienteering.db.repository.*
import ee.taltech.orienteering.track.Track
import ee.taltech.orienteering.track.TrackType
import ee.taltech.orienteering.track.pracelable.loaction.Checkpoint
import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation
import ee.taltech.orienteering.track.pracelable.loaction.WayPoint
import io.jenetics.jpx.GPX

import java.text.SimpleDateFormat
import java.util.*

class TrackUtils {
    companion object {
        private val trackNameRegex = "\\w+\\s\\d{1,2}-\\d{1,2}+-\\d{4}".toRegex()
        private val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)

        fun generateNameIfNeeded(name: String, type: TrackType): String {
            if (name == "" || TrackTypeIcons.OPTIONS.any { t -> name.startsWith(t) } && name.matches(trackNameRegex)) {
                val date = Date()
                return "${TrackTypeIcons.getString(type)} ${dateFormatter.format(date)}"
            }
            return name
        }

        fun syncTracks(
            offlineSessionsRepository: OfflineSessionsRepository,
            trackSummaryRepository: TrackSummaryRepository,
            trackLocationsRepository: TrackLocationsRepository,
            checkpointsRepository: CheckpointsRepository,
            wayPointsRepository: WayPointsRepository,
            trackSyncController: TrackSyncController,
            context: Context
        ) {
            val sessionsToSync = offlineSessionsRepository.readOfflineSessions()

            sessionsToSync.forEach { toDeleteSession ->
                val session = trackSummaryRepository.readTrackSummary(toDeleteSession.trackId)
                if (session == null) {
                    offlineSessionsRepository.deleteOfflineSession(toDeleteSession.id)
                } else {
                    val locations = trackLocationsRepository.readTrackLocations(toDeleteSession.trackId, 0L, Long.MAX_VALUE)
                    val checkpoints = checkpointsRepository.readTrackCheckpoints(toDeleteSession.trackId)
                    val wayPoints = wayPointsRepository.readTrackWayPoints(toDeleteSession.trackId)

                    val sessionDto = GpsSessionDto(
                        name = session.name,
                        description = session.name,
                        recordedAt = Date(session.startTimestamp)
                    )
                    trackSyncController.createNewSession(sessionDto, { resp ->
                        val locationsToUpload = mutableListOf<GpsLocationDto>()
                        locations.forEach { location ->
                            locationsToUpload.add(GpsLocationDto.fromTrackLocation(location, resp.id!!))
                        }

                        checkpoints.forEach { cp ->
                            locationsToUpload.add(GpsLocationDto.fromCheckpoint(cp, resp.id!!))
                        }

                        wayPoints.forEach { wp ->
                            locationsToUpload.add(GpsLocationDto.fromWayPoint(wp, resp.id!!))
                        }

                        trackSyncController.addMultipleLocationsToSession(locationsToUpload, resp.id!!, {
                            offlineSessionsRepository.deleteOfflineSession(toDeleteSession.id)
                            Toast.makeText(context, "Uploaded session: ${session.name}", Toast.LENGTH_SHORT).show()
                        }, {
                            Toast.makeText(context, "Error uploading session: ${session.name}", Toast.LENGTH_SHORT).show()
                        })
                    }, { }
                    )
                }
            }
            if (sessionsToSync.isEmpty()) {
                Toast.makeText(context, "Everything is up to date!", Toast.LENGTH_SHORT).show()
            }
        }

        fun serializeToGpx(trackLocations: List<TrackLocation>, checkpoints: List<Checkpoint>, wayPoints: List<WayPoint>): GPX {
            val gpx = GPX.builder()
                .addTrack { gpxTrack ->
                    gpxTrack.addSegment { gpxSegment ->
                        trackLocations.forEach { trackLocation ->
                            gpxSegment.addPoint { p ->
                                p.lat(trackLocation.latitude)
                                    .lon(trackLocation.longitude)
                                    .ele(trackLocation.altitude)
                                    .hdop(trackLocation.accuracy.toDouble())
                                    .vdop(trackLocation.altitudeAccuracy.toDouble())
                                    .time(trackLocation.timestamp)
                                    .desc("LOC")
                                    .type("LOC")
                            }
                        }
                        /*wayPoints.forEach { wp ->
                            gpxSegment.addPoint { p ->
                                p.lat(wp.latitude)
                                    .lon(wp.longitude)
                                    .time(wp.timeAdded)
                                    .desc("WP")
                                    .sym("WP")
                                    .type("WP")

                            }
                        }*/
                    }
                }
            checkpoints.forEach { cp ->
                gpx.addWayPoint { p ->
                    p.lat(cp.latitude)
                        .lon(cp.longitude)
                        .ele(cp.altitude)
                        .hdop(cp.accuracy)
                        .vdop(cp.altitudeAccuracy)
                        .time(cp.timestamp)
                        .desc("CP")
                        .sym("CP")
                        .type("CP")

                }
            }

            wayPoints.forEach { wp ->
                gpx.addWayPoint { p ->
                    p.lat(wp.latitude)
                        .lon(wp.longitude)
                        .time(wp.timeAdded)
                        .desc("WP")
                        .sym("WP")
                        .type("WP")

                }
            }

            return gpx.build()
        }

        fun serializeToGpx(track: Track): GPX {
            var lastPause = 0
            val pauses = track.pauses
            pauses.add(track.track!!.size )

            val gpx = GPX.builder()
                .addTrack { gpxTrack ->
                    pauses.forEach { pause ->
                        gpxTrack.addSegment { gpxSegment ->
                            track.track.subList(lastPause, pause).forEach { trackLocation ->
                                gpxSegment.addPoint { p ->
                                    p.lat(trackLocation.latitude)
                                        .lon(trackLocation.longitude)
                                        .ele(trackLocation.altitude)
                                        .hdop(trackLocation.accuracy.toDouble())
                                        .vdop(trackLocation.altitudeAccuracy.toDouble())
                                        .time(trackLocation.timestamp)
                                }
                            }
                        }
                        lastPause = pause
                    }
                }.build()
            return gpx
        }
    }
}