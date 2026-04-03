package lti;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

sealed abstract class ListF<A> implements HKT<ListF.Tag, A> permits ListF.Nil, ListF.Cons {

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
      case Nil<A> _ -> ys;
      case Cons<A> c      -> new Cons<>(c.head, append(c.tail, ys));
    };
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

  // -------------------------------------------------------------------------
  // Monad
  // -------------------------------------------------------------------------

  static final Monad<Tag> MONAD = new Monad<>() {
    @Override
    public <A> HKT<Tag, A> pure(A a) {
      return new Cons<>(a, nil());
    }

    @Override
    public <A, B> HKT<Tag, B> bind(HKT<Tag, A> fa, Function<A, HKT<Tag, B>> f) {
      return flatMap(ListF.narrow(fa), a -> ListF.narrow(f.apply(a)));
    }

    private <A, B> ListF<B> flatMap(ListF<A> xs, Function<A, ListF<B>> f) {
      return switch (xs) {
        case Nil<A> _ -> nil();
        case Cons<A> c      -> append(f.apply(c.head), flatMap(c.tail, f));
      };
    }

    @Override
    public <A, B> HKT<Tag, B> fmap(HKT<Tag, A> fa, Function<A, B> f) {
      return map(ListF.narrow(fa), f);
    }

    private <A, B> ListF<B> map(ListF<A> xs, Function<A, B> f) {
      return switch (xs) {
        case Nil<A> _ -> nil();
        case Cons<A> c      -> new Cons<>(f.apply(c.head), map(c.tail, f));
      };
    }

    @Override
    public <A, B> HKT<Tag, B> splat(HKT<Tag, Function<A, B>> ff, HKT<Tag, A> fa) {
      return flatMap(ListF.narrow(ff), fn -> map(ListF.narrow(fa), fn));
    }
  };
}
