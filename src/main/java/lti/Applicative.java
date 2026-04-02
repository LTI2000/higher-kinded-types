package lti;

import java.util.function.Function;

interface Applicative<F> extends Functor<F> {
  <A> HKT<F, A> pure(A a);

  <A, B> HKT<F, B> splat(HKT<F, Function<A, B>> ff, HKT<F, A> fa);
}
