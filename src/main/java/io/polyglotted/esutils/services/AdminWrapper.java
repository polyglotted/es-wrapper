package io.polyglotted.esutils.services;

import com.google.common.base.Strings;
import io.polyglotted.esutils.indexing.IndexSetting;
import io.polyglotted.esutils.indexing.TypeMapping;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MappingMetaData;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static io.polyglotted.esutils.indexing.IndexSerializer.GSON;

@RequiredArgsConstructor
public final class AdminWrapper {
    private final Client client;

    public boolean indexExists(String... indices) {
        IndicesAdminClient indicesAdmin = client.admin().indices();
        return indicesAdmin.exists(new IndicesExistsRequest(indices)).actionGet().isExists();
    }

    public void createIndex(IndexSetting setting, String... indices) {
        for (String index : indices) {
            createIndex(index, setting);
        }
    }

    public void createIndex(String index, IndexSetting setting, String... aliases) {
        if (indexExists(index)) return;

        CreateIndexRequest request = new CreateIndexRequest(index).settings(GSON.toJson(setting));
        if (aliases.length > 0)
            request.aliases(GSON.toJson(aliases));

        IndicesAdminClient indicesAdmin = client.admin().indices();
        AcknowledgedResponse response = indicesAdmin.create(request).actionGet();
        checkState(response.isAcknowledged(), "unable to create index for " + index);
    }

    public void updateIndex(String index, IndexSetting setting, String alias) {
        checkState(indexExists(index), "index does not exist " + index);
        IndicesAdminClient indicesAdmin = client.admin().indices();

        if (setting != null) {
            UpdateSettingsRequest settingsRequest = Requests.updateSettingsRequest(index)
                    .settings(GSON.toJson(setting));
            AcknowledgedResponse response = indicesAdmin.updateSettings(settingsRequest).actionGet();
            checkState(response.isAcknowledged(), "unable to update settings for " + index);
        }
        if (!Strings.isNullOrEmpty(alias)) {
            IndicesAliasesRequest aliasesRequest = Requests.indexAliasesRequest().addAlias(alias, index);
            AcknowledgedResponse response = indicesAdmin.aliases(aliasesRequest).actionGet();
            checkState(response.isAcknowledged(), "unable to update settings for " + index);
        }
    }

    public boolean typeExists(String index, String... types) {
        IndicesAdminClient indicesAdmin = client.admin().indices();
        TypesExistsRequest request = new TypesExistsRequest(new String[]{index}, types);
        return indicesAdmin.typesExists(request).actionGet().isExists();
    }

    public void createType(TypeMapping mapping) {
        checkState(indexExists(mapping.index), "create the index before creating type");
        if (typeExists(mapping.index, mapping.type)) return;

        IndicesAdminClient indicesAdmin = client.admin().indices();
        if (!indicesAdmin.putMapping(new PutMappingRequest(mapping.index).type(mapping.type)
                .source(GSON.toJson(mapping))).actionGet().isAcknowledged())
            throw new RuntimeException("could not create type " + mapping.type);

        forceRefresh();
    }

    public String getMapping(String index, String type) {
        ClusterState state = client.admin().cluster().prepareState().setIndices(index).execute()
                .actionGet().getState();
        MappingMetaData mapping = state.getMetaData().index(index).mapping(type);
        return mapping.source().toString();
    }

    public void forceRefresh(String... indices) {
        client.admin().indices().refresh(Requests.refreshRequest(indices)).actionGet();
    }

    public void dropIndex(String... indices) {
        for (String index : indices) {
            if (!client.admin().indices().delete(new DeleteIndexRequest(index).indicesOptions(
                    IndicesOptions.lenientExpandOpen())).actionGet().isAcknowledged())
                throw new RuntimeException("Could not clear the index!");
        }
    }

    public void waitForYellowStatus() {
        ClusterHealthRequestBuilder healthRequest = client
                .admin()
                .cluster()
                .prepareHealth()
                .setWaitForRelocatingShards(0)
                .setWaitForYellowStatus();
        ClusterHealthResponse clusterHealth = healthRequest.execute().actionGet();
        List<String> validationFailures = clusterHealth.getAllValidationFailures();
        checkState(validationFailures.isEmpty(), "cluster has validation errors " + validationFailures);
    }
}
