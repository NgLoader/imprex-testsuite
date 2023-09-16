package dev.imprex.testsuite.common.netty;

import io.netty.buffer.ByteBuf;

public interface Packet<T extends PacketHandler> {

	public void read(ByteBuf buffer);
	public void write(ByteBuf buffer);

	public void handle(T handler);
}