package lti;

import java.util.function.Function;

/**
 * Continuation monad transformer.
 *
 * <p>{@code ContT<R, M, A>} wraps a function {@code (A → M[R]) → M[R]}.
 * Monadic sequencing chains continuations; the underlying tag {@code M} is phantom —
 * no constraints on {@code M} are needed for {@code pure} or {@code bind}.
 *
 * <pre>
 *   pure(a)  = ContT(k → k(a))
 *   m >>= f  = ContT(k → m.run(a → f(a).run(k)))
 * </pre>
 *
 * <p>{@link #lift} embeds a base-monad action; {@link #callCC} captures the current
 * continuation to enable non-local exit.
 */
final class ContT<R, M, A> implements HKT<ContT.Tag<R, M>, A>, Monad<ContT.Tag<R, M>> {

    interface Tag<R, M> {}

    final Function<Function<A, HKT<M, R>>, HKT<M, R>> run;

    private ContT(Function<Function<A, HKT<M, R>>, HKT<M, R>> run) {
        this.run = run;
    }

    // ── Factories ─────────────────────────────────────────────────────────────

    static <R, M, A> ContT<R, M, A> of(Function<Function<A, HKT<M, R>>, HKT<M, R>> run) {
        return new ContT<>(run);
    }

    /**
     * Lift a base-monad action into ContT.
     *
     * <p>{@code lift(M, ma) = ContT(k → M.bind(ma, k))}
     */
    static <R, M, A> ContT<R, M, A> lift(Monad<M> M, HKT<M, A> ma) {
        return ContT.<R, M, A>of(k -> M.bind(ma, k));
    }

    /**
     * Call with current continuation.
     *
     * <p>Passes an {@code escape} function to {@code f}. Invoking {@code escape(a)} delivers
     * {@code a} directly to the outer continuation, bypassing any remaining chain.
     * The escape's nominal result type {@code B} is never observed.
     *
     * <p>{@code callCC(f) = ContT(k → f(a → ContT(_ → k(a))).run(k))}
     */
    static <R, M, A, B> ContT<R, M, A> callCC(
            Function<Function<A, ContT<R, M, B>>, ContT<R, M, A>> f) {
        return ContT.<R, M, A>of(k ->
            f.apply(a -> ContT.<R, M, B>of(_ -> k.apply(a))).run.apply(k));
    }

    // ── Runner ────────────────────────────────────────────────────────────────

    /** Apply the final continuation {@code k} to produce an {@code M[R]} answer. */
    HKT<M, R> runContT(Function<A, HKT<M, R>> k) {
        return run.apply(k);
    }

    // ── Monad ─────────────────────────────────────────────────────────────────

    @Override
    public <B> HKT<Tag<R, M>, B> pure(B b) {
        return ContT.<R, M, B>of(k -> k.apply(b));
    }

    @Override
    public <B, C> HKT<Tag<R, M>, C> bind(HKT<Tag<R, M>, B> fb, Function<B, HKT<Tag<R, M>, C>> f) {
        ContT<R, M, B> mb = narrow(fb);
        return ContT.<R, M, C>of(k -> mb.run.apply(b -> narrow(f.apply(b)).run.apply(k)));
    }

    // ── Narrow / witness ──────────────────────────────────────────────────────

    static <R, M, A> ContT<R, M, A> narrow(HKT<Tag<R, M>, A> hkt) {
        return (ContT<R, M, A>) hkt;
    }

    /** Monad witness — {@code R} and {@code M} are fixed at the call site. */
    static <R, M> ContT<R, M, Void> monad() {
        return ContT.<R, M, Void>of(k -> k.apply(null));
    }
}
