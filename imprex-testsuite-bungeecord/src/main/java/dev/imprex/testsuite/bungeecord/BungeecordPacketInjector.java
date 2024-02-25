package dev.imprex.testsuite.bungeecord;

import java.lang.reflect.Field;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;

import dev.imprex.testsuite.api.TestsuiteSender;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.TabCompleteRequest;
import net.md_5.bungee.protocol.packet.TabCompleteResponse;

public class BungeecordPacketInjector extends MessageToMessageDecoder<PacketWrapper> {
	
	private static Field ProxiedPlayerChannelField;
	private static Field ChannelWrapperChannelField;
	
	static {
		try {
			ProxiedPlayerChannelField = Class.forName("net.md_5.bungee.UserConnection").getDeclaredField("ch");
			ChannelWrapperChannelField = Class.forName("net.md_5.bungee.netty.ChannelWrapper").getDeclaredField("ch");
		} catch (NoSuchFieldException | SecurityException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private final CommandDispatcher<TestsuiteSender> dispatcher;
	private final List<String> commandPrefixList;

	private final BungeecordPlayer player;
	
	public BungeecordPacketInjector(BungeecordPlugin plugin, BungeecordPlayer player) {
		this.dispatcher = plugin.getTestsuite().getCommandRegistry().getDispatcher();
		this.commandPrefixList = plugin.getCommandPrefixList();
		this.player = player;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, PacketWrapper msg, List<Object> out) throws Exception {
		DefinedPacket packet = msg.packet;
		if (packet == null) {
			out.add(msg);
			return;
		}
		
		if (packet instanceof TabCompleteRequest tabCompletePacket) {
			StringReader cursor = new StringReader(tabCompletePacket.getCursor());
			if (cursor.canRead() && cursor.peek() == '/') {
				cursor.skip();
			}

			if (!this.commandPrefixList.contains(cursor.readStringUntil(' ').toLowerCase())) {
				out.add(msg);
				return;
			}

			ParseResults<TestsuiteSender> result = this.dispatcher.parse(cursor, this.player);
			this.dispatcher.getCompletionSuggestions(result).whenComplete((suggestions, error) -> {
				if (error != null) {
					error.printStackTrace();
					return;
				}

				ProxiedPlayer proxiedPlayer = this.player.getProxiedPlayer();
				if (proxiedPlayer.isConnected() && !suggestions.isEmpty()) {
					proxiedPlayer.unsafe().sendPacket(new TabCompleteResponse(tabCompletePacket.getTransactionId(), suggestions));
				}
			});
		} else {
			out.add(msg);
		}
	}
	
	public void inject() {
		try {
			Object channelWrapper = ProxiedPlayerChannelField.get(this.player);
			Channel channel = (Channel) ChannelWrapperChannelField.get(channelWrapper);
			channel.pipeline().addAfter("packet-decoder", "imprex-testsuite-decoder", this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
