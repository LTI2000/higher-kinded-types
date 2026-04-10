package lti;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContTTest {

    // Answer type = Integer, inner monad = Maybe.Tag
    private static final Monad<ContT.Tag<Integer, Maybe.Tag>> M = ContT.monad();

    // Run with the identity continuation: delivers the final value via Maybe.just
    private static int run(HKT<ContT.Tag<Integer, Maybe.Tag>, Integer> hkt) {
        return Maybe.narrow(
            ContT.<Integer, Maybe.Tag, Integer>narrow(hkt).runContT(Maybe::just)
        ).get();
    }

    // ── Functor laws ──────────────────────────────────────────────────────────

    @Property
    void functorLaw_identity(@ForAll int n) {
        assertEquals(run(M.pure(n)), run(M.fmap(M.pure(n), x -> x)));
    }

    @Property
    void functorLaw_composition(@ForAll int n) {
        Function<Integer, Integer> f = x -> x + 1;
        Function<Integer, Integer> g = x -> x * 3;
        HKT<ContT.Tag<Integer, Maybe.Tag>, Integer> fa = M.pure(n);

        assertEquals(
            run(M.fmap(fa, f.compose(g))),
            run(M.fmap(M.fmap(fa, g), f)));
    }

    // ── Monad laws ────────────────────────────────────────────────────────────

    @Property
    void monadLaw_leftIdentity(@ForAll int n) {
        Function<Integer, HKT<ContT.Tag<Integer, Maybe.Tag>, Integer>> f = x -> M.pure(x * 2);
        assertEquals(run(M.bind(M.pure(n), f)), run(f.apply(n)));
    }

    @Property
    void monadLaw_rightIdentity(@ForAll int n) {
        HKT<ContT.Tag<Integer, Maybe.Tag>, Integer> m = M.pure(n);
        assertEquals(run(M.bind(m, x -> M.pure(x))), run(m));
    }

    @Property
    void monadLaw_associativity(@ForAll int n) {
        HKT<ContT.Tag<Integer, Maybe.Tag>, Integer> m = M.pure(n);
        Function<Integer, HKT<ContT.Tag<Integer, Maybe.Tag>, Integer>> f = x -> M.pure(x + 1);
        Function<Integer, HKT<ContT.Tag<Integer, Maybe.Tag>, Integer>> g = x -> M.pure(x * 3);

        assertEquals(
            run(M.bind(M.bind(m, f), g)),
            run(M.bind(m, x -> M.bind(f.apply(x), g))));
    }

    // ── Kleisli laws ──────────────────────────────────────────────────────────

    @Property
    void kleisliLaw_leftIdentity(@ForAll int n) {
        Function<Integer, HKT<ContT.Tag<Integer, Maybe.Tag>, Integer>> f = x -> M.pure(x * 5);
        Function<Integer, HKT<ContT.Tag<Integer, Maybe.Tag>, Integer>> composed =
            Combinators.kleisli(M, (Integer x) -> M.pure(x), f);
        assertEquals(run(composed.apply(n)), run(f.apply(n)));
    }

    @Property
    void kleisliLaw_rightIdentity(@ForAll int n) {
        Function<Integer, HKT<ContT.Tag<Integer, Maybe.Tag>, Integer>> f = x -> M.pure(x * 5);
        Function<Integer, HKT<ContT.Tag<Integer, Maybe.Tag>, Integer>> composed =
            Combinators.kleisli(M, f, (Integer x) -> M.pure(x));
        assertEquals(run(composed.apply(n)), run(f.apply(n)));
    }

    // ── callCC ────────────────────────────────────────────────────────────────

    @Test
    void callCC_noEscape_returnsNormal() {
        ContT<Integer, Maybe.Tag, Integer> ct = ContT.<Integer, Maybe.Tag, Integer, Integer>callCC(
            exit -> ContT.narrow(M.pure(42))
        );
        assertEquals(42, run(ct));
    }

    @Test
    void callCC_escape_bypassesRemainder() {
        // escape(99) delivers 99 to the outer continuation; the subsequent bind is never reached
        ContT<Integer, Maybe.Tag, Integer> ct = ContT.<Integer, Maybe.Tag, Integer, Integer>callCC(exit ->
            ContT.narrow(M.bind(exit.apply(99), x -> M.pure(x + 1000)))
        );
        assertEquals(99, run(ct));
    }

    // ── lift ──────────────────────────────────────────────────────────────────

    @Test
    void lift_embedsBaseMonadValue() {
        ContT<Integer, Maybe.Tag, Integer> ct =
            ContT.<Integer, Maybe.Tag, Integer>lift(Maybe.MONAD, Maybe.just(7));
        assertEquals(7, run(ct));
    }
}
