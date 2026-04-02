package lti;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class EitherTest {

    private static final Monad<Either.Tag<String>> E = Either.monad();

    private static final Function<Integer, HKT<Either.Tag<String>, Integer>> parsePositive =
        n -> n > 0 ? Either.right(n) : Either.left("Expected positive, got: " + n);

    private static final Function<Integer, HKT<Either.Tag<String>, Integer>> parseEven =
        n -> n % 2 == 0 ? Either.right(n) : Either.left("Expected even, got: " + n);

    private static final Function<Integer, HKT<Either.Tag<String>, Integer>> parsePosEven =
        Combinators.kleisli(E, parsePositive, parseEven);

    // -------------------------------------------------------------------------
    // Kleisli composition — typed error handling
    // -------------------------------------------------------------------------

    @Test
    void parsePosEven_positiveEven_returnsRight() {
        assertRight(4, parsePosEven.apply(4));
    }

    @Test
    void parsePosEven_negative_failsAtPositiveCheck() {
        assertLeft("Expected positive, got: -2", parsePosEven.apply(-2));
    }

    @Test
    void parsePosEven_positiveOdd_failsAtEvenCheck() {
        assertLeft("Expected even, got: 3", parsePosEven.apply(3));
    }

    // -------------------------------------------------------------------------
    // mapM
    // -------------------------------------------------------------------------

    @Test
    void mapM_allValid_collectsResults() {
        HKT<Either.Tag<String>, List<Integer>> result =
            Combinators.mapM(E, List.of(4, 6, 8), parsePosEven);
        assertRight(List.of(4, 6, 8), result);
    }

    @Test
    void mapM_withOdd_returnsFirstError() {
        HKT<Either.Tag<String>, List<Integer>> result =
            Combinators.mapM(E, List.of(4, 3, 8), parsePosEven);
        assertLeft("Expected even, got: 3", result);
    }

    // -------------------------------------------------------------------------
    // Property: Monad laws
    //
    // 1. Left identity:  pure(a) >>= f   ≡ f(a)
    // 2. Right identity: m       >>= pure ≡ m
    // 3. Associativity:  (m >>= f) >>= g ≡ m >>= (a -> f(a) >>= g)
    // -------------------------------------------------------------------------

    @Property
    void monadLaw_leftIdentity(@ForAll int a) {
        Either<String, Integer> lhs = Either.narrow(E.bind(E.pure(a), parsePositive));
        Either<String, Integer> rhs = Either.narrow(parsePositive.apply(a));
        assertEitherEquals(lhs, rhs);
    }

    @Property
    void monadLaw_rightIdentity_right(@ForAll int a) {
        Either<String, Integer> m = Either.right(a);
        Either<String, Integer> result = Either.narrow(E.bind(m, x -> E.pure(x)));
        assertTrue(result.isRight());
        assertEquals(a, result.getRight());
    }

    @Property
    void monadLaw_rightIdentity_left(@ForAll String msg) {
        Either<String, Integer> m = Either.left(msg);
        Either<String, Integer> result = Either.narrow(E.bind(m, x -> E.pure(x)));
        assertTrue(result.isLeft());
        assertEquals(msg, result.getLeft());
    }

    @Property
    void monadLaw_associativity(@ForAll int a) {
        HKT<Either.Tag<String>, Integer> m = Either.right(a);

        Either<String, Integer> lhs = Either.narrow(E.bind(E.bind(m, parsePositive), parseEven));
        Either<String, Integer> rhs = Either.narrow(E.bind(m, x -> E.bind(parsePositive.apply(x), parseEven)));
        assertEitherEquals(lhs, rhs);
    }

    // -------------------------------------------------------------------------
    // Property: Left is absorbing — Left short-circuits all subsequent binds
    // -------------------------------------------------------------------------

    @Property
    void left_absorbsAllSubsequentBinds(@ForAll String error, @ForAll int ignored) {
        Either<String, Integer> left = Either.left(error);
        Either<String, Integer> result = Either.narrow(E.bind(left, x -> Either.right(x + ignored)));
        assertTrue(result.isLeft());
        assertEquals(error, result.getLeft());
    }

    // -------------------------------------------------------------------------
    // Property: Kleisli identity laws
    //
    // 1. Left identity:  pure >=> f ≡ f
    // 2. Right identity: f >=> pure ≡ f
    // -------------------------------------------------------------------------

    @Property
    void kleisliLaw_leftIdentity(@ForAll int a) {
        Function<Integer, HKT<Either.Tag<String>, Integer>> composed =
            Combinators.kleisli(E, (Integer x) -> E.pure(x), parsePositive);
        assertEitherEquals(Either.narrow(composed.apply(a)), Either.narrow(parsePositive.apply(a)));
    }

    @Property
    void kleisliLaw_rightIdentity(@ForAll int a) {
        Function<Integer, HKT<Either.Tag<String>, Integer>> composed =
            Combinators.kleisli(E, parsePositive, (Integer x) -> E.pure(x));
        assertEitherEquals(Either.narrow(composed.apply(a)), Either.narrow(parsePositive.apply(a)));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void assertRight(Object expected, HKT<Either.Tag<String>, ?> hkt) {
        Either<String, ?> e = Either.narrow(hkt);
        assertTrue(e.isRight(), "Expected Right(" + expected + ") but got Left(" + e.getLeft() + ")");
        assertEquals(expected, e.getRight());
    }

    private static void assertLeft(String expected, HKT<Either.Tag<String>, ?> hkt) {
        Either<String, ?> e = Either.narrow(hkt);
        assertTrue(e.isLeft(), "Expected Left(" + expected + ") but got Right(" + e.getRight() + ")");
        assertEquals(expected, e.getLeft());
    }

    private static void assertEitherEquals(Either<String, Integer> a, Either<String, Integer> b) {
        assertEquals(a.isRight(), b.isRight(), "Sidedness differs");
        if (a.isRight()) assertEquals(a.getRight(), b.getRight());
        else assertEquals(a.getLeft(), b.getLeft());
    }
}
