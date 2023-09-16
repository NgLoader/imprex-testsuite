package dev.imprex.testsuite.common.netty;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;

public class NetworkHandler {

	protected final List<NetworkManager> networkManagers = new ArrayList<>();
	private final List<ChannelFuture> channels = new ArrayList<>();

	private final Thread networkRunner;
	private AtomicBoolean running = new AtomicBoolean();

	public NetworkHandler(String threadName) {
		this.networkRunner = new Thread(this::run, threadName);
		this.networkRunner.setDaemon(true);
	}

	public void run() {
		while (this.running.get()) {
			synchronized (this.networkManagers) {
				for (Iterator<NetworkManager> iterator = this.networkManagers.iterator(); iterator.hasNext();) {
					NetworkManager networkManager = iterator.next();
					if (networkManager.hasChannel()) {
						if (networkManager.isConnected()) {
							try {
								networkManager.run();
							} catch (Exception e) {
								networkManager.close("Internal client error");
								networkManager.stopReading();
							}
						} else {
							iterator.remove();
							networkManager.handleDisconnect();
						}
					}
				}
			}

			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void checkRunning() {
		if (!this.running.get()) {
			this.running.set(true);
			this.networkRunner.start();
		}
	}

	public void startServer(InetSocketAddress address, int threads, ChannelInitializer<Channel> channelInitializer) {
		this.checkRunning();
		this.channels.add(new ServerBootstrap()
			.group(NettyUtil.newEventLoopGroup(threads), NettyUtil.newEventLoopGroup())
			.channel(NettyUtil.newServerSocketChannelClass())
			.childHandler(channelInitializer)
			.bind(address)
			.syncUninterruptibly());
	}

	public void startClient(InetSocketAddress address, ChannelInitializer<Channel> channelInitializer) {
		this.checkRunning();
		this.channels.add(new Bootstrap().group(NettyUtil.newEventLoopGroup())
			.channel(NettyUtil.newClientSocketChannelClass())
			.handler(channelInitializer)
			.connect(address)
			.syncUninterruptibly());
	}

	public void close() {
		for (ChannelFuture channel : this.channels) {
			try {
				channel.channel().close().sync();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}