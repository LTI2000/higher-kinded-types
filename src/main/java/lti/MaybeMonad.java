package lti;

import java.util.function.Function;

/**
 * The Maybe monad instance. A singleton — no state needed.
 */
final class MaybeMonad implements Monad<Maybe.Tag> {

  static final MaybeMonad INSTANCE = new MaybeMonad();

  private MaybeMonad() {
  }

  @Override
  public <A> HKT<Maybe.Tag, A> pure(A a) {
    return Maybe.just(a);
  }

  @Override
  public <A, B> HKT<Maybe.Tag, B> bind(
          HKT<Maybe.Tag, A> fa,
          Function<A, HKT<Maybe.Tag, B>> f) {
    Maybe<A> ma = Maybe.narrow(fa);
    return ma.isNothing() ? Maybe.nothing() : f.apply(ma.get());
  }
}
