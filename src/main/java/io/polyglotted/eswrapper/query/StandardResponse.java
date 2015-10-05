package io.polyglotted.eswrapper.query;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.polyglotted.eswrapper.query.response.Aggregation;
import io.polyglotted.eswrapper.query.response.ResponseHeader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.polyglotted.eswrapper.query.StandardScroll.fromScrollId;

@RequiredArgsConstructor
public final class StandardResponse {
    public final ResponseHeader header;
    public final ImmutableList<Object> results;
    public final ImmutableList<Aggregation> aggregations;

    public <T> List<T> resultsAs(Class<? extends T> tClass) {
        return Lists.transform(results, tClass::cast);
    }

    public StandardScroll nextScroll() {
        return fromScrollId(checkNotNull(header.scrollId, "cannot scroll null scrollId"));
    }

    public static Builder responseBuilder() {
        return new Builder();
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {
        private ResponseHeader header = null;
        private final List<Object> results = new ArrayList<>();
        private List<Aggregation> aggregations = new ArrayList<>();

        public Builder results(Iterable<?> objects) {
            Iterables.addAll(results, objects);
            return this;
        }

        public StandardResponse build() {
            return new StandardResponse(checkNotNull(header, "header cannot be null"),
               ImmutableList.copyOf(results), ImmutableList.copyOf(aggregations));
        }
    }
}
