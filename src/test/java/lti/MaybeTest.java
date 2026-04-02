package lti;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class MaybeTest {

    private static final MaybeMonad M = MaybeMonad.INSTANCE;

    // -------------------------------------------------------------------------
    // Functor
    // -------------------------------------------------------------------------

    @Test
    void fmap_just_appliesFunction() {
        assertJust(10, M.fmap(Maybe.just(5), x -> x * 2));
    }

    @Test
    void fmap_nothing_remainsNothing() {
        assertNothing(M.fmap(Maybe.<Integer>nothing(), x -> x * 2));
    }

    // -------------------------------------------------------------------------
    // Monad — safe division chain
    // -------------------------------------------------------------------------

    @Test
    void bind_safeDivChain_succeeds() {
        // 100 / 5 / 4 = 5
        HKT<Maybe.Tag, Integer> result =
            M.bind(M.bind(M.pure(100), safeDiv(5)), safeDiv(4));
        assertJust(5, result);
    }

    @Test
    void bind_safeDivByZero_shortCircuits() {
        // 100 / 0 / 4 — Nothing propagates
        HKT<Maybe.Tag, Integer> result =
            M.bind(M.bind(M.pure(100), safeDiv(0)), safeDiv(4));
        assertNothing(result);
    }

    // -------------------------------------------------------------------------
    // Kleisli composition
    // -------------------------------------------------------------------------

    @Test
    void kleisli_halfThenThird_composesCorrectly() {
        // 60 / 2 / 3 = 10
        Function<Integer, HKT<Maybe.Tag, Integer>> composed =
            Combinators.kleisli(M, safeDiv(2), safeDiv(3));
        assertJust(10, composed.apply(60));
    }

    @Test
    void kleisli_withDivByZero_propagatesNothing() {
        Function<Integer, HKT<Maybe.Tag, Integer>> composed =
            Combinators.kleisli(M, safeDiv(0), safeDiv(3));
        assertNothing(composed.apply(60));
    }

    // -------------------------------------------------------------------------
    // Applicative — liftA2
    // -------------------------------------------------------------------------

    @Test
    void liftA2_bothJust_combinesValues() {
        HKT<Maybe.Tag, Integer> result = Combinators.liftA2(M,
            (Integer x) -> (Integer y) -> x + y,
            Maybe.just(3), Maybe.just(7));
        assertJust(10, result);
    }

    @Test
    void liftA2_withNothing_returnsNothing() {
        HKT<Maybe.Tag, Integer> result = Combinators.liftA2(M,
            (Integer x) -> (Integer y) -> x + y,
            Maybe.just(3), Maybe.<Integer>nothing());
        assertNothing(result);
    }

    // -------------------------------------------------------------------------
    // mapM
    // -------------------------------------------------------------------------

    @Test
    void mapM_allPositive_collectsSquares() {
        HKT<Maybe.Tag, List<Integer>> result = Combinators.mapM(M,
            List.of(2, 4, 6),
            n -> n > 0 ? Maybe.just(n * n) : Maybe.<Integer>nothing());
        assertJust(List.of(4, 16, 36), result);
    }

    @Test
    void mapM_withNegative_shortCircuits() {
        HKT<Maybe.Tag, List<Integer>> result = Combinators.mapM(M,
            List.of(2, -1, 6),
            n -> n > 0 ? Maybe.just(n * n) : Maybe.<Integer>nothing());
        assertNothing(result);
    }

    // -------------------------------------------------------------------------
    // Property: Functor laws
    //
    // 1. Identity:    fmap(id)    ≡ id
    // 2. Composition: fmap(f ∘ g) ≡ fmap(f) ∘ fmap(g)
    // -------------------------------------------------------------------------

    @Property
    void functorLaw_identity_just(@ForAll int value) {
        Maybe<Integer> result = Maybe.narrow(M.fmap(Maybe.just(value), x -> x));
        assertFalse(result.isNothing());
        assertEquals(value, result.get());
    }

    @Property
    void functorLaw_composition_just(@ForAll int value) {
        Function<Integer, Integer> f = x -> x + 1;
        Function<Integer, Integer> g = x -> x * 3;
        HKT<Maybe.Tag, Integer> fa = Maybe.just(value);

        Maybe<Integer> lhs = Maybe.narrow(M.fmap(fa, f.compose(g)));       // fmap (f ∘ g)
        Maybe<Integer> rhs = Maybe.narrow(M.fmap(M.fmap(fa, g), f));       // fmap f ∘ fmap g
        assertMaybeEquals(lhs, rhs);
    }

    @Test
    void functorLaw_identity_nothing() {
        assertTrue(Maybe.narrow(M.fmap(Maybe.<Integer>nothing(), x -> x)).isNothing());
    }

    @Test
    void functorLaw_composition_nothing() {
        Function<Integer, Integer> f = x -> x + 1;
        Function<Integer, Integer> g = x -> x * 3;
        HKT<Maybe.Tag, Integer> nothing = Maybe.nothing();

        Maybe<Integer> lhs = Maybe.narrow(M.fmap(nothing, f.compose(g)));
        Maybe<Integer> rhs = Maybe.narrow(M.fmap(M.fmap(nothing, g), f));
        assertTrue(lhs.isNothing());
        assertTrue(rhs.isNothing());
    }

    // -------------------------------------------------------------------------
    // Property: Monad laws
    //
    // 1. Left identity:  pure(a) >>= f       ≡ f(a)
    // 2. Right identity: m       >>= pure     ≡ m
    // 3. Associativity:  (m >>= f) >>= g     ≡ m >>= (a -> f(a) >>= g)
    // -------------------------------------------------------------------------

    @Property
    void monadLaw_leftIdentity(@ForAll int a) {
        Function<Integer, HKT<Maybe.Tag, Integer>> f =
            x -> x > 0 ? Maybe.just(x * 2) : Maybe.nothing();

        Maybe<Integer> lhs = Maybe.narrow(M.bind(M.pure(a), f));
        Maybe<Integer> rhs = Maybe.narrow(f.apply(a));
        assertMaybeEquals(lhs, rhs);
    }

    @Property
    void monadLaw_rightIdentity_just(@ForAll int value) {
        Maybe<Integer> result = Maybe.narrow(M.bind(Maybe.just(value), x -> M.pure(x)));
        assertFalse(result.isNothing());
        assertEquals(value, result.get());
    }

    @Test
    void monadLaw_rightIdentity_nothing() {
        assertTrue(Maybe.narrow(M.bind(Maybe.<Integer>nothing(), x -> M.pure(x))).isNothing());
    }

    @Property
    void monadLaw_associativity(@ForAll int a) {
        HKT<Maybe.Tag, Integer> m = Maybe.just(a);
        Function<Integer, HKT<Maybe.Tag, Integer>> f =
            x -> x != 0 ? Maybe.just(x * 2) : Maybe.nothing();
        Function<Integer, HKT<Maybe.Tag, Integer>> g =
            x -> x > 0 ? Maybe.just(x + 1) : Maybe.nothing();

        Maybe<Integer> lhs = Maybe.narrow(M.bind(M.bind(m, f), g));
        Maybe<Integer> rhs = Maybe.narrow(M.bind(m, x -> M.bind(f.apply(x), g)));
        assertMaybeEquals(lhs, rhs);
    }

    // -------------------------------------------------------------------------
    // Property: Kleisli identity laws
    //
    // 1. Left identity:  pure >=> f ≡ f
    // 2. Right identity: f >=> pure ≡ f
    // -------------------------------------------------------------------------

    @Property
    void kleisliLaw_leftIdentity(@ForAll int a) {
        Function<Integer, HKT<Maybe.Tag, Integer>> f =
            x -> x > 0 ? Maybe.just(x * 10) : Maybe.nothing();

        Maybe<Integer> lhs = Maybe.narrow(Combinators.kleisli(M, (Integer x) -> M.pure(x), f).apply(a));
        Maybe<Integer> rhs = Maybe.narrow(f.apply(a));
        assertMaybeEquals(lhs, rhs);
    }

    @Property
    void kleisliLaw_rightIdentity(@ForAll int a) {
        Function<Integer, HKT<Maybe.Tag, Integer>> f =
            x -> x > 0 ? Maybe.just(x * 10) : Maybe.nothing();

        Maybe<Integer> lhs = Maybe.narrow(Combinators.kleisli(M, f, (Integer x) -> M.pure(x)).apply(a));
        Maybe<Integer> rhs = Maybe.narrow(f.apply(a));
        assertMaybeEquals(lhs, rhs);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Function<Integer, HKT<Maybe.Tag, Integer>> safeDiv(int denom) {
        return num -> denom == 0 ? Maybe.nothing() : Maybe.just(num / denom);
    }

    private static void assertJust(Object expected, HKT<Maybe.Tag, ?> hkt) {
        Maybe<?> m = Maybe.narrow(hkt);
        assertFalse(m.isNothing(), "Expected Just(" + expected + ") but got Nothing");
        assertEquals(expected, m.get());
    }

    private static void assertNothing(HKT<Maybe.Tag, ?> hkt) {
        assertTrue(Maybe.narrow(hkt).isNothing(), "Expected Nothing but got a value");
    }

    private static void assertMaybeEquals(Maybe<?> a, Maybe<?> b) {
        assertEquals(a.isNothing(), b.isNothing(), "One is Nothing, the other is not");
        if (!a.isNothing()) assertEquals(a.get(), b.get());
    }
}
