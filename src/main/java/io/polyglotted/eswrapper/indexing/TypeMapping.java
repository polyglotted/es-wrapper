package io.polyglotted.eswrapper.indexing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.polyglotted.pgmodel.search.index.FieldMapping;
import io.polyglotted.pgmodel.search.index.Script;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.copyOf;
import static io.polyglotted.eswrapper.indexing.IndexSerializer.GSON;
import static java.util.Collections.singleton;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(includeFieldNames = false, doNotUseGetters = true)
public final class TypeMapping {
    public final String index;
    public final String type;
    public final String parent;
    public final boolean strict;
    public final boolean enabled;
    public final boolean enableSource;
    public final boolean enableAll;
    public final boolean enableType;
    public final boolean enableTtl;
    public final String allAnalyzer;
    public final ImmutableSet<String> sourceIncludes;
    public final ImmutableSet<FieldMapping> fieldMappings;
    public final ImmutableList<Script> scripts;
    public final ImmutableMap<String, Object> meta;

    @Override
    public boolean equals(Object o) {
        return this == o || (!(o == null || getClass() != o.getClass()) &&
           mappingJson().equals(((TypeMapping) o).mappingJson()));
    }

    @Override
    public int hashCode() {
        return 29 * mappingJson().hashCode();
    }

    public String mappingJson() {
        return GSON.toJson(this);
    }

    public static Builder typeBuilder() {
        return new Builder();
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {
        private String index;
        private String type;
        private String parent;
        private boolean strict = false;
        private boolean enabled = true;
        private boolean enableSource = true;
        private boolean enableAll = true;
        private boolean enableType = true;
        private boolean enableTtl = false;
        private String allAnalyzer = "all_analyzer";
        private final Set<String> sourceIncludes = new TreeSet<>();
        private final Set<FieldMapping> fieldMappings = new TreeSet<>();
        private final List<Script> scripts = new ArrayList<>();
        private final Map<String, Object> metaData = new LinkedHashMap<>();

        public Builder strict() { return strict(true); }

        public Builder fieldMapping(FieldMapping.Builder mapping) {
            return fieldMapping(mapping.build());
        }

        public Builder fieldMapping(FieldMapping mapping) {
            return fieldMapping(singleton(mapping));
        }

        public Builder fieldMapping(Collection<FieldMapping> mappings) {
            fieldMappings.removeAll(mappings);
            fieldMappings.addAll(mappings);
            return this;
        }

        public Builder transform(Script.Builder script) {
            return transform(script.build());
        }

        public Builder transform(Script script) {
            return transform(singleton(script));
        }

        public Builder transform(Collection<Script> scripts) {
            this.scripts.addAll(scripts);
            return this;
        }

        public Builder include(String field) {
            sourceIncludes.add(field);
            return this;
        }

        public Builder metaData(String name, Object value) {
            metaData.put(name, value);
            return this;
        }

        public TypeMapping build() {
            return new TypeMapping(checkNotNull(index, "index required"), checkNotNull(type, "type required"), parent,
               strict, enabled, enableSource, enableAll, enableType, enableTtl, allAnalyzer, copyOf(sourceIncludes),
               copyOf(fieldMappings), ImmutableList.copyOf(scripts), ImmutableMap.copyOf(metaData));
        }
    }
}
