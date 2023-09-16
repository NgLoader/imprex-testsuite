package dev.imprex.testsuite.common.netty.packet;

import dev.imprex.testsuite.common.netty.PacketHandler;
import dev.imprex.testsuite.common.netty.packet.server.SPacketStandartKeepAlive;

public interface StandartServerPacketHandler extends PacketHandler {

	public void handleKeepAlive(SPacketStandartKeepAlive packet);
}