package dev.imprex.testsuite.common.netty;

public interface PacketHandler {

	public void setNetworkManager(NetworkManager manager);

	public default void onConnected() { };
	public default void onDisconnect(String reason) { };
}