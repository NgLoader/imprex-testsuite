package dev.imprex.testsuite.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

public class SuggestionProvider {

	public static boolean match(String input, String message) {
		char[] words = input.toCharArray();
		for (int i = 0; i < words.length; i++) {
			if (message.charAt(i) != words[i]) {
				return false;
			}
		}
		return true;
	}

	public static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Iterator<String> values) {
		String input = builder.getRemaining().toLowerCase();
		while (values.hasNext()) {
			String value = values.next().toLowerCase();
			if (match(input, value)) {
				builder.suggest(value);
			}
		}
		return builder.buildFuture();
	}

	public static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Collection<String> values) {
		return suggest(builder, values.stream());
	}

	public static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, String[] values) {
		return suggest(builder, Arrays.stream(values));
	}

	public static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Stream<String> values) {
		String input = builder.getRemaining().toLowerCase();
		values.filter(value -> match(input, value.toLowerCase())).forEach(builder::suggest);
		return builder.buildFuture();
	}
}