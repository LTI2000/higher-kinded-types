package lti;

import java.util.function.Function;

interface Functor<F> {
  <A, B> HKT<F, B> fmap(HKT<F, A> fa, Function<A, B> f);
}
