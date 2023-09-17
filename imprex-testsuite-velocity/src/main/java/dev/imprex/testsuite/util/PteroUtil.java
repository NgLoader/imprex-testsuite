package dev.imprex.testsuite.util;

import java.util.concurrent.CompletableFuture;

import com.mattmalec.pterodactyl4j.PteroAction;

public class PteroUtil {

	public static <T> CompletableFuture<T> execute(PteroAction<T> action) {
		CompletableFuture<T> future = new CompletableFuture<>();
		action.executeAsync(future::complete, future::completeExceptionally);
		return future;
	}
}
