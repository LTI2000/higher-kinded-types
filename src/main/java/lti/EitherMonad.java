package lti;

import java.util.function.Function;

final class EitherMonad<E> implements Monad<Either.Tag<E>> {
  @Override
  public <A> HKT<Either.Tag<E>, A> pure(A a) {
    return Either.right(a);
  }

  @Override
  public <A, B> HKT<Either.Tag<E>, B> bind(
          HKT<Either.Tag<E>, A> fa,
          Function<A, HKT<Either.Tag<E>, B>> f) {
    Either<E, A> ea = Either.narrow(fa);
    return ea.isLeft() ? Either.left(ea.getLeft()) : f.apply(ea.getRight());
  }
}
