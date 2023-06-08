package dev.atsushieno.ktmidi

fun interface OnMidiEventListener {
    fun onEvent(e: MidiEvent)
}

val emptyMidiAccess = EmptyMidiAccess()

abstract class MidiAccess {
    abstract val name: String

    abstract val inputs: Iterable<MidiPortDetails>
    abstract val outputs: Iterable<MidiPortDetails>

    abstract suspend fun openInput(portId: String): MidiInput
    abstract suspend fun openOutput(portId: String): MidiOutput

    open val canDetectStateChanges = false
    var stateChanged : (MidiPortDetails) -> Unit = {}

    open val canCreateVirtualPort = false

    open suspend fun createVirtualInputSender(context: PortCreatorContext): MidiOutput {
        throw UnsupportedOperationException()
    }
    open suspend fun createVirtualOutputReceiver(context: PortCreatorContext): MidiInput {
        throw UnsupportedOperationException()
    }
}

interface MidiPortDetails {
    val id: String
    val manufacturer: String?
    val name: String?
    val version: String?
}

enum class MidiPortConnectionState {
    OPEN,
    CLOSED,
}

interface MidiPort {
    val details: MidiPortDetails
    val connectionState: MidiPortConnectionState
    fun close()

    var midiProtocol: Int
}

fun interface OnMidiReceivedEventListener {
    fun onEventReceived(data: ByteArray, start: Int, length: Int, timestampInNanoseconds: Long)
}

interface MidiInput : MidiPort {
    abstract fun setMessageReceivedListener(listener: OnMidiReceivedEventListener)
}

interface MidiOutput : MidiPort {
    abstract fun send(mevent: ByteArray, offset: Int, length: Int, timestampInNanoseconds: Long)
}

// Virtual MIDI port support

abstract class SimpleVirtualMidiPort protected constructor(
    override val details: MidiPortDetails,
    private val onDispose: () -> Unit
) : MidiPort {

    private var state: MidiPortConnectionState = MidiPortConnectionState.OPEN

    override val connectionState: MidiPortConnectionState
        get() = state

    override fun close ()
    {
        onDispose ()
        state = MidiPortConnectionState.CLOSED
    }

    override var midiProtocol: Int
        get() = MidiCIProtocolValue.MIDI1
        set(v) = throw UnsupportedOperationException("This MidiPort implementation does not support promoting MIDI protocols")
}

class SimpleVirtualMidiInput(details: MidiPortDetails, onDispose: () -> Unit) : SimpleVirtualMidiPort(
    details,
    onDispose
), MidiInput
{
    private var messageReceived: OnMidiReceivedEventListener? = null

    override fun setMessageReceivedListener(listener: OnMidiReceivedEventListener) {
        messageReceived = listener
    }
}

class SimpleVirtualMidiOutput(details: MidiPortDetails, onDispose: () -> Unit) : SimpleVirtualMidiPort(
    details,
    onDispose
), MidiOutput
{
    var onSend: (ByteArray,Int,Int, Long) -> Unit = { _, _, _, _ -> }

    override fun send (mevent: ByteArray, offset: Int, length: Int, timestampInNanoseconds: Long) {
        onSend (mevent, offset, length, timestampInNanoseconds)
    }
}

data class PortCreatorContext(
    var applicationName: String,
    var portName: String,
    var manufacturer: String,
    var version: String
)

// MidiAccess implementation.

class EmptyMidiAccess : MidiAccess() {
    override val name: String
        get() = "(EMPTY)"

    override val inputs: Iterable<MidiPortDetails>
        get() = arrayListOf(EmptyMidiInput.instance.details)
    override val outputs: Iterable<MidiPortDetails>
        get() = arrayListOf(EmptyMidiOutput.instance.details)

    override suspend fun openInput(portId: String): MidiInput {
        if (portId != EmptyMidiInput.instance.details.id)
            throw IllegalArgumentException("Port ID $portId does not exist.")
        return EmptyMidiInput.instance
    }

    override suspend fun openOutput(portId: String): MidiOutput {
        if (portId != EmptyMidiOutput.instance.details.id)
            throw IllegalArgumentException("Port ID $portId does not exist.")
        return EmptyMidiOutput.instance
    }
}

internal abstract class EmptyMidiPort : MidiPort {
    override val connectionState = MidiPortConnectionState.OPEN

    override fun close() {
        // do nothing.
    }

    private var _midiProtocol: Int = MidiCIProtocolType.MIDI1
    // take whatever specified
    override var midiProtocol: Int
        get() = _midiProtocol
        set(v) { _midiProtocol = v }
}

internal class EmptyMidiPortDetails(override val id: String, name: String) : MidiPortDetails {
    override val manufacturer = "dummy project"
    override val name: String? = name
    override val version = "0.0"
}

internal class EmptyMidiInput : EmptyMidiPort(), MidiInput {
    companion object {
        val instance = EmptyMidiInput()
    }

    override val details: MidiPortDetails = EmptyMidiPortDetails("empty_in", "Empty MIDI Input")

    override fun setMessageReceivedListener(listener: OnMidiReceivedEventListener) {
        // do nothing, it will receive nothing
    }
}

internal class EmptyMidiOutput : EmptyMidiPort(), MidiOutput {
    companion object {
        val instance = EmptyMidiOutput()
    }

    override fun send(mevent: ByteArray, offset: Int, length: Int, timestampInNanoseconds: Long) {
        // do nothing.
    }

    override val details: MidiPortDetails = EmptyMidiPortDetails("empty_out", "Empty MIDI Output")
}
