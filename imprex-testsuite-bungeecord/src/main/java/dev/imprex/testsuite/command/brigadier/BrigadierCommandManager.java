package dev.imprex.testsuite.command.brigadier;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import dev.imprex.testsuite.TestsuitePlugin;
import dev.imprex.testsuite.common.ReflectionUtil;
import dev.imprex.testsuite.util.ArgumentBuilder;
import io.netty.channel.Channel;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BrigadierCommandManager implements Listener {

	private final TestsuitePlugin plugin;

	private final BrigadierCommandDispatcher dispatcher = new BrigadierCommandDispatcher();

	public BrigadierCommandManager(TestsuitePlugin plugin) {
		this.plugin = plugin;

		ProxyServer.getInstance().getPluginManager().registerListener(this.plugin, this);
	}

	@EventHandler
	public void onPostLogin(PostLoginEvent event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ProxiedPlayer player = event.getPlayer();
		Object playerChannel = ReflectionUtil.getField(player, "ch");
		Method getHandle = ReflectionUtil.getMethod(playerChannel.getClass(), "getHandle");
		Channel channel = (Channel) getHandle.invoke(playerChannel);
		channel.pipeline().addAfter("packet-decoder", "testsuite-decoder", new BrigadierPacketDecoder(this.dispatcher, player));
	}

	public void register(BrigadierCommand command) {
		CommandNode<CommandSender> commandNode = this.dispatcher.register(command.command());
		for (String alias : command.aliases()) {
			// redirect sub commands
			this.dispatcher.register(ArgumentBuilder.literal(alias).redirect(commandNode));
		}

		BrigadierCommandWrapper commandWrapper = new BrigadierCommandWrapper(this.dispatcher, commandNode, command.aliases());
		ProxyServer.getInstance().getPluginManager().registerCommand(this.plugin, commandWrapper);
	}

	public void unregister(String command) {
		RootCommandNode<?> node = this.dispatcher.getRoot();
		((Map<?, ?>) ReflectionUtil.getField(CommandNode.class, node, "children")).remove(command);
		((Map<?, ?>) ReflectionUtil.getField(CommandNode.class, node, "literals")).remove(command);
		((Map<?, ?>) ReflectionUtil.getField(CommandNode.class, node, "arguments")).remove(command);
	}

	public void unregisterNode(CommandNode<?> node) {
		this.unregister(node.getName());
	}

	public CommandDispatcher<CommandSender> getDispatcher() {
		return this.dispatcher;
	}
}