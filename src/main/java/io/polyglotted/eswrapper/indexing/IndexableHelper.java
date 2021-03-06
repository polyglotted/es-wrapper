package io.polyglotted.eswrapper.indexing;

import com.google.common.collect.ImmutableMap;
import io.polyglotted.pgmodel.search.DocStatus;
import io.polyglotted.pgmodel.search.IndexKey;
import io.polyglotted.pgmodel.search.SimpleDoc;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Functions.identity;
import static com.google.common.collect.Maps.uniqueIndex;
import static io.polyglotted.eswrapper.indexing.IndexRecord.createRecord;
import static io.polyglotted.eswrapper.indexing.IndexRecord.deleteRecord;
import static io.polyglotted.eswrapper.indexing.IndexRecord.updateRecord;
import static io.polyglotted.eswrapper.indexing.IndexSerializer.GSON;
import static io.polyglotted.pgmodel.search.index.HiddenFields.APPROVAL_ROLES_FIELD;
import static io.polyglotted.pgmodel.search.index.HiddenFields.BASEVERSION_FIELD;

public abstract class IndexableHelper {
    public static Indexable approvalIndexable(Iterable<SimpleDoc> docs, String comment, String user, long timestamp) {
        Indexable.Builder builder = Indexable.indexableBuilder().user(user).timestamp(timestamp);
        for (SimpleDoc doc : docs) {
            IndexKey baseKey = doc.baseKey(doc.baseVersion());

            if (doc.status() == DocStatus.PENDING_DELETE) {
                builder.record(deleteRecord(doc.key, comment, DocStatus.DELETED));
                builder.record(deleteRecord(baseKey));
            } else {
                builder.record(deleteRecord(doc.key, comment, DocStatus.LIVE));
                if (doc.hasItem(BASEVERSION_FIELD)) {
                    builder.record(updateRecord(baseKey, GSON.toJson(doc.filteredCopy(false))));
                } else {
                    builder.record(createRecord(baseKey, GSON.toJson(doc.filteredCopy(false))));
                }
            }
        }
        return builder.build();
    }

    public static Indexable rejectionIndexable(Iterable<SimpleDoc> docs, String comment, String user, long timestamp) {
        Indexable.Builder builder = Indexable.indexableBuilder().user(user).timestamp(timestamp);
        for (SimpleDoc doc : docs)
            builder.record(updateRecord(doc.key, DocStatus.REJECTED, comment, GSON.toJson(doc.filteredCopy(true))));
        return builder.build();
    }

    public static Indexable discardIndexable(Iterable<SimpleDoc> docs, String user, long timestamp) {
        Indexable.Builder builder = Indexable.indexableBuilder().user(user).timestamp(timestamp);
        for (SimpleDoc doc : docs) builder.record(deleteRecord(doc.key, null, DocStatus.DISCARDED));
        return builder.build();
    }

    public static boolean validateApprovalRoles(List<SimpleDoc> docs, List<String> userRoles) {
        outer:
        for (SimpleDoc doc : docs) {
            @SuppressWarnings("unchecked")
            Map<String, String> approvalRoles = doc.hasItem(APPROVAL_ROLES_FIELD) ? uniqueIndex((List<String>)
               doc.source.get(APPROVAL_ROLES_FIELD), identity()) : ImmutableMap.of();
            if (approvalRoles.isEmpty()) continue;

            for (String userRole : userRoles)
                if (approvalRoles.containsKey(userRole)) continue outer;
            return false;
        }
        return true;
    }
}
