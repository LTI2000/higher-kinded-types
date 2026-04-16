package lti;

import java.util.function.Function;

/**
 * Extends {@link Functor} with the ability to lift pure values and apply wrapped functions
 * ({@code pure} and {@code <*>} in Haskell).
 *
 * @param <F> phantom tag identifying the container
 */
interface Applicative<F> extends Functor<F> {

  /** Lifts a pure value {@code a} into the container. */
  <A> HKT<F, A> pure(A a);

  /**
   * Sequential application: applies the wrapped function {@code ff} to the wrapped value
   * {@code fa} ({@code <*>} in Haskell).
   */
  <A, B> HKT<F, B> splat(HKT<F, Function<A, B>> ff, HKT<F, A> fa);
}
