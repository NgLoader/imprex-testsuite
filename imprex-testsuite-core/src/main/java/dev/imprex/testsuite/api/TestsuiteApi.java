package dev.imprex.testsuite.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface TestsuiteApi {

	void scheduleTask(Runnable runnable, int delay, int repeat, TimeUnit unit);

	TestsuitePlayer getPlayer(String name);

	List<TestsuitePlayer> getPlayers();

	TestsuiteServer getServer(String name);

	TestsuiteServer createServer(String name, String ip, int port);

	boolean deleteServer(String name);

	List<TestsuiteServer> getServers();
}
