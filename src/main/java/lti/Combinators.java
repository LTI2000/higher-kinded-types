package lti;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Combinators {
    /**
     * mapM — sequence a list through a monadic function, collecting results.
     * Equivalent to Haskell's mapM / traverse.
     * If any step produces a "failure" (Nothing, Left), the whole thing short-circuits.
     */
    static <F, A, B> HKT<F, List<B>> mapM(
            Monad<F> M,
            List<A> items,
            Function<A, HKT<F, B>> f) {
        HKT<F, List<B>> acc = M.pure(new ArrayList<>());
        for (A item : items) {
            acc = M.bind(acc, collected ->
                M.fmap(f.apply(item), result -> {
                    List<B> next = new ArrayList<>(collected);
                    next.add(result);
                    return (List<B>) next;
                })
            );
        }
        return acc;
    }

    /**
     * Kleisli composition (>=> in Haskell).
     * Composes two monadic functions f: A → F[B] and g: B → F[C] into A → F[C].
     */
    static <F, A, B, C> Function<A, HKT<F, C>> kleisli(
            Monad<F> M,
            Function<A, HKT<F, B>> f,
            Function<B, HKT<F, C>> g) {
        return a -> M.bind(f.apply(a), g);
    }

    /**
     * liftA2 — combine two independent effectful values with a pure function.
     * Equivalent to Haskell's liftA2. Works for any Applicative.
     */
    static <F, A, B, C> HKT<F, C> liftA2(
            Applicative<F> A,
            Function<A, Function<B, C>> f,
            HKT<F, A> fa,
            HKT<F, B> fb) {
        return A.splat(A.fmap(fa, f), fb);
    }
}
