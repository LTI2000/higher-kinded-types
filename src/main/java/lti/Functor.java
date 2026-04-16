package lti;

import java.util.function.Function;

/**
 * Maps a pure function over a wrapped value ({@code <$>} in Haskell).
 *
 * <p>Laws:
 * <pre>
 *   fmap(id)    = id
 *   fmap(f ∘ g) = fmap(f) ∘ fmap(g)
 * </pre>
 *
 * @param <F> phantom tag identifying the container
 */
interface Functor<F> {

  /**
   * Applies {@code f} to the value(s) inside {@code fa}, preserving the outer structure.
   */
  <A, B> HKT<F, B> fmap(HKT<F, A> fa, Function<A, B> f);
}
