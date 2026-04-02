package lti;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class ListF<A> implements HKT<ListF.Tag, A> {
  interface Tag {
  }

  static final Monad<Tag> MONAD = new Monad<>() {
    @Override
    public <A> HKT<Tag, A> pure(A a) {
      return ListF.of(a);
    }

    @Override
    public <A, B> HKT<Tag, B> bind(HKT<Tag, A> fa, Function<A, HKT<Tag, B>> f) {
      List<A> as = ListF.narrow(fa).values;
      List<B> out = new ArrayList<>();
      for (A a : as) out.addAll(ListF.narrow(f.apply(a)).values);
      return ListF.of(out);
    }

    @Override
    public <A, B> HKT<Tag, B> fmap(HKT<Tag, A> fa, Function<A, B> f) {
      List<A> as = ListF.narrow(fa).values;
      List<B> out = new ArrayList<>(as.size());
      for (A a : as) out.add(f.apply(a));
      return ListF.of(out);
    }

    @Override
    public <A, B> HKT<Tag, B> splat(HKT<Tag, Function<A, B>> ff, HKT<Tag, A> fa) {
      List<Function<A, B>> fns = ListF.narrow(ff).values;
      List<A> as = ListF.narrow(fa).values;
      List<B> out = new ArrayList<>(fns.size() * as.size());
      for (Function<A, B> fn : fns)
        for (A a : as)
          out.add(fn.apply(a));
      return ListF.of(out);
    }
  };

  final List<A> values;

  private ListF(List<A> values) {
    this.values = List.copyOf(values);
  }

  public static <A> ListF<A> of(List<A> values) {
    return new ListF<>(values);
  }

  @SafeVarargs
  public static <A> ListF<A> of(A... values) {
    return new ListF<>(List.of(values));
  }

  public static <A> ListF<A> narrow(HKT<ListF.Tag, A> HKT) {
    return (ListF<A>) HKT;
  }

  @Override
  public String toString() {
    return "ListF" + values;
  }
}
