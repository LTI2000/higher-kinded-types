# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

In the following, Claude Code assumes the role of a senior developer, who dislikes noise and clutter, and rather prefers to use clean, albeit advanced, abstractions.

All changes being made should also include documentation and AI related files. We strive to automate automation, and improve AI by AI.

## Commands

```bash
# Build
mvn compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=MaybeTest

# Run a single test method
mvn test -Dtest=MaybeTest#monadLaw_leftIdentity

# Run only jqwik property tests (they are discovered by the jqwik engine, not Jupiter)
mvn test -Dtest=MaybeTest

# Generate Javadoc (output: target/site/apidocs/index.html)
mvn javadoc:javadoc
```

## Architecture

This project demonstrates higher-kinded types (HKT) in Java using the **witness/tag pattern** — a simulation of `F<A>` that Java's type system cannot express natively.

### The core encoding

`HKT<F, A>` is a marker interface that stands in for `F<A>`. Every concrete container implements `HKT<ItsOwnTag, A>` and carries a phantom tag type:

```
HKT<F, A>
├── Maybe<A>       implements HKT<Maybe.Tag, A>,         Monad<Maybe.Tag>
├── ListF<A>       implements HKT<ListF.Tag, A>,         Monad<ListF.Tag>
├── Either<E,A>    implements HKT<Either.Tag<E>, A>,     Monad<Either.Tag<E>>
└── ContT<R,M,A>   implements HKT<ContT.Tag<R,M>, A>,   Monad<ContT.Tag<R,M>>
```

**Narrowing** (`Maybe.narrow(hkt)`) is an unchecked cast back to the concrete type. It is safe because within this package, only one class ever implements each tag.

### Type class hierarchy

```
Functor<F>         fmap
  └── Applicative<F>   pure, splat (<*>)
        └── Monad<F>       bind (>>=)
                           fmap and splat have default implementations derived from bind
```

Each container class directly implements `Monad` — there are no separate companion objects. The singleton instances double as the monad accessor:
- `Maybe.MONAD` — `Nothing.INSTANCE` (typed `Maybe<?>`)
- `ListF.MONAD` — `Nil.INSTANCE` (typed `ListF<?>`); overrides default `fmap`/`splat` for efficiency
- `Either.monad()` — factory returning `new Right<>(null)` (typed `Either<E, Void>`); a factory is necessary because `E` must be fixed at the call site
- `ContT.monad()` — factory returning a trivial `ContT<R, M, Void>`; both `R` and `M` must be fixed at the call site

### Monad transformer (`ContT.java`)

`ContT<R, M, A>` is the continuation monad transformer. It wraps a function `(A → M[R]) → M[R]`, threading any base monad `M` through a continuation-passing chain:

```
pure(a)  = ContT(k → k(a))
m >>= f  = ContT(k → m.run(a → f(a).run(k)))
```

Key operations beyond the monad interface:
- **`ContT.lift(M, ma)`** — embeds a base-monad action: `ContT(k → M.bind(ma, k))`
- **`ContT.callCC(f)`** — captures the current continuation; calling `escape(a)` delivers `a` directly to the outer continuation, bypassing any remaining chain
- **`runContT(k)`** — applies the final continuation `k` to produce an `M[R]` answer

### Combinators (`Combinators.java`)

Three generic combinators that work across any monad:

- **`kleisli(M, f, g)`** — Kleisli composition (`>=>` in Haskell): composes `A → F[B]` with `B → F[C]` into `A → F[C]`
- **`liftA2(A, f, fa, fb)`** — combines two independent applicative values with a binary function
- **`mapM(M, items, f)`** — traverses a list monadically, short-circuiting on the first failure (equivalent to Haskell's `mapM` / `traverse`)

### Testing

Uses **JUnit Jupiter** (`@Test`) for unit tests and **jqwik** (`@Property`, `@ForAll`) for property-based tests. Both engines run on the JUnit Platform via Maven Surefire.

Property tests prove the standard algebraic laws for each type class:
- Functor: identity, composition
- Applicative: identity, homomorphism
- Monad: left identity, right identity, associativity
- Kleisli: left identity, right identity

All test classes are in `src/test/java/lti/` (same package as production code) to access package-private types. `M::pure` cannot be used as a method reference due to Java's type inference limits — use `x -> M.pure(x)` instead.
