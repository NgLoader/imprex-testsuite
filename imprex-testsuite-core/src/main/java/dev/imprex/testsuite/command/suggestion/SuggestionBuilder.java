package dev.imprex.testsuite.command.suggestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.imprex.testsuite.util.ArgumentBuilder;
import dev.imprex.testsuite.util.TestsuiteSender;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SuggestionBuilder<TRoot, TOut> {

	private final List<Function<Stream, Stream>> transformations = new ArrayList<>();
	protected final Supplier<Stream<TRoot>> supplier;

	public SuggestionBuilder(Supplier<Stream<TRoot>> supplier) {
		this.supplier = supplier;
	}

	public SuggestionBuilder<TRoot, TOut> filter(Predicate<TOut> predicate) {
		this.transformations.add(stream -> stream.filter(predicate));
		return this;
	}

	public <TMap> SuggestionBuilder<TRoot, TMap> map(Function<TOut, TMap> function) {
		this.transformations.add(stream -> stream.map(function));
		return (SuggestionBuilder<TRoot, TMap>) this;
	}

	public <TMap> SuggestionBuilder<TRoot, TMap> flatMap(Function<TOut, TMap> function) {
		this.transformations.add(stream -> stream.flatMap(function));
		return (SuggestionBuilder<TRoot, TMap>) this;
	}

	public Function<Stream<TRoot>, Stream<TOut>> buildStream() {
		return stream -> {
			for (Function<Stream, Stream> transformation : this.transformations) {
				stream = transformation.apply(stream);
			}
			return (Stream<TOut>) stream;
		};
	}

	public Function<Iterable<TRoot>, Stream<TOut>> buildIterable() {
		return (iterable) -> {
			Stream stream = StreamSupport.stream(iterable.spliterator(), false);
			for (var transformation : transformations) {
				stream = transformation.apply(stream);
			}
			return (Stream<TOut>) stream;
		};
	}

	public SuggestionProvider<TestsuiteSender> buildSuggest() {
		final Function<Stream<TRoot>, Stream<TOut>> transformation = this.buildStream();
		return (context, builder) -> {
			transformation.apply(this.supplier.get())
				.map(Objects::toString)
				.forEach(builder::suggest);
			return builder.buildFuture();
		};
	}

	public SuggestionProvider<TestsuiteSender> buildSuggest(String fieldName) {
		final Function<Stream<TRoot>, Stream<String>> transformation = this.map(Objects::toString).buildStream();

		return (context, builder) -> {
			String input = ArgumentBuilder.getSafeStringArgument(context, fieldName);

			transformation.apply(this.supplier.get())
				.map(Objects::toString)
				.filter(name -> {
					if (name.toLowerCase().contains(input)) {
						return true;
					}
					return false;
				})
				.forEach(builder::suggest);
			return builder.buildFuture();
		};
	}
}