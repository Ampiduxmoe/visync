package com.example.visync.messaging

import com.example.visync.ui.screens.player.Watcher
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
            TextMessage::class.simpleName               -> decodeAs<TextMessage>(msg)
            RequestSelfInfoMessage::class.simpleName    -> decodeAs<RequestSelfInfoMessage>(msg)
            SelfInfoMessage::class.simpleName           -> decodeAs<SelfInfoMessage>(msg)
            AllWatchersUpdateMessage::class.simpleName  -> decodeAs<AllWatchersUpdateMessage>(msg)
            VersionMessage::class.simpleName            -> decodeAs<VersionMessage>(msg)
            RequestVersionMessage::class.simpleName     -> decodeAs<RequestVersionMessage>(msg)
            OpenPlayerMessage::class.simpleName         -> decodeAs<OpenPlayerMessage>(msg)
            SetVideofilesMessage::class.simpleName      -> decodeAs<SetVideofilesMessage>(msg)
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
class VersionMessage(
    val version: Int
) : VisyncMessage(
    type = VersionMessage::class.simpleName!!
)

@Serializable
class RequestVersionMessage(

) : VisyncMessage(
    type = RequestVersionMessage::class.simpleName!!
)

@Serializable
class OpenPlayerMessage(

) : VisyncMessage(
    type = OpenPlayerMessage::class.simpleName!!
)

@Serializable
class SetVideofilesMessage(
    val videofileNames: List<String>
) : VisyncMessage(
    type = SetVideofilesMessage::class.simpleName!!
)