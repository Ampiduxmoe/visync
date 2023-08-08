package com.example.visync.messaging

import com.example.visync.ui.screens.main.playback_setup.FinalDevicePositionConfiguration
import com.example.visync.ui.screens.main.playback_setup.PlaybackSetupOptions
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
            TextMessage::class.simpleName                       -> decodeAs<TextMessage>(msg)
            RequestSelfInfoMessage::class.simpleName            -> decodeAs<RequestSelfInfoMessage>(msg)
            SelfInfoMessage::class.simpleName                   -> decodeAs<SelfInfoMessage>(msg)
            AllWatchersUpdateMessage::class.simpleName          -> decodeAs<AllWatchersUpdateMessage>(msg)
            GuestWatcherInfoMessage::class.simpleName           -> decodeAs<GuestWatcherInfoMessage>(msg)
            RequestGuestWatcherInfoMessage::class.simpleName             -> decodeAs<RequestGuestWatcherInfoMessage>(msg)
            PlaybackSetupOptionsUpdateMessage::class.simpleName -> decodeAs<PlaybackSetupOptionsUpdateMessage>(msg)
            DoNotHaveVideofilesMessage::class.simpleName        -> decodeAs<DoNotHaveVideofilesMessage>(msg)
            OpenPlayerMessage::class.simpleName                 -> decodeAs<OpenPlayerMessage>(msg)
            PlaybackPauseUnpauseMessage::class.simpleName       -> decodeAs<PlaybackPauseUnpauseMessage>(msg)
            PlaybackSeekToMessage::class.simpleName             -> decodeAs<PlaybackSeekToMessage>(msg)
            PlaybackSeekToPrevNextMessage::class.simpleName     -> decodeAs<PlaybackSeekToPrevNextMessage>(msg)
            VisyncPhysicalDeviceMessage::class.simpleName       -> decodeAs<VisyncPhysicalDeviceMessage>(msg)
            DevicePositionConfigMessage::class.simpleName       -> decodeAs<DevicePositionConfigMessage>(msg)
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
class RequestSelfInfoMessage(

) : VisyncMessage(
    type = RequestSelfInfoMessage::class.simpleName!!
)

@Serializable
class SelfInfoMessage(
    val endpointId: String,
    val username: String,
) : VisyncMessage(
    type = SelfInfoMessage::class.simpleName!!
)

@Serializable
class AllWatchersUpdateMessage(
    val allWatchers: List<Watcher>
) : VisyncMessage(
    type = AllWatchersUpdateMessage::class.simpleName!!
)

@Serializable
class GuestWatcherInfoMessage(
    val physicalDevice: VisyncPhysicalDevice,
    val messagingVersion: Int
) : VisyncMessage(
    type = GuestWatcherInfoMessage::class.simpleName!!
)

@Serializable
class RequestGuestWatcherInfoMessage(

) : VisyncMessage(
    type = RequestGuestWatcherInfoMessage::class.simpleName!!
)

@Serializable
class PlaybackSetupOptionsUpdateMessage(
    val playbackSetupOptions: PlaybackSetupOptions
) : VisyncMessage(
    type = PlaybackSetupOptionsUpdateMessage::class.simpleName!!
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
    val doPause: Boolean
) : VisyncMessage(
    type = PlaybackPauseUnpauseMessage::class.simpleName!!
)

@Serializable
class PlaybackSeekToMessage(
    val seekToLong: Long?,
    val seekToFloat: Float?,
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
class VisyncPhysicalDeviceMessage(
    val device: VisyncPhysicalDevice
) : VisyncMessage(
    type = VisyncPhysicalDeviceMessage::class.simpleName!!
)

@Serializable
class DevicePositionConfigMessage(
    val config: FinalDevicePositionConfiguration
) : VisyncMessage(
    type = DevicePositionConfigMessage::class.simpleName!!
)