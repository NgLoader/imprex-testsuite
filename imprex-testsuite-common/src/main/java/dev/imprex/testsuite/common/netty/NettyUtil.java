package dev.imprex.testsuite.common.netty;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyUtil {

	public static final boolean EPOLL = Epoll.isAvailable();
	public static final boolean KQUEUE = KQueue.isAvailable();

	private static final ThreadGroup THREAD_GROUP = new ThreadGroup("netty");
	private static final AtomicInteger THREAD_GROUP_ID = new AtomicInteger();

	public static ThreadFactory newThreadFactory(String name) {
		return task -> {
			Thread thread = new Thread(THREAD_GROUP, task, String.format(name, THREAD_GROUP_ID.getAndIncrement()));
			thread.setDaemon(true);
			return thread;
		};
	}

	public static EventLoopGroup newEventLoopGroup() {
		return NettyUtil.newEventLoopGroup(0);
	}

	public static EventLoopGroup newEventLoopGroup(int threads) {
		return EPOLL ? new EpollEventLoopGroup(threads, NettyUtil.newThreadFactory("netty-epoll-%d")) :
			KQUEUE ? new KQueueEventLoopGroup(threads, NettyUtil.newThreadFactory("netty-kqueue-%d")) :
			new NioEventLoopGroup(threads, NettyUtil.newThreadFactory("netty-nio-%d"));
	}

	public static Class<? extends SocketChannel> newClientSocketChannelClass() {
		return KQUEUE ? KQueueSocketChannel.class : EPOLL ? EpollSocketChannel.class : NioSocketChannel.class;
	}

	public static Class<? extends ServerSocketChannel> newServerSocketChannelClass() {
		return KQUEUE ? KQueueServerSocketChannel.class : EPOLL ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
	}
}
	