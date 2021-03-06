/*******************************************************************************
 * Copyright (c) 2015
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.network.client.receiver;

import static org.junit.Assert.assertEquals;

import java.util.List;

import jsettlers.network.client.receiver.BufferingPacketReceiver;
import jsettlers.network.infrastructure.channel.TestPacket;

import org.junit.Test;

/**
 * Test for the class {@link BufferingPacketReceiver}.
 * 
 * @author Andreas Eberle
 * 
 */
public class BufferingPacketReceiverTest {

	private BufferingPacketReceiver<TestPacket> receiver = new BufferingPacketReceiver<TestPacket>();

	@Test
	public void testEmptyAtStart() {
		assertEquals(0, receiver.popBufferedPackets().size());
		assertEquals(0, receiver.popBufferedPackets().size());
	}

	@Test
	public void testAddMultipleAndPop() {
		TestPacket[] packets = new TestPacket[] {
				new TestPacket("sgpks", -23424),
				new TestPacket("dsdssdfdsfsfsfgpdsf", 8767624),
				new TestPacket("&/)(&(/\"QZ)U", 234)
		};

		for (TestPacket curr : packets) {
			receiver.receivePacket(curr);
		}

		List<TestPacket> receivedPackets = receiver.popBufferedPackets();
		assertEquals(packets.length, receivedPackets.size());
		assertEquals(0, receiver.popBufferedPackets().size());

		for (int i = 0; i < packets.length; i++) {
			assertEquals(packets[i], receivedPackets.get(i));
		}
	}
}
