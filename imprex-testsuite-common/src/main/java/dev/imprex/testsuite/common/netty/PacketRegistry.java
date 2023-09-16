package dev.imprex.testsuite.common.netty;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dev.imprex.testsuite.common.netty.packet.client.CPacketStandartDisconnect;
import dev.imprex.testsuite.common.netty.packet.client.CPacketStandartKeepAlive;
import dev.imprex.testsuite.common.netty.packet.server.SPacketStandartKeepAlive;

public class PacketRegistry {

	public static final PacketRegistry REGISTRY = new PacketRegistry();

	static {
		REGISTRY.registerPacket(PacketDirection.CLIENT, CPacketStandartKeepAlive.class);
		REGISTRY.registerPacket(PacketDirection.CLIENT, CPacketStandartDisconnect.class);

		REGISTRY.registerPacket(PacketDirection.SERVER, SPacketStandartKeepAlive.class);
	}

	private final Map<Class<? extends Packet<? extends PacketHandler>>, Integer> serverPacketByClass = new HashMap<>();
	private final Map<Integer, Class<? extends Packet<? extends PacketHandler>>> serverPacketById = new HashMap<>();

	private final Map<Class<? extends Packet<? extends PacketHandler>>, Integer> clientPacketByClass = new HashMap<>();
	private final Map<Integer, Class<? extends Packet<? extends PacketHandler>>> clientPacketById = new HashMap<>();

	private Map<Class<? extends Packet<? extends PacketHandler>>, Integer> getPacketByClass(PacketDirection packetDirection) {
		return packetDirection == PacketDirection.SERVER ? this.serverPacketByClass : this.clientPacketByClass;
	}

	private Map<Integer, Class<? extends Packet<? extends PacketHandler>>> getPacketById(PacketDirection packetDirection) {
		return packetDirection == PacketDirection.SERVER ? this.serverPacketById : this.clientPacketById;
	}

	private void registerPacket(PacketDirection packetDirection, Class<? extends Packet<? extends PacketHandler>> packet) {
		Map<Class<? extends Packet<? extends PacketHandler>>, Integer> byClass = this.getPacketByClass(packetDirection);
		Map<Integer, Class<? extends Packet<? extends PacketHandler>>> byId = this.getPacketById(packetDirection);

		int id = byClass.size();
		while (byClass.containsValue(id) || byId.containsKey(id)) {
			id++;
		}

		byClass.put(packet, id);
		byId.put(id, packet);
	}

	public int getId(PacketDirection packetDirection, Packet<?> packet) {
		return this.getPacketByClass(packetDirection).getOrDefault(packet.getClass(), -1);
	}

	public Packet<?> getPacket(PacketDirection packetDirection, int id) throws IOException, ReflectiveOperationException {
		Class<? extends Packet<? extends PacketHandler>> clazz = this.getPacketById(packetDirection).get(id);
		if (clazz == null) {
			throw new IOException("Packet id not found!");
		}
		return clazz.getConstructor().newInstance();
	}
}