package dev.atsushieno.ktmidi.ci

import kotlin.test.Test
import kotlin.test.assertEquals

class CIFactoryTest {

    @Test
    fun testDiscoveryMessages() {
        val all_supported = (CIFactory.PROTOCOL_NEGOTIATION_SUPPORTED + CIFactory.PROFILE_CONFIGURATION_SUPPORTED + CIFactory.PROPERTY_EXCHANGE_SUPPORTED).toByte()

        // Service Discovery (Inquiry)
        val expected1 = mutableListOf<Byte>(
            0x7E, 0x7F, 0x0D, 0x70, 1,
            0x10, 0x10, 0x10, 0x10, 0x7F, 0x7F, 0x7F, 0x7F,
            0x56, 0x34, 0x12, 0x57, 0x13, 0x68, 0x24,
            // LAMESPEC: Software Revision Level does not mention in which endianness this field is stored.
            0x7F, 0x5F, 0x3F, 0x1F,
            0b00001110,
            0x00, 0x02, 0, 0
        )
        val actual1 = MutableList<Byte>(29) { 0 }
        CIFactory.midiCIDiscovery(
            actual1, 1, 0x10101010,
            0x123456, 0x1357, 0x2468, 0x1F3F5F7F,
            all_supported,
            512
        )
        assertEquals(expected1, actual1)

        // Service Discovery Reply
        val actual2 = MutableList<Byte>(29) { 0 }
        CIFactory.midiCIDiscoveryReply(
            actual2, 1, 0x10101010, 0x20202020,
            0x123456, 0x1357, 0x2468, 0x1F3F5F7F,
            all_supported,
            512
        )
        assertEquals(0x71, actual2[3])
        for (i in 9..12)
            assertEquals(0x20, actual2[i]) // destination ID is not 7F7F7F7F.

        // Invalidate MUID
        val expected3 = mutableListOf<Byte>(
            0x7E, 0x7F, 0x0D, 0x7E, 1,
            0x10, 0x10, 0x10, 0x10, 0x7F, 0x7F, 0x7F, 0x7F, 0x20, 0x20, 0x20, 0x20
        )
        val actual3 = MutableList<Byte>(17) { 0 }
        CIFactory.midiCIDiscoveryInvalidateMuid(actual3, 1, 0x10101010, 0x20202020)
        assertEquals(expected3, actual3)

        // NAK
        val expected4 = mutableListOf<Byte>(
            0x7E, 5, 0x0D, 0x7F, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20
        )
        val actual4 = MutableList<Byte>(13) { 0 }
        CIFactory.midiCIDiscoveryNak(actual4, 5, 1, 0x10101010, 0x20202020)
        assertEquals(expected4, actual4)
    }

    @Test
    fun testProtocolNegotiationMessages() {
        val infos = mutableListOf(
            MidiCIProtocolTypeInfo(1, 0, 0x10, 0, 0),
            MidiCIProtocolTypeInfo(2, 0, 0x20, 0, 0)
        )

        // Protocol Negotiation (Inquiry)
        val expected1 = mutableListOf<Byte>(
            0x7E, 0x7F, 0x0D, 0x10, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20,
            1, 2,
            1, 0, 0x10, 0, 0,
            2, 0, 0x20, 0, 0
        )
        val actual1 = MutableList<Byte>(25) { 0 }
        CIFactory.midiCIProtocolNegotiation(
            actual1, false, 0x10101010, 0x20202020,
            1, infos
        )
        assertEquals(expected1, actual1)

        // Protocol Negotiation Reply
        val actual2 = MutableList<Byte>(25) { 0 }
        CIFactory.midiCIProtocolNegotiation(
            actual2, true, 0x10101010, 0x20202020,
            1, infos
        )
        assertEquals(0x11, actual2[3])

        // Set New Protocol
        val expected3 = mutableListOf<Byte>(
            0x7E, 0x7F, 0x0D, 0x12, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20,
            1, 2, 0, 0x20, 0, 0
        )
        val actual3 = MutableList<Byte>(19) { 0 }
        CIFactory.midiCIProtocolSet(
            actual3, 0x10101010, 0x20202020,
            1, infos[1]
        )
        assertEquals(expected3, actual3)

        // Test New Protocol - Initiator to Recipient
        val testData = mutableListOf<Byte>(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            0, 1, 2, 3, 4, 5, 6, 7
        )
        val expected4 = mutableListOf<Byte>(
            0x7E, 0x7F, 0x0D, 0x13, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20,
            1
        )
        val actual4 = MutableList<Byte>(14 + 48) { 0 }
        CIFactory.midiCIProtocolTest(
            actual4, true, 0x10101010, 0x20202020,
            1, testData
        )
        assertEquals(expected4, actual4.take(14))
        assertEquals(testData, actual4.drop(14))

        // Test New Protocol - Responder to Initiator
        val actual5 = MutableList<Byte>(14 + 48) { 0 }
        CIFactory.midiCIProtocolTest(
            actual5, false, 0x10101010, 0x20202020,
            1, testData
        )
        assertEquals(0x14, actual5[3])

        // Confirmation New Protocol Established
        val expected6 = mutableListOf<Byte>(
            0x7E, 0x7F, 0x0D, 0x15, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20,
            1
        )
        val actual6 = MutableList<Byte>(14) { 0 }
        CIFactory.midiCIProtocolConfirmEstablished(actual6, 0x10101010, 0x20202020, 1)
        assertEquals(expected6, actual6)
    }

    @Test
    fun testProfileConfigurationMessages() {
        // Profile Inquiry
        val expected1 = mutableListOf<Byte>(
            0x7E, 5, 0x0D, 0x20, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20
        )
        val actual1 = MutableList<Byte>(13) { 0 }
        CIFactory.midiCIProfileInquiry(actual1, 5, 0x10101010, 0x20202020)
        assertEquals(expected1, actual1)

        // Profile Inquiry Reply
        val b7E: Byte = 0x7E
        val profiles1 = mutableListOf<MidiCIProfileId>(MidiCIProfileId(b7E, 2, 3, 4, 5), MidiCIProfileId(b7E, 7, 8, 9, 10))
        val profiles2 = mutableListOf<MidiCIProfileId>(MidiCIProfileId(b7E, 12, 13, 14, 15), MidiCIProfileId(b7E, 17, 18, 19, 20))
        val expected2 = mutableListOf(
            0x7E, 5, 0x0D, 0x21, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20,
            2,
            0,
            b7E, 2, 3, 4, 5,
            b7E, 7, 8, 9, 10,
            2,
            0,
            b7E, 12, 13, 14, 15,
            b7E, 17, 18, 19, 20
        )
        val actual2 = MutableList<Byte>(37) { 0 }
        CIFactory.midiCIProfileInquiryReply(
            actual2, 5, 0x10101010, 0x20202020, profiles1, profiles2)
        assertEquals(expected2, actual2)

        // Set Profile On
        val expected3 = mutableListOf<Byte>(
            0x7E, 5, 0x0D, 0x22, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20,
            0x7E, 2, 3, 4, 5
        )
        val actual3 = MutableList<Byte>(18) { 0 }
        CIFactory.midiCIProfileSet(
            actual3, 5, true, 0x10101010, 0x20202020,
            profiles1[0]
        )
        assertEquals(expected3, actual3)

        // Set Profile Off
        val actual4 = MutableList<Byte>(18) { 0 }
        CIFactory.midiCIProfileSet(
            actual4, 5, false, 0x10101010, 0x20202020,
            profiles1[0]
        )
        assertEquals(0x23, actual4[3])

        // Profile Enabled Report
        val expected5 = mutableListOf<Byte>(
            0x7E, 5, 0x0D, 0x24, 1,
            0x10, 0x10, 0x10, 0x10, 0x7F, 0x7F, 0x7F, 0x7F,
            0x7E, 2, 3, 4, 5
        )
        val actual5 = MutableList<Byte>(18) { 0 }
        CIFactory.midiCIProfileReport(
            actual5, 5, true, 0x10101010,
            profiles1[0]
        )
        assertEquals(expected5, actual5)

        // Profile Disabled Report
        val actual6 = MutableList<Byte>(18) { 0 }
        CIFactory.midiCIProfileReport(
            actual6, 5, false, 0x10101010,
            profiles1[0]
        )
        assertEquals(0x25, actual6[3])

        // Profile Specific Data
        val expected7 = mutableListOf<Byte>(
            0x7E, 5, 0x0D, 0x2F, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20,
            0x7E, 2, 3, 4, 5,
            8, 0, 0, 0,
            8, 7, 6, 5, 4, 3, 2, 1
        )
        val actual7 = MutableList<Byte>(30) { 0 }
        val data = mutableListOf<Byte>(8, 7, 6, 5, 4, 3, 2, 1)
        CIFactory.midiCIProfileSpecificData(
            actual7, 5, 0x10101010, 0x20202020,
            profiles1[0], 8, data
        )
        assertEquals(expected7, actual7)
    }

    @Test
    fun testPropertyExchangeMessages() {
        val header = mutableListOf<Byte>(11, 22, 33, 44)
        val data = mutableListOf<Byte>(55, 66, 77, 88, 99)

        // Property Inquiry
        val expected1 = mutableListOf<Byte>(
            0x7E, 5, 0x0D, 0x30, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20,
            16
        )
        val actual1 = MutableList<Byte>(14) { 0 }
        CIFactory.midiCIPropertyGetCapabilities(actual1, 5, false, 0x10101010, 0x20202020, 16)
        assertEquals(expected1, actual1)

        // Property Inquiry Reply
        val actual2 = MutableList<Byte>(14) { 0 }
        CIFactory.midiCIPropertyGetCapabilities(actual2, 5, true, 0x10101010, 0x20202020, 16)
        assertEquals(0x31, actual2[3])

        // Has Property Data
        val expected3 = mutableListOf<Byte>(
            0x7E, 5, 0x0D, 0x32, 1,
            0x10, 0x10, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20,
            2,
            4, 0,
            11, 22, 33, 44,
            3, 0,
            1, 0,
            5, 0,
            55, 66, 77, 88, 99
        )
        val actual3 = MutableList<Byte>(31) { 0 }
        CIFactory.midiCIPropertyCommon(
            actual3, 5, CIFactory.SUB_ID_2_PROPERTY_HAS_DATA_INQUIRY,
            0x10101010, 0x20202020,
            2, 4, header, 3, 1, 5, data
        )
        assertEquals(expected3, actual3)

        // Reply to Has Property Data
        val actual4 = MutableList<Byte>(31) { 0 }
        CIFactory.midiCIPropertyCommon(
            actual4, 5, CIFactory.SUB_ID_2_PROPERTY_HAS_DATA_REPLY,
            0x10101010, 0x20202020,
            2, 4, header, 3, 1, 5, data
        )
        assertEquals(0x33, actual4[3])

        // Get Property Data
        val actual5 = MutableList<Byte>(31) { 0 }
        CIFactory.midiCIPropertyCommon(
            actual5, 5, CIFactory.SUB_ID_2_PROPERTY_GET_DATA_INQUIRY,
            0x10101010, 0x20202020,
            2, 4, header, 3, 1, 5, data
        )
        assertEquals(0x34, actual5[3])

        // Reply to Get Property Data
        val actual6 = MutableList<Byte>(31) { 0 }
        CIFactory.midiCIPropertyCommon(
            actual6, 5, CIFactory.SUB_ID_2_PROPERTY_GET_DATA_REPLY,
            0x10101010, 0x20202020,
            2, 4, header, 3, 1, 5, data
        )
        assertEquals(0x35, actual6[3])

        // Set Property Data
        val actual7 = MutableList<Byte>(31) { 0 }
        CIFactory.midiCIPropertyCommon(
            actual7, 5, CIFactory.SUB_ID_2_PROPERTY_SET_DATA_INQUIRY,
            0x10101010, 0x20202020,
            2, 4, header, 3, 1, 5, data
        )
        assertEquals(0x36, actual7[3])

        // Reply to Set Property Data
        val actual8 = MutableList<Byte>(31) { 0 }
        CIFactory.midiCIPropertyCommon(
            actual8, 5, CIFactory.SUB_ID_2_PROPERTY_SET_DATA_REPLY,
            0x10101010, 0x20202020,
            2, 4, header, 3, 1, 5, data
        )
        assertEquals(0x37, actual8[3])

        // Subscription
        val actual9 = MutableList<Byte>(31) { 0 }
        CIFactory.midiCIPropertyCommon(
            actual9, 5, CIFactory.SUB_ID_2_PROPERTY_SUBSCRIBE,
            0x10101010, 0x20202020,
            2, 4, header, 3, 1, 5, data
        )
        assertEquals(0x38, actual9[3])

        // Reply to Subscription
        val actual10 = MutableList<Byte>(31) { 0 }
        CIFactory.midiCIPropertyCommon(
            actual10, 5, CIFactory.SUB_ID_2_PROPERTY_SUBSCRIBE_REPLY,
            0x10101010, 0x20202020,
            2, 4, header, 3, 1, 5, data
        )
        assertEquals(0x39, actual10[3])

        // Notify
        val actual11 = MutableList<Byte>(31) { 0 }
        CIFactory.midiCIPropertyCommon(
            actual11, 5, CIFactory.SUB_ID_2_PROPERTY_NOTIFY,
            0x10101010, 0x20202020,
            2, 4, header, 3, 1, 5, data
        )
        assertEquals(0x3F, actual11[3])
    }
}
