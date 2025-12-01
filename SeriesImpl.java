/**
 * Copyright (c) 2025 Sami Menik, PhD. All rights reserved.
 * 
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * This software is provided "as is," without warranty of any kind.
 */
package cs2725.impl.df;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import cs2725.api.List;
import cs2725.api.Set;
import cs2725.api.df.Series;
import cs2725.api.functional.Accumulator;
import cs2725.api.functional.PairwiseOperator;
import cs2725.api.functional.PrefixOperator;
import cs2725.api.functional.ValueMapper;
import cs2725.impl.ArrayList;
import cs2725.impl.ImmutableList;

/**
 * Implementation of a Series that maintains values and an index.
 * 
 * @param <T> the type of elements in the series
 */
public class SeriesImpl<T> implements Series<T> {

    private final List<T> values;
    private final List<Integer> index;

    /**
     * Constructs a Series with given values and default index.
     * 
     * @param values the values in the Series
     */
    public SeriesImpl(List<T> values) {
        if (values == null) {
            throw new IllegalArgumentException("Values list cannot be null.");
        }
        this.values = ImmutableList.of(values);
        this.index = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            this.index.insertItem(i);
        }
    }

    /**
     * Constructs a Series with explicit index. Note that the index can have more
     * values than the number of values in the values list as long as the index
     * values are within the valid range. On the other hand the number of items in
     * the values can be larger than the number of items in the index as long as the
     * index values are within the valid range for the given values.
     * 
     * @param index  the index mapping to the values
     * @param values the values in the Series
     */
    public SeriesImpl(List<Integer> index, List<T> values) {
        if (values == null || index == null) {
            throw new IllegalArgumentException("Values and index cannot be null.");
        }
        for (int idx : index) {
            if (idx < 0 || idx >= values.size()) {
                throw new IllegalArgumentException("Index values must be within valid range.");
            }
        }
        this.values = ImmutableList.of(values);
        this.index = ImmutableList.of(index);
    }

    @Override
    public int size() {
        return index.size();
    }

    @Override
    public T get(int i) {
        if (i < 0 || i >= size()) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + i);
        }
        return values.getItem(index.getItem(i));
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int pos = 0;

            @Override
            public boolean hasNext() {
                return pos < size();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements in Series.");
                }
                return get(pos++);
            }
        };
    }

    @Override
    public List<Integer> index() {
        return ImmutableList.of(index);
    }

    @Override
    public List<T> values() {
       List<T> projected = new ArrayList<>(index.size());

        // For each position in index, pull the corresponding value
        for (int idx : index) {
            projected.add(vals.get(idx));
        }

        // Return as an ImmutableList<T>
        return ImmutableList.copy(projected);

        // This method returns a new list containing the values of the Series,
        // in the order specified by the index list.
        //
        // For example:
        // If the index is [2, 0, 1] and the values list is [10, 20, 30],
        // the method should return [30, 10, 20] because:
        // index[0] = 2 → values[2] = 30
        // index[1] = 0 → values[0] = 10
        // index[2] = 1 → values[1] = 20
        //
        // Another example:
        // If index = [0, 1, 3] and values = [5, 6, 7, 8], return [5, 6, 8].
        //
        // Return a new ImmutableList<T> containing the indexed values.
    }

    @Override
    public Set<T> unique() {
        Set<T> uniques = new HashSet<>();

        // Only consider values referenced by the index
        for (int idx : index) {
            uniques.add(vals.get(idx));
        }

        return uniques;

        // This method returns a Set containing the unique values in the Series.
        // Only the values referenced by the index should be considered.
        //
        // Example 1:
        // values = [10, 20, 10], index = [2, 0, 1]
        // The referenced values are: [10, 10, 20]
        // The result should be a Set containing [10, 20]
        //
        // Example 2:
        // values = [1, 2, 3, 2], index = [0, 1, 3]
        // The referenced values are: [1, 2, 2]
        // The result should be a Set containing [1, 2]
        //
        // Use a HashSet<T> to collect unique elements.
    }

    @Override
    public Series<T> withIndex(List<Integer> newIndex) {
         if (newIndex == null) {
            throw new IllegalArgumentException("The new index cannot be null.");
        }
        for (int idx : newIndex) {
            if (idx < 0 || idx >= size()) {
                throw new IllegalArgumentException("Index is out of bounds: " + idx);
            }
        }
        return new SeriesImpl<>(newIndex, this.values);

        // Returns a new Series whose index is obtained by selecting positions from
        // the current Series according to newIndex.
        //
        // newIndex contains *positions into the current Series*, not positions into the
        // underlying values list. This means each integer i in newIndex refers to the
        // i-th element of the current index.
        //
        // For example:
        // If current index is [5, 3, 1], and newIndex is [2, 0],
        // then the new Series will have index [1, 5], which corresponds to values[1]
        // and values[5].
        //
        // The result must preserve the original values list.
        //
        // If any value in newIndex is out of bounds for the current Series (i.e., < 0
        // or ≥ size()), the method must throw IllegalArgumentException.
    }

    @Override
    public <R> R reduce(Accumulator<R, ? super T> accumulator, R initialValue) {
        R result = initialValue;
        for (T val : this) {
            result = accumulator.apply(result, val);
        }
        return result;
    }

    @Override
    public <R> Series<R> prefix(PrefixOperator<R, ? super T> prefixOperator, R initialValue) {
        List<R> result = new ArrayList<>(size());
        R previousPrefix = initialValue;
        for (T currentElement : this) {
            R currentPrefix = prefixOperator.apply(previousPrefix, currentElement);
            result.insertItem(currentPrefix);
            previousPrefix = currentPrefix;
        }
        return new SeriesImpl<>(result);
    }

    @Override
    public <U> Series<U> mapValues(ValueMapper<? super T, ? extends U> mapper) {
        List<U> mappedValues = new ArrayList<>(size());
        for (T val : this) { // Note: the iterator follows index order.
            mappedValues.insertItem(mapper.map(val));
        }
        return new SeriesImpl<>(mappedValues);
    }

    @Override
    public Series<T> sortBy(Comparator<? super T> comparator) {
        List<Integer> newIndex = index.copy();
        newIndex.sort((i, j) -> comparator.compare(values.getItem(i), values.getItem(j)));
        return this.withIndex(newIndex);
    }

    @Override
    public Series<T> selectByMask(Series<Boolean> mask) {
        // Todo: Project 2: To be implemented.
        if (mask.size() != size()) {
            throw new IllegalArgumentException("Series sizes must match for combination.");
        }
        List<Integer> newIndices = new ArrayList<>();
        for (int i = 0; i < size(); i++) {
            if (Boolean.TRUE.equals(mask.get(i))) {
                newIndices.insertItem(index.getItem(i));
            }
        }
        return this.withIndex(newIndices);
        // Returns a new Series containing only the elements whose corresponding
        // mask values are true.
        //
        // The mask is a Series of Booleans of the same size as this Series.
        // If mask.get(i) is Boolean.TRUE, then include the i-th element;
        // otherwise skip it.
        //
        // For example:
        // values = [10, 20, 30, 40], index = [1, 3, 0]
        // Series looks like: [20, 40, 10]
        // mask = [true, false, true]
        // Resulting Series should reference values [20, 10], with updated index [1, 0]
        //
        // Do not change the original values list; just build a new index.
    }

    @Override
    public <U, R> Series<R> combineWith(Series<U> other, PairwiseOperator<? super T, ? super U, ? extends R> combiner) {
        if (other.size() != size()) {
            throw new IllegalArgumentException("Series sizes must match for combination.");
        }
        List<R> combined = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            combined.insertItem(combiner.apply(get(i), other.get(i)));
        }
        return new SeriesImpl<>(combined);
    }

    @Override
    public double sum() {
        double sum = 0;
            try {
                for (int i = 0; i < size(); i++) {
                    double val = asDouble(get(i));
                    sum += val;
                }
            } catch (Exception e) {
                throw new UnsupportedOperationException("The series is not numeric");
            }
            
        return sum;
        // Note: This function should only consider values referenced by the index.
        // Note: The series can hold any type of values. Attempt to convert values to
        // double by using Series.asDouble(x) function.
    }

    @Override
    public long count() {
        return (long)this.size();
        // Note: This function should only consider values referenced by the index.
        // Note: The series can hold any type of values. Attempt to convert values to
        // double by using Series.asDouble(x) function.
    }

    @Override
    public double mean() {
        if (size() == 0) {
        throw new IllegalStateException("Cannot compute mean of empty Series.");
        }

        double sum = 0.0;

        // Sum only the values referenced by the index
        for (int idx : index) {
            T value = vals.get(idx);
            sum += Series.asDouble(value);   // convert using provided utility
        }

        return sum / index.size();
        }

    @Override
    public double min() {
        if (size() == 0) {
        throw new IllegalStateException("Cannot compute min of empty Series.");
        }

        // Initialize min with the first referenced value
        double minValue = Series.asDouble(vals.get(index.get(0)));

        // Scan through the remaining referenced values
        for (int i = 1; i < index.size(); i++) {
            double v = Series.asDouble(vals.get(index.get(i)));
            if (v < minValue) {
                minValue = v;
            }
        }

        return minValue;

        // Note: This function should only consider values referenced by the index.
        // Note: The series can hold any type of values. Attempt to convert values to
        // double by using Series.asDouble(x) function.
    }

    @Override
    public double max() {
        if (size() == 0) {
            throw new IllegalStateException("Series is empty. Cannot compute max.");
        }
    
        double maxInSeries = asDouble(get(0)); 
        for (int i = 1; i < size(); i++) {
            maxInSeries = Math.max(maxInSeries, asDouble(get(i))); // Update maxValue by comparing
        }
        return maxInSeries;
    }

    @Override
    public double var() {
        if (size() <= 1) {
            throw new IllegalStateException("Series needs to have 1+ element to compute variance.");
        }
        double mean = mean(); 
        double diffs = 0.0;
    
        for (int i = 0; i < size(); i++) {
            double diff = asDouble(get(i)) - mean; 
            diffs += diff * diff;     
        }
    
        return diffs / (size() - 1); // Divide by (size - 1) for sample variance
    }

    @Override
    public double std() {
    return Math.sqrt(this.var());

        // Note: This function should only consider values referenced by the index.
        // Note: The series can hold any type of values. Attempt to convert values to
        // double by using Series.asDouble(x) function.
    }

    @Override
    public double median() {
         List<Double> sortedList = new ArrayList<>();
            try {
                for (int i = 0; i < size(); i++) {
                    double val = asDouble(get(i));
                    sortedList.insertItem(val);
                }
            } catch (Exception e) {
                throw new UnsupportedOperationException("The series is not numeric");
            }
            
        if (sortedList.size() % 2 != 0) {
            int ind = ((size() + 1) / 2) - 1;
            return sortedList.getItem(ind);
        } else {
            int ind1 = (size() / 2) - 1;
            int ind2 = size() / 2;
            return (sortedList.getItem(ind1) + sortedList.getItem(ind2)) / 2;
        }
        // Note: This function should only consider values referenced by the index.
        // Note: The series can hold any type of values. Attempt to convert values to
        // double by using Series.asDouble(x) function.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Index\tValue\n");
        for (int i = 0; i < size(); i++) {
            sb.append(index.getItem(i)).append("\t").append(values.getItem(index.getItem(i))).append("\n");
        }
        return sb.toString();
    }

}
