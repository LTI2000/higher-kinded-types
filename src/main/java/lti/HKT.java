package lti;

// Higher Kinded Types in Java
//
// Java has no native HKT support (no F<_> syntax). We use the same
// "witness" (HKT<F, A>) pattern as the Go version, but Java's generics
// let us preserve A end-to-end — no Object/any casts at call sites.
//
// Key idea: HKT<F, A> is a stand-in for F<A>.
//   F  — a phantom "tag" type (empty interface), identifies the container.
//   A  — the value type, fully tracked by the compiler.
//
// Hierarchy (mirrors Haskell):
//   Functor<F>        — fmap (<$>)
//   Applicative<F>    — extends Functor, adds pure + splat (<*>)
//   Monad<F>          — extends Applicative, adds bind (>>=)

/**
 * {@code HKT<F, A>} is the stand-in for {@code F<A>}.
 * Concrete containers implement this interface for their tag {@code F}.
 * Because {@code A} is a real generic parameter, the compiler tracks it fully.
 */
interface HKT<F, A> {
}
