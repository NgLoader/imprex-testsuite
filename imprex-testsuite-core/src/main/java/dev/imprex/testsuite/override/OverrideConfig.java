package dev.imprex.testsuite.override;

import java.util.Map;

public record OverrideConfig(String parser, Boolean createFileWhenNotExist, Boolean overrideAfterStart,
		Map<String, Object> find) {

	boolean isValid() {
		return this.parser != null && this.createFileWhenNotExist != null && this.overrideAfterStart != null
				&& this.find != null;
	}
}