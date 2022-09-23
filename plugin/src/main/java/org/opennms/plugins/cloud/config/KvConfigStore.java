package org.opennms.plugins.cloud.config;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.opennms.integration.api.v1.distributed.KeyValueStore;

/** ConfigStore backed by KeyValueStore. */
public class KvConfigStore implements ConfigStore {

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
        return store.get(type.name(), STORE_PREFIX);
    }

    @Override
    public void putProperty(Key key, String value) {
        store.put(key.name(), value, STORE_PREFIX);
    }

    @Override
    public void putProperties(Map<Key, String> properties) {
        Objects.requireNonNull(properties);
        properties.forEach((key, value) -> store.put(key.name(), value, STORE_PREFIX));
    }
}
