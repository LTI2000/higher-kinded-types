package lti;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class ListMonad implements Monad<ListF.Tag> {

  static final ListMonad INSTANCE = new ListMonad();

  private ListMonad() {
  }

  @Override
  public <A> HKT<ListF.Tag, A> pure(A a) {
    return ListF.of(a);
  }

  @Override
  public <A, B> HKT<ListF.Tag, B> bind(
          HKT<ListF.Tag, A> fa,
          Function<A, HKT<ListF.Tag, B>> f) {
    List<A> as = ListF.narrow(fa).values;
    List<B> out = new ArrayList<>();
    for (A a : as) out.addAll(ListF.narrow(f.apply(a)).values);
    return ListF.of(out);
  }

  @Override
  public <A, B> HKT<ListF.Tag, B> fmap(HKT<ListF.Tag, A> fa, Function<A, B> f) {
    List<A> as = ListF.narrow(fa).values;
    List<B> out = new ArrayList<>(as.size());
    for (A a : as) out.add(f.apply(a));
    return ListF.of(out);
  }

  @Override
  public <A, B> HKT<ListF.Tag, B> splat(HKT<ListF.Tag, Function<A, B>> ff, HKT<ListF.Tag, A> fa) {
    List<Function<A, B>> fns = ListF.narrow(ff).values;
    List<A> as = ListF.narrow(fa).values;
    List<B> out = new ArrayList<>(fns.size() * as.size());
    for (Function<A, B> fn : fns)
      for (A a : as)
        out.add(fn.apply(a));
    return ListF.of(out);
  }
}
