package lti;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * An immutable singly-linked list as a monad ({@code bind} = {@code concatMap}).
 *
 * <p>The two variants are {@link Nil} (empty) and {@link Cons} (head + tail).
 * {@code fmap} and {@code splat} are overridden with direct recursive implementations
 * that avoid the extra allocation from the default {@code bind}-derived versions.
 *
 * <p>Use {@code ListF.MONAD} as the monad witness; it is the {@code Nil} singleton
 * typed as {@code ListF<?>}.
 *
 * @param <A> the element type
 */
sealed abstract class ListF<A> implements HKT<ListF.Tag, A>, Monad<ListF.Tag>
    permits ListF.Nil, ListF.Cons {

  /** Phantom tag identifying {@code ListF} in the HKT encoding. */
  interface Tag {}

  static final class Nil<A> extends ListF<A> {
    @SuppressWarnings("rawtypes")
    private static final Nil INSTANCE = new Nil();

    private Nil() {}

    @Override public String toString() { return "ListF[]"; }
  }

  static final class Cons<A> extends ListF<A> {
    final A head;
    final ListF<A> tail;

    Cons(A head, ListF<A> tail) {
      this.head = head;
      this.tail = tail;
    }

    @Override public String toString() { return "ListF" + toList(); }
  }

  // -------------------------------------------------------------------------
  // Structure
  // -------------------------------------------------------------------------

  boolean isEmpty() { return this instanceof Nil; }

  List<A> toList() {
    List<A> out = new ArrayList<>();
    ListF<A> cur = this;
    while (cur instanceof Cons<A> c) { out.add(c.head); cur = c.tail; }
    return out;
  }

  private static <A> ListF<A> append(ListF<A> xs, ListF<A> ys) {
    return switch (xs) {
      case Nil<A>  _ -> ys;
      case Cons<A> c -> new Cons<>(c.head, append(c.tail, ys));
    };
  }

  private static <A, B> ListF<B> flatMap(ListF<A> xs, Function<A, ListF<B>> f) {
    return switch (xs) {
      case Nil<A>  _ -> nil();
      case Cons<A> c -> append(f.apply(c.head), flatMap(c.tail, f));
    };
  }

  private static <A, B> ListF<B> map(ListF<A> xs, Function<A, B> f) {
    return switch (xs) {
      case Nil<A>  _ -> nil();
      case Cons<A> c -> new Cons<>(f.apply(c.head), map(c.tail, f));
    };
  }

  // -------------------------------------------------------------------------
  // Monad
  // -------------------------------------------------------------------------

  @Override
  public <B> HKT<Tag, B> pure(B b) { return new Cons<>(b, nil()); }

  @Override
  public <B, C> HKT<Tag, C> bind(HKT<Tag, B> fb, Function<B, HKT<Tag, C>> f) {
    return flatMap(ListF.narrow(fb), b -> ListF.narrow(f.apply(b)));
  }

  @Override
  public <B, C> HKT<Tag, C> fmap(HKT<Tag, B> fb, Function<B, C> f) {
    return map(ListF.narrow(fb), f);
  }

  @Override
  public <B, C> HKT<Tag, C> splat(HKT<Tag, Function<B, C>> ff, HKT<Tag, B> fb) {
    return flatMap(ListF.narrow(ff), fn -> map(ListF.narrow(fb), fn));
  }

  // -------------------------------------------------------------------------
  // Factories
  // -------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  static <A> ListF<A> nil() { return (ListF<A>) Nil.INSTANCE; }

  @SafeVarargs
  static <A> ListF<A> of(A... values) {
    ListF<A> acc = nil();
    for (int i = values.length - 1; i >= 0; i--) acc = new Cons<>(values[i], acc);
    return acc;
  }

  static <A> ListF<A> of(List<A> values) {
    ListF<A> acc = nil();
    for (int i = values.size() - 1; i >= 0; i--) acc = new Cons<>(values.get(i), acc);
    return acc;
  }

  static <A> ListF<A> narrow(HKT<ListF.Tag, A> hkt) { return (ListF<A>) hkt; }

  static final ListF<?> MONAD = Nil.INSTANCE;
}
