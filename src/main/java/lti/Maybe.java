package lti;

import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Optional value — either {@link Just} wrapping a value or {@link Nothing} for absence.
 *
 * <p>Models Haskell's {@code Maybe}. The monad instance short-circuits on {@code Nothing}:
 * once absent, no subsequent {@code bind} step runs.
 *
 * <p>Use {@code Maybe.MONAD} as the monad witness; it is the {@code Nothing} singleton
 * typed as {@code Maybe<?>}.
 *
 * @param <A> the wrapped value type
 */
sealed abstract class Maybe<A> implements HKT<Maybe.Tag, A>, Monad<Maybe.Tag>
    permits Maybe.Just, Maybe.Nothing {

  /** Phantom tag identifying {@code Maybe} in the HKT encoding. */
  interface Tag {}

  static final class Just<A> extends Maybe<A> {
    final A value;

    Just(A value) { this.value = value; }

    @Override public boolean isNothing() { return false; }
    @Override public A get() { return value; }
    @Override public String toString() { return "Just(" + value + ")"; }
  }

  static final class Nothing<A> extends Maybe<A> {
    @SuppressWarnings("rawtypes")
    private static final Nothing INSTANCE = new Nothing();

    private Nothing() {}

    @Override public boolean isNothing() { return true; }
    @Override public A get() { throw new NoSuchElementException("Nothing.get"); }
    @Override public String toString() { return "Nothing"; }
  }

  abstract boolean isNothing();
  abstract A get();

  // -------------------------------------------------------------------------
  // Monad
  // -------------------------------------------------------------------------

  @Override
  public <B> HKT<Tag, B> pure(B b) { return Maybe.just(b); }

  @Override
  public <B, C> HKT<Tag, C> bind(HKT<Tag, B> fb, Function<B, HKT<Tag, C>> f) {
    Maybe<B> mb = Maybe.narrow(fb);
    return mb.isNothing() ? Maybe.nothing() : f.apply(mb.get());
  }

  // -------------------------------------------------------------------------
  // Factories
  // -------------------------------------------------------------------------

  static <A> Maybe<A> just(A a) { return new Just<>(a); }

  @SuppressWarnings("unchecked")
  static <A> Maybe<A> nothing() { return (Maybe<A>) Nothing.INSTANCE; }

  static <A> Maybe<A> narrow(HKT<Maybe.Tag, A> hkt) { return (Maybe<A>) hkt; }

  static final Maybe<?> MONAD = Nothing.INSTANCE;
}
