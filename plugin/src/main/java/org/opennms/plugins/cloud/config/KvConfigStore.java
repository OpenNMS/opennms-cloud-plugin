package org.opennms.plugins.cloud.config;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.json.JSONObject;
import org.opennms.integration.api.v1.distributed.KeyValueStore;

/** ConfigStore backed by KeyValueStore. */
public class KvConfigStore implements ConfigStore {

    // The KeyValue Store uses Json => we need to provide Json Strings to the store.
    final KeyValueStore<String> store;

    public KvConfigStore(final KeyValueStore<String> keyValueStore) {
        this.store = Objects.requireNonNull(keyValueStore);
    }

    @Override
    public String getOrNull(Key key) {
        return get(key).orElse(null);
    }

    @Override
    public Optional<String> get(Key type) {
        return store
                .get(type.name(), STORE_PREFIX)
                .map(this::fromJson);
    }

    @Override
    public void putProperty(Key key, String value) {
        store.put(key.name(), toJson(value), STORE_PREFIX);
    }

    @Override
    public void putProperties(Map<Key, String> properties) {
        Objects.requireNonNull(properties);
        properties.forEach(this::putProperty);
    }

    public String toJson(final String value) {
        JSONObject json = new JSONObject();
        json.put("payload", value);
        return json.toString();
    }

    public String fromJson(final String valueJson) {
        return new JSONObject(valueJson).getString("payload");
    }

}
