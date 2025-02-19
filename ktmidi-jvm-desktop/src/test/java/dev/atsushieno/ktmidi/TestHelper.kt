package dev.atsushieno.ktmidi

class TestHelper {
    companion object {
        @OptIn(ExperimentalUnsignedTypes::class)
        fun getMidiMusic(): MidiMusic {
            var music = MidiMusic()
            music.deltaTimeSpec = 192
            var track = MidiTrack()
            var ch = 1
            track.messages.add(MidiMessage(188, MidiEvent(MidiChannelStatus.PROGRAM + ch, 1, 0, null, 0, 0)))
            for (i in 0 until 100) {
                track.messages.add(
                    MidiMessage(4, MidiEvent(MidiChannelStatus.NOTE_ON + ch, 60, 120, null, 0, 0))
                )
                track.messages.add(
                    MidiMessage(44, MidiEvent(MidiChannelStatus.NOTE_OFF + ch, 60, 0, null, 0, 0))
                )
            }

            music.tracks.add(track)
            return music
        }

        fun getMidiMusic(resourceId: String): MidiMusic {
            TODO()
            //using (var stream = typeof (TestHelper).Assembly.GetManifestResourceStream (resourceId))
            //return MidiMusic.Read (stream)
        }

        suspend fun getMidiPlayer(
            timeManager: MidiPlayerTimer?,
            midiMusic: MidiMusic?,
            midiAccess: MidiAccess? = null
        ): Midi1Player {
            val access = midiAccess ?: emptyMidiAccess
            val music = midiMusic ?: getMidiMusic()
            val tm = timeManager ?: VirtualMidiPlayerTimer()
            return Midi1Player.create(music, access, tm)
        }

        suspend fun getMidiPlayer(
            timeManager: MidiPlayerTimer? = null,
            midiAccess: MidiAccess? = null,
            resourceId: String? = null
        ): Midi1Player {
            val access = midiAccess ?: emptyMidiAccess
            val music = if (resourceId != null) getMidiMusic(resourceId) else getMidiMusic()
            val tm = timeManager ?: VirtualMidiPlayerTimer()
            return Midi1Player.create(music, access, tm)
        }
    }
}
