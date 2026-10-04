package io.github.nexalloy.morphe.youtube.video.audio

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string


/**
 * Media3 DefaultAudioSink, where the audio session id of a newly created AudioTrack is read.
 */
internal object AudioTrackSessionIdFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            string("ExoPlayer:AudioTrackReleaseThread")
        )
    ),
    filters = listOf(
        methodCall(smali = "Landroid/media/AudioTrack;->getAudioSessionId()I"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately())
    )
)
