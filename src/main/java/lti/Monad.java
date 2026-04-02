package lti;

import java.util.function.Function;

interface Monad<F> extends Applicative<F> {
  <A, B> HKT<F, B> bind(HKT<F, A> fa, Function<A, HKT<F, B>> f);

  // fmap is derivable from bind + pure — a default implementation
  @Override
  default <A, B> HKT<F, B> fmap(HKT<F, A> fa, Function<A, B> f) {
    return bind(fa, a -> pure(f.apply(a)));
  }

  // splat is also derivable from bind — Applicative for free
  @Override
  default <A, B> HKT<F, B> splat(HKT<F, Function<A, B>> ff, HKT<F, A> fa) {
    return bind(ff, f -> fmap(fa, f));
  }
}
