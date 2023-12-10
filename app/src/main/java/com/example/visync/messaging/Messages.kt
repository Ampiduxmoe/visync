package com.example.visync.messaging

import com.example.visync.ui.screens.main.playback_setup.DevicePositionsEditor
import com.example.visync.ui.screens.main.playback_setup.PlaybackOptions
import com.example.visync.ui.screens.main.playback_setup.SingleEndpointPings
import com.example.visync.ui.screens.main.playback_setup.Watcher
import com.example.visync.ui.screens.player.VisyncPhysicalDevice
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
open class VisyncMessage(
    val type: String
)

class JsonVisyncMessageConverter {
    private val jsonIgnoreUnknownKeys = Json { ignoreUnknownKeys = true }

    inline fun <reified T: VisyncMessage> encode(msg: T): String {
        return Json.encodeToString(msg)
    }

    fun decode(msg: String): VisyncMessage {
        val baseVisyncMessage = jsonIgnoreUnknownKeys.decodeFromString<VisyncMessage>(msg)
        return when (baseVisyncMessage.type) {
            TextMessage::class.simpleName                           -> decodeAs<TextMessage>(msg)
            RequestOwnEndpointIdMessage::class.simpleName           -> decodeAs<RequestOwnEndpointIdMessage>(msg)
            YourOwnEndpointIdMessage::class.simpleName              -> decodeAs<YourOwnEndpointIdMessage>(msg)
            SelfWatcherUpdateMessage::class.simpleName              -> decodeAs<SelfWatcherUpdateMessage>(msg)
            AllWatchersUpdateMessage::class.simpleName              -> decodeAs<AllWatchersUpdateMessage>(msg)
            RequestMessengerVersionMessage::class.simpleName        -> decodeAs<RequestMessengerVersionMessage>(msg)
            MessengerVersionMessage::class.simpleName               -> decodeAs<MessengerVersionMessage>(msg)
            RequestPhysicalDeviceMessage::class.simpleName          -> decodeAs<RequestPhysicalDeviceMessage>(msg)
            PhysicalDeviceMessage::class.simpleName                 -> decodeAs<PhysicalDeviceMessage>(msg)
            PlaybackOptionsUpdateMessage::class.simpleName          -> decodeAs<PlaybackOptionsUpdateMessage>(msg)
            DoNotHaveVideofilesMessage::class.simpleName            -> decodeAs<DoNotHaveVideofilesMessage>(msg)
            OpenPlayerMessage::class.simpleName                     -> decodeAs<OpenPlayerMessage>(msg)
            PlaybackPauseUnpauseMessage::class.simpleName           -> decodeAs<PlaybackPauseUnpauseMessage>(msg)
            PlaybackSeekToMessage::class.simpleName                 -> decodeAs<PlaybackSeekToMessage>(msg)
            PlaybackSeekToPrevNextMessage::class.simpleName         -> decodeAs<PlaybackSeekToPrevNextMessage>(msg)
            DevicePositionsMessage::class.simpleName                -> decodeAs<DevicePositionsMessage>(msg)
            RestoreMyConnectionMessage::class.simpleName            -> decodeAs<RestoreMyConnectionMessage>(msg)
            CanNotRestoreYourConnectionMessage::class.simpleName    -> decodeAs<CanNotRestoreYourConnectionMessage>(msg)
            ConnectionRestoredMessage::class.simpleName             -> decodeAs<ConnectionRestoredMessage>(msg)
            PingMessage::class.simpleName                           -> decodeAs<PingMessage>(msg)
            PongMessage::class.simpleName                           -> decodeAs<PongMessage>(msg)
            SyncBallMessage::class.simpleName                       -> decodeAs<SyncBallMessage>(msg)
            else -> {
                throw IllegalArgumentException("Could not match message to any message type")
            }
        }
    }

    private inline fun <reified T> decodeAs(msg: String): T {
        return Json.decodeFromString(msg)
    }
}

@Serializable
class TextMessage(
    val message: String
) : VisyncMessage(
    type = TextMessage::class.simpleName!!
)

@Serializable
class RequestOwnEndpointIdMessage(

) : VisyncMessage(
    type = RequestOwnEndpointIdMessage::class.simpleName!!
)

@Serializable
class YourOwnEndpointIdMessage(
    val endpointId: String,
) : VisyncMessage(
    type = YourOwnEndpointIdMessage::class.simpleName!!
)

@Serializable
class SelfWatcherUpdateMessage(
    val self: Watcher,
) : VisyncMessage(
    type = SelfWatcherUpdateMessage::class.simpleName!!
)


@Serializable
class AllWatchersUpdateMessage(
    val allWatchers: List<Watcher>,
    val timestamp: Long,
) : VisyncMessage(
    type = AllWatchersUpdateMessage::class.simpleName!!
)

@Serializable
class RequestMessengerVersionMessage(

) : VisyncMessage(
    type = RequestMessengerVersionMessage::class.simpleName!!
)

@Serializable
class MessengerVersionMessage(
    val messagingVersion: Int
) : VisyncMessage(
    type = MessengerVersionMessage::class.simpleName!!
)

@Serializable
class RequestPhysicalDeviceMessage(

) : VisyncMessage(
    type = RequestPhysicalDeviceMessage::class.simpleName!!
)

@Serializable
class PhysicalDeviceMessage(
    val physicalDevice: VisyncPhysicalDevice
) : VisyncMessage(
    type = PhysicalDeviceMessage::class.simpleName!!
)

@Serializable
class PlaybackOptionsUpdateMessage(
    val playbackOptions: PlaybackOptions,
    val timestamp: Long,
) : VisyncMessage(
    type = PlaybackOptionsUpdateMessage::class.simpleName!!
)

@Serializable
class DoNotHaveVideofilesMessage(
    val videofileNames: List<String>
) : VisyncMessage(
    type = DoNotHaveVideofilesMessage::class.simpleName!!
)

@Serializable
class OpenPlayerMessage(

) : VisyncMessage(
    type = OpenPlayerMessage::class.simpleName!!
)

@Serializable
class PlaybackPauseUnpauseMessage(
    val pingList: List<SingleEndpointPings>,
    val doPause: Boolean,
    val position: Long,
) : VisyncMessage(
    type = PlaybackPauseUnpauseMessage::class.simpleName!!
)

@Serializable
class PlaybackSeekToMessage(
    val pingList: List<SingleEndpointPings>,
    val seekTo: Long,
) : VisyncMessage(
    type = PlaybackSeekToMessage::class.simpleName!!
)

@Serializable
class PlaybackSeekToPrevNextMessage(
    val toPrev: Boolean
) : VisyncMessage(
    type = PlaybackSeekToPrevNextMessage::class.simpleName!!
)

@Serializable
class DevicePositionsMessage(
    val positions: DevicePositionsEditor
) : VisyncMessage(
    type = DevicePositionsMessage::class.simpleName!!
)

@Serializable
class RestoreMyConnectionMessage(
    val oldEndpointId: String,
) : VisyncMessage(
    type = RestoreMyConnectionMessage::class.simpleName!!
)

@Serializable
class CanNotRestoreYourConnectionMessage(
    val reason: String,
) : VisyncMessage(
    type = CanNotRestoreYourConnectionMessage::class.simpleName!!
)

@Serializable
class ConnectionRestoredMessage(
    val allWatchers: List<Watcher>,
    val playbackOptions: PlaybackOptions,
    val positions: DevicePositionsEditor?,
    // TODO: playback position (if disconnected during playback)
) : VisyncMessage(
    type = ConnectionRestoredMessage::class.simpleName!!
)

@Serializable
class PingMessage(
    val pingTimestamp: Long
) : VisyncMessage(
    type = PingMessage::class.simpleName!!
)

@Serializable
class PongMessage(
    val pingTimestamp: Long
) : VisyncMessage(
    type = PongMessage::class.simpleName!!
)

@Serializable
class SyncBallMessage(
    val posX: Float,
    val posY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val pingData: List<SingleEndpointPings>
) : VisyncMessage(
    type = SyncBallMessage::class.simpleName!!
)