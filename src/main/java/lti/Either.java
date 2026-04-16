package lti;

import java.util.function.Function;

/**
 * A value that is either an error ({@link Left}) or a result ({@link Right}).
 *
 * <p>Models Haskell's {@code Either}. The monad instance is right-biased: {@code bind}
 * propagates {@code Left} unchanged and only applies the function to a {@code Right} value.
 *
 * <p>Because the error type {@code E} must be fixed at the call site, use
 * {@code Either.<E>monad()} to obtain the monad witness rather than a constant field.
 *
 * @param <E> the error type (left side)
 * @param <A> the success type (right side)
 */
sealed abstract class Either<E, A> implements HKT<Either.Tag<E>, A>, Monad<Either.Tag<E>>
    permits Either.Left, Either.Right {

  /** Phantom tag parameterised by the error type {@code E}. */
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

  // -------------------------------------------------------------------------
  // Monad
  // -------------------------------------------------------------------------

  @Override
  public <B> HKT<Either.Tag<E>, B> pure(B b) { return Either.right(b); }

  @Override
  public <B, C> HKT<Either.Tag<E>, C> bind(HKT<Either.Tag<E>, B> fb, Function<B, HKT<Either.Tag<E>, C>> f) {
    Either<E, B> eb = Either.narrow(fb);
    return eb.isLeft() ? Either.left(eb.getLeft()) : f.apply(eb.getRight());
  }

  // -------------------------------------------------------------------------
  // Factories
  // -------------------------------------------------------------------------

  static <E, A> Either<E, A> left(E e)  { return new Left<>(e); }
  static <E, A> Either<E, A> right(A a) { return new Right<>(a); }

  static <E, A> Either<E, A> narrow(HKT<Either.Tag<E>, A> hkt) { return (Either<E, A>) hkt; }

  static <E> Either<E, Void> monad() { return new Right<>(null); }
}
