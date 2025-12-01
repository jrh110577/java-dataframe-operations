/**
 * Copyright (c) 2025 Sami Menik, PhD. All rights reserved.
 * 
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * This software is provided "as is," without warranty of any kind.
 */
package cs2725.impl.df;

import cs2725.api.List;
import cs2725.api.Map;
import cs2725.api.df.Series;
import cs2725.api.df.SeriesGroupBy;
import cs2725.api.functional.AggregateFunction;
import cs2725.impl.ArrayList;
import cs2725.impl.HashMap;
import cs2725.impl.ImmutableList;

/**
 * Implementation of SeriesGroupBy.
 *
 * @param <T> the type of elements in the original Series
 */
public class SeriesGroupByImpl<T> implements SeriesGroupBy<T> {

    /*
     * The original Series that is being grouped.
     */
    private final Series<T> series;

    /*
     * A map from unique group values to lists of indices. Each key is a unique
     * group value, and the associated list contains the position indices of items
     * belonging to that group.
     */
    private final Map<T, List<Integer>> groups;

    /*
     * A list of unique values from the group-by operation. This maintains the group
     * order of the groups.
     */
    private final List<T> index;

    /**
     * Constructs a grouped Series.
     *
     * @param series the original Series
     * @throws IllegalArgumentException if series is null
     */
    public SeriesGroupByImpl(Series<T> series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null.");
        }

        // The 'series' is the original ungrouped Series.
        this.series = series;

        // 'groups' maps each unique value in the Series to the list of index positions
        // where that value appears. For example, if series = [a, b, a], then:
        // groups = { a → [0, 2], b → [1] }
        this.groups = groupByValues();

        // 'index' holds the list of unique values in the Series (group labels),
        // and defines the group ordering in aggregate output.
        this.index = buildIndex();
    }

    /**
     * Groups elements by their values, creating a map from unique values to index
     * positions.
     *
     * @return A map where each unique value from the series is a key, and the
     *         associated value is a list of indices indicating the positions in the
     *         series where this unique value appears.
     */
    private Map<T, List<Integer>> groupByValues() {
        Map<T, List<Integer>> groupMap = new HashMap<>();

        for (int i = 0; i < series.size(); i++) {
            T value = series.get(i);  // Get the value at index i from the series
            if (!groupMap.containsKey(value)) {
                groupMap.put(value, new ArrayList<>());
            }
            groupMap.get(value).insertItem(i); 
        }

        return groupMap;

        // Create a map where each key is a unique value from the Series.
        // The value for each key is a list of integer positions (indices)
        // indicating where that value occurs in the Series.

        // Example:
        // If the Series is ["cat", "dog", "cat", "bird"], then:
        // - "cat" appears at indices 0 and 2 → "cat" → [0, 2]
        // - "dog" appears at index 1 → "dog" → [1]
        // - "bird" appears at index 3 → "bird" → [3]
        //
        // Resulting map:
        // {
        // "cat" -> [0, 2],
        // "dog" -> [1],
        // "bird" -> [3]
        // }
    }

    /**
     * Builds the index of unique group values.
     *
     * @return a list of unique values from the group-by operation
     */
    private List<T> buildIndex() {
        List<T> uniqueValues = new ArrayList<>(groups.size());
        for (T key : groups.keySet()) {
            uniqueValues.insertItem(key);
        }
        return uniqueValues;
    }

    @Override
    public Series<T> series() {
        return series;
    }

    @Override
    public Map<T, List<Integer>> groups() {
        // Return a copy to ensure immutability.
        // Note: Map.copy does not copy the values.
        Map<T, List<Integer>> copy = new HashMap<>(groups.size(), 0.75);
        for (T key : groups.keySet()) {
            List<Integer> indicesImmutable = ImmutableList.of(groups.get(key));
            copy.put(key, indicesImmutable);
        }
        return copy;
    }

    @Override
    public List<T> index() {
        List<T> indexImmutable = ImmutableList.of(index);
        return indexImmutable;
    }

    @Override
    public <R> Series<R> aggregate(AggregateFunction<T, R> aggregator) {
         if (aggregator == null) {
            throw new IllegalArgumentException("Aggregator function cannot be null.");
        }
    
        List<R> aggregatedValues = new ArrayList<>(index.size());
    
        //  the list of index positions for that key from the groups map.
        for (T groupKey : index) {
            List<Integer> indices = groups.get(groupKey);
    
            //construct a sub-Series from the original Series.
            List<T> groupElements = new ArrayList<>(indices.size());
            for (int ind : indices) {
                groupElements.insertItem(series.get(ind));
            }
    
            
            Series<T> subSeries = new SeriesImpl<>(groupElements);
    
            // Apply the aggregator function to obtain the result
            R aggregatedResult = aggregator.apply(subSeries);
            aggregatedValues.insertItem(aggregatedResult);

        }
        return new SeriesImpl<>(aggregatedValues); 

        // Applies the given aggregate function to each group in the grouped Series.
        //
        // For each group key in the index list:
        // - Retrieve the list of index positions for that key from the groups map.
        // - Use those indices to construct a sub-Series from the original Series.
        // - Apply the aggregator to the sub-Series.
        // - Store the result in the order of the index list.
        //
        // The result is a new Series of the same length as the number of groups.
        // Its values are the results of the aggregator, and it uses a default index.
        //
        // Example (using a count aggregator):
        // Original Series: ["apple", "banana", "apple", "banana", "apple"]
        // Grouping:
        // "apple" → [0, 2, 4] → Series(["apple", "apple", "apple"]) → count = 3
        // "banana" → [1, 3] → Series(["banana", "banana"]) → count = 2
        // Aggregated result: Series([3, 2])
        //
        // If the aggregator is null, throw IllegalArgumentException.
    }
    public static void main(String[] args) {
        List<String> values = List.of("apple", "banana", "apple", "banana", "apple", "orange");
        Series<String> series = new SeriesImpl<>(values);

        // Creating a grouped Series
        SeriesGroupBy<String> groupBy = new SeriesGroupByImpl<>(series);

        // Display group mappings
        System.out.println("Groups: " + groupBy.groups()); 
        // Expected Output: { "apple" -> [0, 2, 4], "banana" -> [1, 3], "orange" -> [5] }

        // Implementing a count aggregator function
        AggregateFunction<String, Integer> countAggregator = subSeries -> subSeries.size();

        // Applying aggregation
        Series<Integer> aggregatedSeries = groupBy.aggregate(countAggregator);

        // Printing aggregated results
        System.out.println("Aggregated counts: " + aggregatedSeries.values()); 
        // Expected Output: Series([3, 2, 1])

        // Example 2: Grouping and Aggregating a Series of Numbers (Sum Aggregation)
        List<Integer> numbers = List.of(5, 10, 5, 20, 10, 5, 20);
        Series<Integer> numberSeries = new SeriesImpl<>(numbers);

        SeriesGroupBy<Integer> numberGroupBy = new SeriesGroupByImpl<>(numberSeries);

        // Display group mappings
        System.out.println("Number Groups: " + numberGroupBy.groups());
        // Expected Output: { 5 -> [0, 2, 5], 10 -> [1, 4], 20 -> [3, 6] }

        // Sum aggregator function
        AggregateFunction<Integer, Integer> sumAggregator = subSeries -> {
            int sum = 0;
            for (int num : subSeries) {
                sum += num;
            }
            return sum;
        };

        // Applying aggregation
        Series<Integer> sumAggregatedSeries = numberGroupBy.aggregate(sumAggregator);

        // Printing aggregated results
        System.out.println("Aggregated sums: " + sumAggregatedSeries.values());
        // Expected Output: Series([15, 20, 40])

    }
}
