package dev.imprex.testsuite.command.suggestion;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.imprex.testsuite.template.ServerTemplate;
import dev.imprex.testsuite.util.ArgumentBuilder;
import dev.imprex.testsuite.util.TestsuiteSender;

public class TemplateSuggestionBuilder extends SuggestionBuilder<ServerTemplate, ServerTemplate> {

	public TemplateSuggestionBuilder(Supplier<Stream<ServerTemplate>> supplier) {
		super(supplier);
	}

	@Override
	public SuggestionProvider<TestsuiteSender> buildSuggest() {
		final Function<Stream<ServerTemplate>, Stream<String>> transformation = this.map(ServerTemplate::getName)
				.buildStream();

		return (context, builder) -> {
			transformation.apply(this.supplier.get())
				.map(Objects::toString)
				.forEach(builder::suggest);
			return builder.buildFuture();
		};
	}

	@Override
	public SuggestionProvider<TestsuiteSender> buildSuggest(String fieldName) {
		final Function<Stream<ServerTemplate>, Stream<String>> transformation = this.map(ServerTemplate::getName)
				.buildStream();

		return (context, builder) -> {
			String input = ArgumentBuilder.getSafeStringArgument(context, fieldName);
			String[] keywords = input.toLowerCase().split("[-_. ]");

			transformation.apply(this.supplier.get())
				.map(Objects::toString)
				.filter(name -> {
					for (String keyword : keywords) {
						if (!name.toLowerCase().contains(keyword)) {
							return false;
						}
					}
					return true;
				})
				.forEach(builder::suggest);
			return builder.buildFuture();
		};
	}
}