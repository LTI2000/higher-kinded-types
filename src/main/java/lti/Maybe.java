package lti;

import java.util.Optional;

/**
 * The concrete Maybe container. Wraps Optional internally.
 */
final class Maybe<A> implements HKT<Maybe.Tag, A> {
  /**
   * Phantom tag for Maybe.
   */
  interface Tag {
  }

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
