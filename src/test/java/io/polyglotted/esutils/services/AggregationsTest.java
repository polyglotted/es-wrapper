package io.polyglotted.esutils.services;

import io.polyglotted.esutils.AbstractElasticTest;
import io.polyglotted.esutils.indexing.*;
import io.polyglotted.esutils.query.StandardQuery;
import io.polyglotted.esutils.query.StandardResponse;
import io.polyglotted.esutils.query.request.Aggregates;
import io.polyglotted.esutils.query.request.Expression;
import io.polyglotted.esutils.query.response.Aggregation;
import io.polyglotted.esutils.query.response.Bucket;
import io.polyglotted.esutils.query.response.Flattened;
import io.polyglotted.esutils.query.response.ResultBuilder;
import org.testng.annotations.Test;

import java.util.Iterator;

import static io.polyglotted.esutils.indexing.FieldMapping.notAnalyzedField;
import static io.polyglotted.esutils.indexing.TypeMapping.typeBuilder;
import static io.polyglotted.esutils.query.AggregationType.Avg;
import static io.polyglotted.esutils.query.AggregationType.Count;
import static io.polyglotted.esutils.query.AggregationType.Max;
import static io.polyglotted.esutils.query.AggregationType.Min;
import static io.polyglotted.esutils.query.AggregationType.Sum;
import static io.polyglotted.esutils.query.StandardQuery.queryBuilder;
import static io.polyglotted.esutils.query.request.Aggregates.*;
import static io.polyglotted.esutils.query.request.Expressions.equalsTo;
import static io.polyglotted.esutils.services.Trade.FieldDate;
import static io.polyglotted.esutils.services.Trade.FieldRegion;
import static io.polyglotted.esutils.services.Trade.FieldValue;
import static io.polyglotted.esutils.services.Trade.TRADE_TYPE;
import static io.polyglotted.esutils.services.Trade.tradesRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class AggregationsTest extends AbstractElasticTest {
    private static final String TRADE_AGGREGATES_INDEX = "trade_aggregates_index";

    @Override
    protected void performSetup() {
        admin.dropIndex(TRADE_AGGREGATES_INDEX);
        admin.createIndex(IndexSetting.with(3, 0), TRADE_AGGREGATES_INDEX);
        admin.createType(typeBuilder().index(TRADE_AGGREGATES_INDEX).type(TRADE_TYPE)
           .fieldMapping(notAnalyzedField(FieldDate, FieldType.DATE)).build());
    }

    @Test
    public void getMaxAggregation() throws Exception {
        Aggregation aggs = indexAndAggregate(max(FieldValue, FieldValue));
        assertFalse(aggs.hasBuckets());
        assertThat(aggs.doubleValue(Max.name()), is(equalTo(50.0)));
    }

    @Test
    public void getMinAggregation() throws Exception {
        Aggregation aggs = indexAndAggregate(min(FieldValue, FieldValue));
        assertFalse(aggs.hasBuckets());
        assertThat(aggs.doubleValue(Min.name()), is(equalTo(10.0)));
    }

    @Test
    public void getSumAggregation() throws Exception {
        Aggregation aggs = indexAndAggregate(sum(FieldValue, FieldValue));
        assertFalse(aggs.hasBuckets());
        assertThat(aggs.doubleValue(Sum.name()), is(equalTo(447.0)));
    }

    @Test
    public void getAvgAggregation() throws Exception {
        Aggregation aggs = indexAndAggregate(avg(FieldValue, FieldValue));
        assertFalse(aggs.hasBuckets());
        assertThat(aggs.doubleValue(Avg.name()), is(equalTo(22.35)));
    }

    @Test
    public void getCountAggregation() throws Exception {
        Aggregation aggs = indexAndAggregate(count(FieldValue, FieldValue));
        assertFalse(aggs.hasBuckets());
        assertThat(aggs.longValue(Count.name()), is(equalTo(20L)));
    }

    @Test
    public void testNoAggregation() throws Exception {
        String myVal = "myVal";
        Aggregation aggs = indexAndAggregate(count(myVal, myVal));
        assertThat(aggs.longValue(Count.name()), is(equalTo(0L)));
        assertFalse(aggs.hasBuckets());
    }

    @Test
    public void getStatsAggregations() throws Exception {
        Aggregation aggs = indexAndAggregate(stats(FieldValue, FieldValue));
        assertFalse(aggs.hasBuckets());
        assertThat(aggs.longValue(Count.name()), is(equalTo(20L)));
        assertThat(aggs.doubleValue(Sum.name()), is(equalTo(447.0)));
        assertThat(aggs.doubleValue(Min.name()), is(equalTo(10.0)));
        assertThat(aggs.doubleValue(Max.name()), is(equalTo(50.0)));
        assertThat(aggs.doubleValue(Avg.name()), is(equalTo(22.35)));
    }

    @Test
    public void getTermAggregations() throws Exception {
        Aggregation aggs = indexAndAggregate(term("myaggs", FieldRegion));
        Iterator<Bucket> bucketIter = aggs.buckets().iterator();
        assertBucket(bucketIter.next(), "EMEA", 11L);
        assertBucket(bucketIter.next(), "NA", 5L);
        assertBucket(bucketIter.next(), "SA", 3L);
        assertBucket(bucketIter.next(), "APAC", 1L);
    }

    @Test
    public void getDateHistograms() throws Exception {
        Aggregation aggs = indexAndAggregate(dateHistogram("dates", FieldDate, "month", "yyyy-MMM"));
        Iterator<Bucket> bucketIter = aggs.buckets().iterator();
        assertDateBucket(bucketIter.next(), "2015-Jan", 1420070400000L, 9L);
        assertDateBucket(bucketIter.next(), "2015-Feb", 1422748800000L, 5L);
        assertDateBucket(bucketIter.next(), "2015-Mar", 1425168000000L, 6L);
    }

    @Test
    public void getSimpleGroupBy() throws Exception {
        Aggregates.Builder dateHisto = dateHistogramBuilder("dates", FieldDate, "month");
        dateHisto.addAndGet(termBuilder("regions", FieldRegion));
        Aggregation aggs = indexAndAggregate(dateHisto.build());

        Iterator<Flattened> iterator = Flattened.flatten(aggs).iterator();
        assertEquals(iterator.next(), new Flattened("2015-01-01", "EMEA", 5L));
        assertEquals(iterator.next(), new Flattened("2015-01-01", "SA", 2L));
        assertEquals(iterator.next(), new Flattened("2015-01-01", "APAC", 1L));
        assertEquals(iterator.next(), new Flattened("2015-01-01", "NA", 1L));
        assertEquals(iterator.next(), new Flattened("2015-02-01", "EMEA", 3L));
        assertEquals(iterator.next(), new Flattened("2015-02-01", "NA", 1L));
        assertEquals(iterator.next(), new Flattened("2015-02-01", "SA", 1L));
        assertEquals(iterator.next(), new Flattened("2015-03-01", "EMEA", 3L));
        assertEquals(iterator.next(), new Flattened("2015-03-01", "NA", 3L));
    }

    private Aggregation indexAndAggregate(Expression aggs) {
        indexer.index(tradesRequest(TRADE_AGGREGATES_INDEX, System.currentTimeMillis()));
        return query.aggregate(aggs, TRADE_AGGREGATES_INDEX);
    }

    @Test
    public void getGroupByWithMax() throws Exception {
        assertSingleValueSubAggs(maxBuilder(FieldValue, FieldValue), "EMEA", new Flattened("EMEA", 32.0));
    }

    @Test
    public void getGroupByWithMin() throws Exception {
        assertSingleValueSubAggs(minBuilder(FieldValue, FieldValue), "NA", new Flattened("NA", 10.0));
    }

    @Test
    public void getGroupByWithSum() throws Exception {
        assertSingleValueSubAggs(sumBuilder(FieldValue, FieldValue), "SA", new Flattened("SA", 50.0));
    }

    @Test
    public void getGroupByWithAvg() throws Exception {
        assertSingleValueSubAggs(avgBuilder(FieldValue, FieldValue), "NA", new Flattened("NA", 34.0));
    }

    @Test
    public void getGroupByWithCount() throws Exception {
        assertSingleValueSubAggs(countBuilder(FieldValue, FieldValue), "EMEA", new Flattened("EMEA", 11L));
    }

    private void assertSingleValueSubAggs(Builder child, String filter, Flattened expected) {
        Builder aggBldr = termBuilder("regions", FieldRegion).add(child);
        Aggregation aggs = queryWithAggregations(equalsTo(FieldRegion, filter), aggBldr.build());
        Iterator<Flattened> iterator = Flattened.flatten(aggs).iterator();
        assertEquals(iterator.next(), expected);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void getGroupByWithStats() throws Exception {
        Builder dateHisto = dateHistogramBuilder("dates", FieldDate, "month");
        dateHisto.addAndGet(termBuilder("regions", FieldRegion))
           .addAndGet(statsBuilder(FieldValue, FieldValue));
        Aggregation aggs = queryWithAggregations(equalsTo(FieldRegion, "EMEA"), dateHisto.build());

        Iterator<Flattened> iterator = Flattened.flatten(aggs).iterator();
        assertEquals(iterator.next(), new Flattened("2015-01-01", "EMEA", 15.2, 5L, 20.0, 10.0, 76.0));
        assertEquals(iterator.next(), new Flattened("2015-02-01", "EMEA", 29.0, 3L, 32.0, 25.0, 87.0));
        assertEquals(iterator.next(), new Flattened("2015-03-01", "EMEA", 16.0, 3L, 20.0, 12.0, 48.0));
        assertFalse(iterator.hasNext());
    }

    private Aggregation queryWithAggregations(Expression filter, Expression... aggs) {
        indexer.index(tradesRequest(TRADE_AGGREGATES_INDEX, System.currentTimeMillis()));
        StandardQuery.Builder queryBuilder = queryBuilder().index(TRADE_AGGREGATES_INDEX).type(TRADE_TYPE)
           .size(0).aggregate(aggs);
        if (filter != null) queryBuilder.expression(filter);
        StandardResponse response = query.search(queryBuilder.build(), null, ResultBuilder.EmptyBuilder);
        return response.aggregations.get(0);
    }

    private static void assertDateBucket(Bucket bucket, String key, Long keyValue, long docCount) {
        assertBucket(bucket, key, docCount);
        assertEquals(bucket.keyValue(), keyValue);
    }

    private static void assertBucket(Bucket bucket, String key, long docCount) {
        assertThat(bucket.key, is(equalTo(key)));
        assertThat(bucket.docCount, is(equalTo(docCount)));
    }
}
