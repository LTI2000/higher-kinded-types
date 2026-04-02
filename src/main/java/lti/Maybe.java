package lti;

import java.util.NoSuchElementException;
import java.util.function.Function;

sealed abstract class Maybe<A> implements HKT<Maybe.Tag, A> permits Maybe.Just, Maybe.Nothing {
  interface Tag {}

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

  static <A> Maybe<A> just(A a) { return new Just<>(a); }

  @SuppressWarnings("unchecked")
  static <A> Maybe<A> nothing() { return (Maybe<A>) Nothing.INSTANCE; }

  static <A> Maybe<A> narrow(HKT<Maybe.Tag, A> hkt) { return (Maybe<A>) hkt; }
}
