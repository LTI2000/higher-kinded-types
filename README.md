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

| Type | Variants | Monad instance |
|---|---|---|
| `Maybe<A>` | `Just<A>`, `Nothing<A>` (singleton) | `Maybe.MONAD` |
| `Either<E, A>` | `Left<E, A>`, `Right<E, A>` | `Either.monad()` |
| `ListF<A>` | wraps `List<A>` | `ListF.MONAD` |

Both `Maybe` and `Either` are proper sealed sum types. `Either.monad()` is a factory rather than a static field because `E` must be fixed at the call site.

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
