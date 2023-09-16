package dev.imprex.testsuite.common.netty.packet;

import dev.imprex.testsuite.common.netty.PacketHandler;
import dev.imprex.testsuite.common.netty.packet.client.CPacketStandartDisconnect;
import dev.imprex.testsuite.common.netty.packet.client.CPacketStandartKeepAlive;

public interface StandartClientPacketHandler extends PacketHandler {

	public void handleKeepAlive(CPacketStandartKeepAlive packet);

	public void handleDisconnect(CPacketStandartDisconnect packet);
}