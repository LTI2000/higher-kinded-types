# Higher-Kinded Types in Java

Java has no native higher-kinded type support — you cannot write `F<A>` where `F` itself is a type parameter. This project simulates it using the **witness/tag pattern**, making Functor, Applicative, and Monad expressible as real interfaces.

## The encoding

`HKT<F, A>` stands in for `F<A>`. Each container declares an empty inner interface as its phantom tag and implements `HKT` for that tag:

```java
// Stand-in for F<A>
interface HKT<F, A> {}

// Maybe<A> ≅ HKT<Maybe.Tag, A>
sealed abstract class Maybe<A> implements HKT<Maybe.Tag, A> permits Maybe.Just, Maybe.Nothing {
  interface Tag {}
  ...
}
```

The type class hierarchy mirrors Haskell's:

```
Functor<F>       — fmap
  Applicative<F> — pure, splat (<*>)
    Monad<F>     — bind (>>=)   [fmap and splat derived by default]
```

## Containers

| Type | Variants / structure | Monad instance |
|---|---|---|
| `Maybe<A>` | `Just<A>`, `Nothing<A>` (singleton) | `Maybe.MONAD` |
| `Either<E, A>` | `Left<E, A>`, `Right<E, A>` | `Either.monad()` |
| `ListF<A>` | wraps `List<A>` | `ListF.MONAD` |
| `ContT<R, M, A>` | wraps `(A → M[R]) → M[R]` | `ContT.monad()` |

`Maybe` and `Either` are proper sealed sum types. `Either.monad()` and `ContT.monad()` are factories rather than static fields because their extra type parameters (`E`, `R`, `M`) must be fixed at the call site.

## Monad transformer

`ContT<R, M, A>` is the continuation monad transformer. It sequences computations in continuation-passing style over any base monad `M`:

```java
// embed a base-monad action
ContT<Integer, Maybe.Tag, Integer> ct = ContT.lift(Maybe.MONAD, Maybe.just(7));

// non-local exit: escape(99) bypasses any remaining bind chain
ContT<Integer, Maybe.Tag, Integer> ct2 = ContT.callCC(exit ->
    ContT.narrow(M.bind(exit.apply(99), x -> M.pure(x + 1000)))
);

// run with a final continuation to produce M[R]
HKT<Maybe.Tag, Integer> result = ct.runContT(Maybe::just);
```

## Combinators

`Combinators.java` provides three generic operations over any monad:

```java
// Kleisli composition (>=>): A → F[B]  and  B → F[C]  into  A → F[C]
kleisli(M, f, g)

// liftA2: combine two independent effects with a binary function
liftA2(A, f, fa, fb)

// mapM / traverse: apply a monadic function to a list, short-circuiting on failure
mapM(M, items, f)
```

## Building and testing

```bash
mvn compile
mvn test

# Single test class
mvn test -Dtest=MaybeTest

# Single test method
mvn test -Dtest=MaybeTest#monadLaw_leftIdentity
```

Tests use **JUnit Jupiter** for unit tests and **jqwik** for property-based tests. The properties prove the standard algebraic laws — functor identity and composition, applicative identity and homomorphism, and all three monad laws — across all three containers.
