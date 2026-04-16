package lti;

import java.util.function.Function;

/**
 * Extends {@link Applicative} with monadic sequencing ({@code >>=} in Haskell).
 *
 * <p>Default implementations of {@code fmap} and {@code splat} are derived from {@code bind},
 * giving Functor and Applicative for free.
 *
 * <p>Laws:
 * <pre>
 *   pure(a) >>= f            = f(a)                      // left identity
 *   m >>= pure               = m                         // right identity
 *   (m >>= f) >>= g          = m >>= (a → f(a) >>= g)   // associativity
 * </pre>
 *
 * @param <F> phantom tag identifying the container
 */
interface Monad<F> extends Applicative<F> {

  /**
   * Sequences a monadic action: applies {@code f} to the value inside {@code fa} and
   * flattens the result ({@code >>=} in Haskell).
   */
  <A, B> HKT<F, B> bind(HKT<F, A> fa, Function<A, HKT<F, B>> f);

  /** Derived from {@code bind} and {@code pure}. */
  @Override
  default <A, B> HKT<F, B> fmap(HKT<F, A> fa, Function<A, B> f) {
    return bind(fa, a -> pure(f.apply(a)));
  }

  /** Derived from {@code bind}. */
  @Override
  default <A, B> HKT<F, B> splat(HKT<F, Function<A, B>> ff, HKT<F, A> fa) {
    return bind(ff, f -> fmap(fa, f));
  }
}
