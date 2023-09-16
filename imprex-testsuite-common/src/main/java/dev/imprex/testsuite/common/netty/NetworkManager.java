package dev.imprex.testsuite.common.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class NetworkManager extends SimpleChannelInboundHandler<Packet<PacketHandler>> implements Runnable {

	private Channel channel;
	private PacketHandler packetHandler;

	private boolean disconnected;
	private String disconnectedReasion;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		this.channel = ctx.channel();

		this.packetHandler.onConnected();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		this.close(String.format("%s lost connection", this.channel.toString()));
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet<PacketHandler> packet) throws Exception {
		packet.handle(this.packetHandler);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
		cause.printStackTrace();
	}

	@Override
	public void run() {
		if (this.packetHandler instanceof Runnable) {
			((Runnable) this.packetHandler).run();
		}
	}

	public ChannelFuture sendPacket(Packet<?> packet) {
		return this.sendPacket(packet, null);
	}

	public ChannelFuture sendPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> listener) {
		ChannelPromise channelPromise = listener != null ? this.channel.newPromise() : this.channel.voidPromise();
		ChannelFuture future = this.channel.writeAndFlush(packet, channelPromise);
		if (listener != null) {
			future.addListener(listener);
		}
		return future;
	}

	public void stopReading() {
		this.channel.config().setAutoRead(false);
	}

	public void handleDisconnect() {
		if (this.hasChannel() && !this.channel.isOpen()) {
			if (this.disconnected) {
				//TODO logger warn disconnect was called twice
			} else {
				this.disconnected = true;
				if (this.packetHandler != null) {
					this.packetHandler.onDisconnect(this.disconnectedReasion != null ? this.disconnectedReasion : "Internal server error");
				}
			}
		}
	}

	public void close(String reason) {
		if (this.channel.isOpen()) {
			this.channel.close().awaitUninterruptibly();
		}
		if (this.disconnectedReasion == null) {
			this.disconnectedReasion = reason;
		}
	}

	public void setPacketHandler(PacketHandler packetHandler) {
		packetHandler.setNetworkManager(this);
		this.packetHandler = packetHandler;
	}

	public boolean hasChannel() {
		return this.channel != null;
	}

	public boolean isConnected() {
		return this.channel != null && this.channel.isOpen();
	}

	public PacketHandler getPacketHandler() {
		return this.packetHandler;
	}
}