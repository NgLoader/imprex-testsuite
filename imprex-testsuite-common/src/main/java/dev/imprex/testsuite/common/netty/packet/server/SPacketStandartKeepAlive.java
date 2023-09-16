package dev.imprex.testsuite.common.netty.packet.server;

import dev.imprex.testsuite.common.netty.Packet;
import dev.imprex.testsuite.common.netty.packet.StandartServerPacketHandler;
import io.netty.buffer.ByteBuf;

public class SPacketStandartKeepAlive implements Packet<StandartServerPacketHandler> {

	private long time;

	public SPacketStandartKeepAlive() { }

	public SPacketStandartKeepAlive(long time) {
		this.time = time;
	}

	@Override
	public void read(ByteBuf buffer) {
		this.time = buffer.readLong();
	}

	@Override
	public void write(ByteBuf buffer) {
		buffer.writeLong(this.time);
	}

	@Override
	public void handle(StandartServerPacketHandler handler) {
		handler.handleKeepAlive(this);
	}

	public long getTime() {
		return this.time;
	}
}