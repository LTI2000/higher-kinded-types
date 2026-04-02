package lti;

import java.util.List;
import java.util.function.Function;

public class Main {
  public static void main(String[] args) {
      Monad<Maybe.Tag> M = Maybe.MONAD;
      Monad<ListF.Tag> L = ListF.MONAD;

      // --- Maybe: Functor ---
      System.out.println("=== Maybe Functor ===");
      HKT<Maybe.Tag, Integer> just5   = Maybe.just(5);
      HKT<Maybe.Tag, Integer> doubled = M.fmap(just5, x -> x * 2);
      System.out.println(Maybe.narrow(doubled));         // Just(10)

      HKT<Maybe.Tag, Integer> nothing        = Maybe.nothing();
      HKT<Maybe.Tag, Integer> doubledNothing = M.fmap(nothing, x -> x * 2);
      System.out.println(Maybe.narrow(doubledNothing));  // Nothing

      // --- Maybe: Monad (safe division chain) ---
      System.out.println("\n=== Maybe Monad (safe division chain) ===");
      Function<Integer, Function<Integer, HKT<Maybe.Tag, Integer>>> safeDiv =
          denom -> num -> denom == 0 ? Maybe.nothing() : Maybe.just(num / denom);

      // 100 / 5 / 4 = 5
      HKT<Maybe.Tag, Integer> result =
          M.bind(M.bind(M.pure(100), safeDiv.apply(5)), safeDiv.apply(4));
      System.out.println(Maybe.narrow(result));          // Just(5)

      // 100 / 0 / 4 — short-circuits
      HKT<Maybe.Tag, Integer> failed =
          M.bind(M.bind(M.pure(100), safeDiv.apply(0)), safeDiv.apply(4));
      System.out.println(Maybe.narrow(failed));          // Nothing

      // --- Kleisli composition ---
      System.out.println("\n=== Kleisli Composition (Maybe) ===");
      Function<Integer, HKT<Maybe.Tag, Integer>> half  = safeDiv.apply(2);
      Function<Integer, HKT<Maybe.Tag, Integer>> third = safeDiv.apply(3);
      Function<Integer, HKT<Maybe.Tag, Integer>> halfThenThird = Combinators.kleisli(M, half, third);

      System.out.println(Maybe.narrow(halfThenThird.apply(60))); // Just(10)  60/2/3
      System.out.println(Maybe.narrow(Combinators.kleisli(M, safeDiv.apply(0), third).apply(60))); // Nothing

      // --- Either: typed error handling ---
      System.out.println("\n=== Either Monad (typed errors) ===");
      Monad<Either.Tag<String>> E = Either.monad();

      Function<Integer, HKT<Either.Tag<String>, Integer>> parsePositive =
          n -> n > 0 ? Either.right(n) : Either.left("Expected positive, got: " + n);

      Function<Integer, HKT<Either.Tag<String>, Integer>> parseEven =
          n -> n % 2 == 0 ? Either.right(n) : Either.left("Expected even, got: " + n);

      Function<Integer, HKT<Either.Tag<String>, Integer>> parsePosEven =
          Combinators.kleisli(E, parsePositive, parseEven);

      System.out.println(Either.narrow(parsePosEven.apply(4)));   // Right(4)
      System.out.println(Either.narrow(parsePosEven.apply(-2)));  // Left(Expected positive, got: -2)
      System.out.println(Either.narrow(parsePosEven.apply(3)));   // Left(Expected even, got: 3)

      // --- liftA2 (Applicative) ---
      System.out.println("\n=== liftA2 (Maybe Applicative) ===");
      HKT<Maybe.Tag, Integer> a = Maybe.just(3);
      HKT<Maybe.Tag, Integer> b = Maybe.just(7);
      HKT<Maybe.Tag, Integer> sum = Combinators.liftA2(M,
          (Integer x) -> (Integer y) -> x + y, a, b);
      System.out.println(Maybe.narrow(sum));  // Just(10)

      HKT<Maybe.Tag, Integer> sumFail = Combinators.liftA2(M,
          (Integer x) -> (Integer y) -> x + y, a, Maybe.nothing());
      System.out.println(Maybe.narrow(sumFail));  // Nothing

      // --- List: Functor ---
      System.out.println("\n=== List Functor ===");
      HKT<ListF.Tag, Integer> nums    = ListF.of(1, 2, 3, 4);
      HKT<ListF.Tag, Integer> squares = L.fmap(nums, x -> x * x);
      System.out.println(ListF.narrow(squares));  // ListF[1, 4, 9, 16]

      // --- List: Applicative (cartesian product) ---
      System.out.println("\n=== List Applicative (cartesian product) ===");
      HKT<ListF.Tag, Function<Integer, Integer>> fns = ListF.of(
          List.of(x -> x + 10, x -> x * 2)
      );
      HKT<ListF.Tag, Integer> vals    = ListF.of(1, 2, 3);
      HKT<ListF.Tag, Integer> applied = L.splat(fns, vals);
      System.out.println(ListF.narrow(applied));  // ListF[11, 12, 13, 2, 4, 6]

      // --- List: Monad (flatMap / concatMap) ---
      System.out.println("\n=== List Monad (flatMap) ===");
      HKT<ListF.Tag, Integer> mirrored = L.bind(
          ListF.of(1, 2, 3),
          n -> ListF.of(n, -n)
      );
      System.out.println(ListF.narrow(mirrored));  // ListF[1, -1, 2, -2, 3, -3]

      // --- mapM: safe-parse a list (Maybe monad) ---
      System.out.println("\n=== mapM with Maybe (short-circuit on failure) ===");
      HKT<Maybe.Tag, List<Integer>> allGood = Combinators.mapM(M, List.of(2, 4, 6),
          n -> n > 0 ? Maybe.just(n * n) : Maybe.nothing());
      System.out.println(Maybe.narrow(allGood));  // Just([4, 16, 36])

      HKT<Maybe.Tag, List<Integer>> withBad = Combinators.mapM(M, List.of(2, -1, 6),
          n -> n > 0 ? Maybe.just(n * n) : Maybe.nothing());
      System.out.println(Maybe.narrow(withBad));  // Nothing

      // --- mapM: validate a list (Either monad — collect first error) ---
      System.out.println("\n=== mapM with Either (first error wins) ===");
      HKT<Either.Tag<String>, List<Integer>> validated = Combinators.mapM(E, List.of(4, 6, 8), parsePosEven);
      System.out.println(Either.narrow(validated));  // Right([4, 6, 8])

      HKT<Either.Tag<String>, List<Integer>> validatedFail = Combinators.mapM(E, List.of(4, 3, 8), parsePosEven);
      System.out.println(Either.narrow(validatedFail));  // Left(Expected even, got: 3)
  }
}
