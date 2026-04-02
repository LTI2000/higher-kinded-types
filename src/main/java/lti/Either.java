package lti;

import java.util.function.Function;

sealed abstract class Either<E, A> implements HKT<Either.Tag<E>, A> permits Either.Left, Either.Right {

  interface Tag<E> {}

  static final class Left<E, A> extends Either<E, A> {
    final E value;

    Left(E value) { this.value = value; }

    @Override public boolean isRight() { return false; }
    @Override public boolean isLeft()  { return true; }
    @Override public A getRight() { throw new UnsupportedOperationException("Left.getRight"); }
    @Override public E getLeft()  { return value; }
    @Override public String toString() { return "Left(" + value + ")"; }
  }

  static final class Right<E, A> extends Either<E, A> {
    final A value;

    Right(A value) { this.value = value; }

    @Override public boolean isRight() { return true; }
    @Override public boolean isLeft()  { return false; }
    @Override public A getRight() { return value; }
    @Override public E getLeft()  { throw new UnsupportedOperationException("Right.getLeft"); }
    @Override public String toString() { return "Right(" + value + ")"; }
  }

  abstract boolean isRight();
  abstract boolean isLeft();
  abstract A getRight();
  abstract E getLeft();

  static <E, A> Either<E, A> left(E e)  { return new Left<>(e); }
  static <E, A> Either<E, A> right(A a) { return new Right<>(a); }

  static <E, A> Either<E, A> narrow(HKT<Either.Tag<E>, A> hkt) { return (Either<E, A>) hkt; }

  static <E> Monad<Tag<E>> monad() {
    return new Monad<>() {
      @Override
      public <A> HKT<Tag<E>, A> pure(A a) {
        return Either.right(a);
      }

      @Override
      public <A, B> HKT<Tag<E>, B> bind(HKT<Tag<E>, A> fa, Function<A, HKT<Tag<E>, B>> f) {
        Either<E, A> ea = Either.narrow(fa);
        return ea.isLeft() ? Either.left(ea.getLeft()) : f.apply(ea.getRight());
      }
    };
  }
}
