package lti;

import java.util.Optional;
import java.util.function.Function;

/**
 * The concrete Maybe container. Wraps Optional internally.
 */
final class Maybe<A> implements HKT<Maybe.Tag, A> {
  /**
   * Phantom tag for Maybe.
   */
  interface Tag {
  }

  static final Monad<Tag> MONAD = new Monad<>() {
    @Override
    public <A> HKT<Tag, A> pure(A a) {
      return Maybe.just(a);
    }

    @Override
    public <A, B> HKT<Tag, B> bind(HKT<Tag, A> fa, Function<A, HKT<Tag, B>> f) {
      Maybe<A> ma = Maybe.narrow(fa);
      return ma.isNothing() ? Maybe.nothing() : f.apply(ma.get());
    }
  };

  private final Optional<A> value;

  private Maybe(Optional<A> value) {
    this.value = value;
  }

  public static <A> Maybe<A> just(A a) {
    return new Maybe<>(Optional.of(a));
  }

  public static <A> Maybe<A> nothing() {
    return new Maybe<>(Optional.empty());
  }

  public boolean isNothing() {
    return value.isEmpty();
  }

  public A get() {
    return value.get();
  }

  public Optional<A> toOptional() {
    return value;
  }

  @Override
  public String toString() {
    return value.map(v -> "Just(" + v + ")").orElse("Nothing");
  }

  /**
   * Narrow App<MaybeTag, A> back to Maybe<A> — safe because the only
   * implementor of App<MaybeTag, ?> in this codebase is Maybe.
   */
  public static <A> Maybe<A> narrow(HKT<Maybe.Tag, A> HKT) {
    return (Maybe<A>) HKT;
  }
}
