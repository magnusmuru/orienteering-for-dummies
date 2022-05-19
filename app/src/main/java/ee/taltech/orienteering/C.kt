package ee.taltech.orienteering

class C {
    companion object {
        const val NOTIFICATION_CHANNEL = "ee.taltech.orienteering.default_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Default channel2"
        const val NOTIFICATION_CHANNEL_DESCRIPTION = "Default channel description"
        const val NOTIFICATION_ACTION_ADD_WP = "ee.taltech.orienteering.notification.wp"
        const val NOTIFICATION_ACTION_ADD_WP_DATA = "ee.taltech.orienteering.notification.wp.lat_lng"
        const val NOTIFICATION_ACTION_ADD_CP = "ee.taltech.orienteering.notification.cp"
        const val NOTIFICATION_ACTION_ADD_CP_DATA = "ee.taltech.orienteering.notification.cp.lat_lng"

        const val TRACK_ACTION_ADD_WP = "ee.taltech.orienteering.main.wp"
        const val TRACK_ACTION_ADD_WP_DATA = "ee.taltech.orienteering.main.wp.lat_lng"
        const val TRACK_ACTION_ADD_CP = "ee.taltech.orienteering.main.cp"
        const val TRACK_ACTION_ADD_CP_DATA = "ee.taltech.orienteering.main.cp.lat_lng"

        const val LOCATION_UPDATE_ACTION = "ee.taltech.orienteering.location_update"
        const val LOCATION_UPDATE_ACTION_TRACK_LOCATION = "ee.taltech.orienteering.location_update.track_location"

        const val TRACK_STATS_UPDATE_ACTION = "ee.taltech.orienteering.track_stats_update"
        const val TRACK_STATS_UPDATE_ACTION_DATA = "ee.taltech.orienteering.track_stats_update.stats_data"

        const val TRACK_SYNC_RESPONSE = "ee.taltech.orienteering.track_sync"
        const val TRACK_SYNC_REQUEST = "ee.taltech.orienteering.track_request"
        const val TRACK_SYNC_REQUEST_TIME = "ee.taltech.orienteering.track_sync.time"
        const val TRACK_SYNC_DATA = "ee.taltech.orienteering.track_sync.data"

        const val TRACK_ACTION_REMOVE_WP = "ee.taltech.orienteering.wp.remove"
        const val TRACK_ACTION_REMOVE_WP_LOCATION = "ee.taltech.orienteering.wp.remove.location"

        const val TRACK_SET_RABBIT = "ee.taltech.orienteering.rabbit"
        const val TRACK_SET_RABBIT_NAME = "ee.taltech.orienteering.rabbit.name"
        const val TRACK_SET_RABBIT_VALUE = "ee.taltech.orienteering.rabbit.value"

        const val TRACK_SET_NAME = "ee.taltech.orienteering.track.set_name"
        const val TRACK_SET_NAME_DATA = "ee.taltech.orienteering.track.set_name.data"
        const val TRACK_SET_TYPE = "ee.taltech.orienteering.track.set_type"
        const val TRACK_SET_TYPE_DATA = "ee.taltech.orienteering.track.set_type.data"

        const val TRACK_RESET = "ee.taltech.orienteering.track.reset"
        const val TRACK_RESET_IS_TRACKING = "ee.taltech.orienteering.track.reset.is_tracking"

        const val TRACK_SAVE = "ee.taltech.orienteering.track.save"
        const val TRACK_START = "ee.taltech.orienteering.track.start"
        const val TRACK_STOP = "ee.taltech.orienteering.track.stop"

        const val TRACK_IS_RUNNING = "ee.taltech.orienteering.track.is_running"

        const val TRACK_DETAIL_REQUEST = "ee.taltech.orienteering.track.detail.request"
        const val TRACK_DETAIL_REQUEST_DATA = "ee.taltech.orienteering.track.detail.request.data"
        const val TRACK_DETAIL_RESPONSE = "ee.taltech.orienteering.track.detail.response"
        const val TRACK_DETAIL_DATA = "ee.taltech.orienteering.track.detail.data"

        const val NOTIFICATION_ID = 43210
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 324;


        const val SNAKBAR_REQUEST_FINE_LOCATION_ACCESS_TEXT = "Hey, i really need to access GPS!"
        const val SNAKBAR_REQUEST_FINE_LOCATION_CONFIRM_TEXT = "OK"
        const val SNAKBAR_REQUEST_FINE_LOCATION_DENIED_TEXT = "You denied GPS! What can I do?"
        const val SNAKBAR_OPEN_SETTINGS_TEXT = "Settings"

        const val SNAKBAR_REQUEST_EXTERNAL_STORAGE_ACCESS_TEXT = "Hey, i really need to access external storage!"
        const val SNAKBAR_REQUEST_EXTERNAL_STORAGE_CONFIRM_TEXT = "OK"
        const val SNAKBAR_REQUEST_EXTERNAL_STORAGE_DENIED_TEXT = "You denied external storage! No gpx file for you!"

        const val TOAST_USER_INTERACTION_CANCELLED_TEXT = "User interaction was cancelled."
        const val TOAST_PERMISSION_GRANTED_TEXT = "Permission was granted"
    }
}
