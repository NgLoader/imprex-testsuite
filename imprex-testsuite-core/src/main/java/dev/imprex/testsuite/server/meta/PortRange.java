package dev.imprex.testsuite.server.meta;

import java.util.Arrays;

public class PortRange {

	private int minPort;
	private int maxPort;

	private int offest;
	private boolean[] used;

	public PortRange(int minPort, int maxPort) {
		this.minPort = Math.min(minPort, maxPort);
		this.maxPort = Math.max(minPort, maxPort);

		this.offest = this.maxPort - this.minPort;
		this.used = new boolean[offest];
		Arrays.fill(this.used, false);
	}

	public void markUsedPort(int port) {
		if (this.validPort(port)) {
			this.used[port - this.minPort] = true;
		}
	}

	public int parkFreePort() {
		int valid = 0;
		while (this.used[valid]) {
			valid++;
		}

		int port = this.minPort + valid;
		if (this.validPort(port)) {
			this.used[valid] = true;
			return port;
		}

		return -1;
	}

	public int peekFreePort() {
		int valid = 0;
		while (this.used[valid]) {
			valid++;
		}

		int port = this.minPort + valid;
		if (this.validPort(port)) {
			return port;
		}

		return -1;
	}

	public boolean validPort(int port) {
		return this.minPort <= port && this.maxPort >= port;
	}
}