package dev.imprex.testsuite.override;

public class OverrideException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OverrideException(String message) {
		super(message);
	}

	public OverrideException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
