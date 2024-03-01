package dev.imprex.testsuite.bungeecord;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;

import dev.imprex.testsuite.TestsuiteLogger;
import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.api.TestsuiteSender;
import dev.imprex.testsuite.server.ServerInstance;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventHandler;

public class BungeecordPlugin extends Plugin implements Listener {

	static BungeecordPlugin plugin;
	static BungeeAudiences audiences;

	private final TestsuitePlugin testsuite = new TestsuitePlugin();

	private final List<String> commandPrefixList = new ArrayList<String>();

	public BungeecordPlugin() {
		BungeecordPlugin.plugin = this;
	}

	@Override
	public void onLoad() {
		BungeecordPlugin.audiences = BungeeAudiences.create(this);
		this.testsuite.load(this.getLogger(), this.getDataFolder().toPath());
	}

	@Override
	public void onEnable() {
		this.testsuite.enable(new BungeecordApi(this));

		// Register commands
		PluginManager pluginManager = this.getProxy().getPluginManager();
		CommandDispatcher<TestsuiteSender> parentDispatcher = this.testsuite.getCommandRegistry().getDispatcher();
		for (CommandNode<TestsuiteSender> node : parentDispatcher.getRoot().getChildren()) {
			String literal = node.getName();
			this.commandPrefixList.add(literal);

			pluginManager.registerCommand(this, new BungeecordCommand(
					this.testsuite,
					(args) -> literal + " " + args,
					literal));
		}
		
//		this.testsuite.getCommandRegistry().getCommands().values().stream()
//				.filter(command -> command.isRoot())
//				.forEach(command -> {
//					String literal = command.literal().getLiteral();
//					this.commandPrefixList.add(literal);
//
//					pluginManager.registerCommand(this, new BungeecordCommand(
//							this.testsuite,
//							(args) -> literal + " " + args,
//							literal));
//				});

		pluginManager.registerListener(this, this);
	}

	@Override
	public void onDisable() {
		this.testsuite.disable();
		BungeecordPlugin.audiences.close();
	}

	@EventHandler
	public void onServerConnected(ServerConnectedEvent event) {
		Server server = event.getPlayer().getServer();
		if (server != null) {
			ServerInstance pteroServer = this.testsuite.getServerManager().getServer(server.getInfo().getName());
			if (pteroServer == null) {
				return;
			}

			TestsuiteLogger.debug("Reset inactive time on server {1} because {0} disconnected.", event.getPlayer().getName(), pteroServer.getName());
			pteroServer.resetInactiveTime();
		}
	}

	@EventHandler
	public void onPostLogin(PostLoginEvent event) {
		ProxiedPlayer player = event.getPlayer();
		BungeecordPlayer bungeePlayer = BungeecordPlayer.add(player);
		
		// packet injection to use brigadier async and greedy string only reading last argument on tab complete
		new BungeecordPacketInjector(plugin, bungeePlayer).inject();
	}

	@EventHandler
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		BungeecordPlayer.remove(event.getPlayer());
	}

	public List<String> getCommandPrefixList() {
		return this.commandPrefixList;
	}

	public TestsuitePlugin getTestsuite() {
		return this.testsuite;
	}
}
