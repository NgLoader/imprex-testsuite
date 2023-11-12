package dev.imprex.testsuite.override;

import java.util.Map;

public record OverrideConfig(String parser, Boolean overrideAfterFirstStart, Map<String, Object> find) {

}