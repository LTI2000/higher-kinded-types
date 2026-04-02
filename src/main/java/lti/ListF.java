package lti;

import java.util.List;

final class ListF<A> implements HKT<ListF.Tag, A> {
  interface Tag {
  }

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
