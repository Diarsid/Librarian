package diarsid.search.impl.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class CartesianStringComposition {

    public static class HashedByString<T> implements Comparable<HashedByString<T>> {

        private final T hashedObject;
        private final String string;

        public HashedByString(T hashedObject, String string) {
            this.hashedObject = hashedObject;
            this.string = string;
        }

        public T hashedObject() {
            return hashedObject;
        }

        public String string() {
            return string;
        }

        @Override
        public int compareTo(HashedByString<T> other) {
            return this.string.compareTo(other.string);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HashedByString)) return false;
            HashedByString<?> hashedByString = (HashedByString<?>) o;
            return string.equals(hashedByString.string);
        }

        @Override
        public int hashCode() {
            return Objects.hash(string);
        }
    }

    public static <T> List<List<T>> composeFrom(List<T> list, Function<T, String> stringFromT, int depth) {
        List<HashedByString<T>> hashedByStrings = list
                .stream()
                .map(t -> new HashedByString<>(t, stringFromT.apply(t)))
                .collect(toList());

        return composeFrom(hashedByStrings, depth);
    }

    public static <T> List<List<T>> composeFrom(List<HashedByString<T>> hashedByStringList, int depth) {
        Set<Set<HashedByString<T>>> resultSet = new HashSet<>();
        Set<Set<HashedByString<T>>> previousDepthSets = new HashSet<>();
        Set<Set<HashedByString<T>>> newSets = new HashSet<>();

        for ( HashedByString<T> hashedByString : hashedByStringList) {
            HashSet<HashedByString<T>> singleHashedByStringSet = new HashSet<>();
            singleHashedByStringSet.add(hashedByString);
            previousDepthSets.add(singleHashedByStringSet);
        }

        for (int i = 2; i <= depth; i++) {

            for ( HashedByString<T> hashedByString : hashedByStringList) {
                for ( Set<HashedByString<T>> hashedByStringPrevious : previousDepthSets ) {
                    if ( ! hashedByStringPrevious.contains(hashedByString) ) {
                        Set<HashedByString<T>> newSet = new HashSet<>(hashedByStringPrevious);
                        newSet.add(hashedByString);
                        newSets.add(newSet);
                    }
                }
            }
            previousDepthSets.clear();
            resultSet.addAll(newSets);
            previousDepthSets.addAll(newSets);
            newSets.clear();
        }

        return resultSet
                .stream()
                .map(stringedSet -> stringedSet
                        .stream()
                        .sorted()
                        .map(HashedByString::hashedObject)
                        .collect(toList()))
                .collect(toList());
    }

}
