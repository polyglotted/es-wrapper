package io.polyglotted.eswrapper.services;

import io.polyglotted.eswrapper.AbstractElasticTest;
import io.polyglotted.eswrapper.indexing.Bundling;
import io.polyglotted.eswrapper.indexing.IndexSetting;
import io.polyglotted.eswrapper.indexing.Indexable;
import io.polyglotted.pgmodel.search.IndexKey;
import io.polyglotted.pgmodel.search.SimpleDoc;
import io.polyglotted.pgmodel.search.query.QueryResponse;
import org.testng.annotations.Test;

import java.util.List;

import static io.polyglotted.eswrapper.indexing.Bundling.bundlingBuilder;
import static io.polyglotted.eswrapper.indexing.IndexRecord.createRecord;
import static io.polyglotted.eswrapper.indexing.IndexSerializer.GSON;
import static io.polyglotted.eswrapper.indexing.Indexable.indexableBuilder;
import static io.polyglotted.eswrapper.indexing.TypeMapping.typeBuilder;
import static io.polyglotted.eswrapper.query.ResultBuilder.SimpleDocBuilder;
import static io.polyglotted.eswrapper.query.ResultBuilder.SimpleObjectBuilder;
import static io.polyglotted.eswrapper.services.Portfolio.FieldAddress;
import static io.polyglotted.eswrapper.services.Portfolio.PORTFOLIO_TYPE;
import static io.polyglotted.eswrapper.services.Trade.TRADE_TYPE;
import static io.polyglotted.eswrapper.services.Trade.trade;
import static io.polyglotted.pgmodel.search.IndexKey.keyWithParent;
import static io.polyglotted.pgmodel.search.index.FieldMapping.notAnalyzedStringField;
import static io.polyglotted.pgmodel.search.query.Expressions.equalsTo;
import static io.polyglotted.pgmodel.search.query.Expressions.hasChild;
import static io.polyglotted.pgmodel.search.query.Expressions.hasParent;
import static io.polyglotted.pgmodel.search.query.Expressions.in;
import static io.polyglotted.pgmodel.search.query.StandardQuery.queryBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ParentChildTest extends AbstractElasticTest {
    private static final String PC_INDEX = "parent_child_index";

    @Override
    protected void performSetup() {
        admin.dropIndex(PC_INDEX);
        admin.createIndex(IndexSetting.with(3, 0), PC_INDEX);
        admin.createType(typeBuilder().index(PC_INDEX).type(PORTFOLIO_TYPE)
           .fieldMapping(notAnalyzedStringField(FieldAddress)).build());
        admin.createType(typeBuilder().index(PC_INDEX).type(TRADE_TYPE).parent(PORTFOLIO_TYPE)
           .fieldMapping(notAnalyzedStringField(FieldAddress)).build());
    }

    @Test
    public void writeParentChildRequest() {
        long timestamp = 1425494500000L;
        Portfolio portfolio = new Portfolio("/portfolios/1st", "first portfolio");
        Trade trade = trade("trades:001", "EMEA", "UK", "London", "IEU", "Alex", 1425427200000L, 20.0);
        Indexable indexable = indexableBuilder().timestamp(timestamp).user("unit-tester").records(asList(
           createRecord(PC_INDEX, PORTFOLIO_TYPE, portfolio.address).source(GSON.toJson(portfolio)).build(),
           createRecord(keyWithParent(PC_INDEX, TRADE_TYPE, trade.address, portfolio.address)).source(GSON.toJson(trade)).build()
        )).build();
        List<IndexKey> indexKeys = indexer.twoPhaseCommit(indexable);
        assertThat(query.findAll(indexKeys).size(), is(2));

        ensureHasParent(portfolio, trade);
        ensureHasChild(portfolio, trade);
        ensureBothDocs(2, portfolio.address, trade.address);
    }

    @Test
    public void childWithoutParentTest() {
        long timestamp = 1425495500000L;
        Trade trade = trade("trades:001", "EMEA", "UK", "London", "IEU", "Alex", 1425427200000L, 20.0);
        Bundling bundling = bundlingBuilder().timestamp(timestamp).records(singleton(
           createRecord(keyWithParent(PC_INDEX, TRADE_TYPE, "portfolios/2"))
              .source(GSON.toJson(trade)).build())).build();
        indexer.bulkIndex(bundling);

        ensureBothDocs(1, trade.address);
    }

    private void ensureHasParent(Portfolio portfolio, Trade trade) {
        QueryResponse response = query.search(queryBuilder().index(PC_INDEX)
              .expression(hasParent(PORTFOLIO_TYPE, equalsTo(FieldAddress, portfolio.address))).build(),
           null, SimpleObjectBuilder(GSON, Trade.class));
        List<Trade> simpleDocs = response.resultsAs(Trade.class);
        assertThat(simpleDocs.size(), is(1));
        assertThat(simpleDocs.get(0), is(trade));
    }

    private void ensureHasChild(Portfolio portfolio, Trade trade) {
        QueryResponse response = query.search(queryBuilder().index(PC_INDEX)
              .expression(hasChild(TRADE_TYPE, equalsTo(FieldAddress, trade.address))).build(),
           null, SimpleObjectBuilder(GSON, Portfolio.class));
        List<Portfolio> simpleDocs = response.resultsAs(Portfolio.class);
        assertThat(simpleDocs.size(), is(1));
        assertThat(simpleDocs.get(0), is(portfolio));
    }

    private void ensureBothDocs(int expected, String... addresses) {
        QueryResponse response = query.search(queryBuilder().index(PC_INDEX)
              .expression(in(FieldAddress, addresses)).build(),
           null, SimpleDocBuilder);
        List<SimpleDoc> simpleDocs = response.resultsAs(SimpleDoc.class);
        assertThat(simpleDocs.size(), is(expected));
    }
}
