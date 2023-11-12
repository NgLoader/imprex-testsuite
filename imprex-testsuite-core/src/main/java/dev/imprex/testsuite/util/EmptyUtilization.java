package dev.imprex.testsuite.util;

import java.time.Duration;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mattmalec.pterodactyl4j.client.entities.Utilization;

public class EmptyUtilization implements Utilization {

	@Override
	public UtilizationState getState() {
		return UtilizationState.OFFLINE;
	}

	@Override
	public Duration getUptime() {
		return Duration.ZERO;
	}

	@Override
	public long getMemory() {
		return 0;
	}

	@Override
	public long getDisk() {
		return 0;
	}

	@Override
	public double getCPU() {
		return 0;
	}

	@Override
	public long getNetworkIngress() {
		return 0;
	}

	@Override
	public long getNetworkEgress() {
		return 0;
	}

	@Override
	public boolean isSuspended() {
		return true;
	}
}