package io.polyglotted.eswrapper.query.request;

import com.google.common.collect.ImmutableList;
import lombok.*;
import lombok.experimental.Accessors;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.io.stream.BytesStreamInput;
import org.elasticsearch.common.io.stream.StreamInput;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.elasticsearch.action.support.IndicesOptions.readIndicesOptions;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class QueryHints {
    public final SearchOptions options;
    public final SearchType type;
    public final int timeout;
    public final ImmutableList<String> routing;
    public final String preference;
    public final boolean fetch;

    @SuppressWarnings("unused")
    public enum SearchOptions {
        IGNORE_UNAVAILABLE((byte) 1), ALLOW_NO_INDICES((byte) 2), EXPAND_WILDCARDS_OPEN((byte) 4),
        EXPAND_WILDCARDS_CLOSED((byte) 8), FORBID_ALIASES_TO_MULTIPLE_INDICES((byte) 16),
        FORBID_CLOSED_INDICES((byte) 32), STRICT_EXPAND_OPEN((byte) 6), LENIENT_EXPAND_OPEN((byte) 7),
        STRICT_EXPAND_OPEN_CLOSED((byte) 14), STRICT_EXPAND_OPEN_FORBID_CLOSED((byte) 38),
        STRICT_SINGLE_INDEX_NO_EXPAND_FORBID_CLOSED((byte) 48);

        private final StreamInput bytes;

        SearchOptions(byte value) {
            this.bytes = new BytesStreamInput(new byte[]{value});
        }

        @SneakyThrows
        public IndicesOptions toOptions() {
            bytes.reset();
            return readIndicesOptions(bytes);
        }
    }

    @SuppressWarnings("unused")
    public enum SearchType {
        DFS_QUERY_THEN_FETCH, DFS_QUERY_AND_FETCH, QUERY_THEN_FETCH, QUERY_AND_FETCH, SCAN, COUNT;

        public String toType() {
            return name().toLowerCase();
        }
    }

    public static Builder hintsBuilder() {
        return new Builder();
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {
        private SearchOptions indicesOptions = SearchOptions.LENIENT_EXPAND_OPEN;
        private SearchType searchType = SearchType.QUERY_THEN_FETCH;
        private int timeoutInSeconds = 10;
        private final List<String> routing = new ArrayList<>();
        private String preference = null;
        private boolean fetchSource = true;

        public Builder routing(String... routing) {
            this.routing.addAll(asList(routing));
            return this;
        }

        public QueryHints build() {
            return new QueryHints(indicesOptions, searchType, timeoutInSeconds,
               ImmutableList.copyOf(routing), preference, fetchSource);
        }
    }
}
