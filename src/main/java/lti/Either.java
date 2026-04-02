package lti;

final class Either<E, A> implements HKT<Either.Tag<E>, A> {
  /**
   * Phantom tag for Either<E, ?>. E is baked into the tag to keep F single-kinded.
   */
  interface Tag<E> {
  }

  private final E left;
  private final A right;
  private final boolean isRight;

  private Either(E left, A right, boolean isRight) {
    this.left = left;
    this.right = right;
    this.isRight = isRight;
  }

  public static <E, A> Either<E, A> right(A a) {
    return new Either<>(null, a, true);
  }

  public static <E, A> Either<E, A> left(E e) {
    return new Either<>(e, null, false);
  }

  public boolean isRight() {
    return isRight;
  }

  public boolean isLeft() {
    return !isRight;
  }

  public A getRight() {
    return right;
  }

  public E getLeft() {
    return left;
  }

  @Override
  public String toString() {
    return isRight ? "Right(" + right + ")" : "Left(" + left + ")";
  }

  @SuppressWarnings("unchecked")
  public static <E, A> Either<E, A> narrow(HKT<Either.Tag<E>, A> HKT) {
    return (Either<E, A>) HKT;
  }
}
