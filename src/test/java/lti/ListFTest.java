package lti;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ListFTest {

    private static final Monad<ListF.Tag> L = ListF.MONAD;

    // -------------------------------------------------------------------------
    // Functor
    // -------------------------------------------------------------------------

    @Test
    void fmap_squaresAllElements() {
        HKT<ListF.Tag, Integer> result = L.fmap(ListF.of(1, 2, 3, 4), x -> x * x);
        assertEquals(List.of(1, 4, 9, 16), ListF.narrow(result).values);
    }

    // -------------------------------------------------------------------------
    // Applicative — cartesian product
    // -------------------------------------------------------------------------

    @Test
    void splat_cartesianProduct() {
        HKT<ListF.Tag, Function<Integer, Integer>> fns =
            ListF.of(List.of(x -> x + 10, x -> x * 2));
        HKT<ListF.Tag, Integer> result = L.splat(fns, ListF.of(1, 2, 3));
        assertEquals(List.of(11, 12, 13, 2, 4, 6), ListF.narrow(result).values);
    }

    // -------------------------------------------------------------------------
    // Monad — flatMap / concatMap
    // -------------------------------------------------------------------------

    @Test
    void bind_mirrorElements() {
        HKT<ListF.Tag, Integer> result = L.bind(ListF.of(1, 2, 3), n -> ListF.of(n, -n));
        assertEquals(List.of(1, -1, 2, -2, 3, -3), ListF.narrow(result).values);
    }

    // -------------------------------------------------------------------------
    // Property: Functor laws
    //
    // 1. Identity:    fmap(id)    ≡ id
    // 2. Composition: fmap(f ∘ g) ≡ fmap(f) ∘ fmap(g)
    // -------------------------------------------------------------------------

    @Property
    void functorLaw_identity(@ForAll List<Integer> values) {
        List<Integer> result = ListF.narrow(L.fmap(ListF.of(values), x -> x)).values;
        assertEquals(values, result);
    }

    @Property
    void functorLaw_composition(@ForAll List<Integer> values) {
        Function<Integer, Integer> f = x -> x + 1;
        Function<Integer, Integer> g = x -> x * 3;
        HKT<ListF.Tag, Integer> fa = ListF.of(values);

        List<Integer> lhs = ListF.narrow(L.fmap(fa, f.compose(g))).values;   // fmap (f ∘ g)
        List<Integer> rhs = ListF.narrow(L.fmap(L.fmap(fa, g), f)).values;   // fmap f ∘ fmap g
        assertEquals(lhs, rhs);
    }

    // -------------------------------------------------------------------------
    // Property: Applicative laws
    //
    // 1. Identity:      pure(id) <*> v        ≡ v
    // 2. Homomorphism:  pure(f)  <*> pure(a)  ≡ pure(f(a))
    // -------------------------------------------------------------------------

    @Property
    void applicativeLaw_identity(@ForAll List<Integer> values) {
        HKT<ListF.Tag, Function<Integer, Integer>> pureId = L.pure(Function.identity());
        List<Integer> result = ListF.narrow(L.splat(pureId, ListF.of(values))).values;
        assertEquals(values, result);
    }

    @Property
    void applicativeLaw_homomorphism(@ForAll int a) {
        Function<Integer, Integer> f = x -> x * 3;
        List<Integer> lhs = ListF.narrow(L.splat(L.pure(f), L.pure(a))).values;
        List<Integer> rhs = ListF.narrow(L.pure(f.apply(a))).values;
        assertEquals(lhs, rhs);
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
        Function<Integer, HKT<ListF.Tag, Integer>> f = x -> ListF.of(x, -x);
        List<Integer> lhs = ListF.narrow(L.bind(L.pure(a), f)).values;
        List<Integer> rhs = ListF.narrow(f.apply(a)).values;
        assertEquals(lhs, rhs);
    }

    @Property
    void monadLaw_rightIdentity(@ForAll List<Integer> values) {
        HKT<ListF.Tag, Integer> m = ListF.of(values);
        List<Integer> result = ListF.narrow(L.bind(m, x -> L.pure(x))).values;
        assertEquals(values, result);
    }

    @Property
    void monadLaw_associativity(@ForAll List<Integer> values) {
        HKT<ListF.Tag, Integer> m = ListF.of(values);
        Function<Integer, HKT<ListF.Tag, Integer>> f = x -> ListF.of(x, x * 2);
        Function<Integer, HKT<ListF.Tag, Integer>> g = x -> ListF.of(x - 1, x + 1);

        List<Integer> lhs = ListF.narrow(L.bind(L.bind(m, f), g)).values;
        List<Integer> rhs = ListF.narrow(L.bind(m, a -> L.bind(f.apply(a), g))).values;
        assertEquals(lhs, rhs);
    }

    // -------------------------------------------------------------------------
    // Property: empty list is absorbing for bind
    // -------------------------------------------------------------------------

    @Property
    void bind_emptyList_alwaysEmpty(@ForAll List<Integer> ignored) {
        HKT<ListF.Tag, Integer> result = L.bind(ListF.of(List.of()), x -> ListF.of(ignored));
        assertTrue(ListF.narrow(result).values.isEmpty());
    }
}
