package org.opennms.plugins.cloud.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.opennms.plugins.cloud.config.ConfigStore.Key;
import static org.opennms.plugins.cloud.config.ConfigStore.STORE_PREFIX;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;

public class SvcConfigStoreTest {

    @Test
    public void shouldFailForNullStore() {
        assertThrows(NullPointerException.class, () -> new KvConfigStore(null));
    }

    @Test
    public void shouldRunCrud() {
        ScvConfigStore store = new ScvConfigStore(new InMemoryScvConfigStore());
        assertEquals(Optional.empty(), store.get(Key.tokenvalue));
        assertNull(store.getOrNull(Key.tokenvalue));
        store.putProperty(Key.tokenvalue, "value");
        assertEquals("value", store.get(Key.tokenvalue).get());
        assertEquals("value", store.getOrNull(Key.tokenvalue));
        store.putProperty(Key.tokenvalue, "value2");
        assertEquals("value2", store.get(Key.tokenvalue).get());
        assertEquals("value2", store.getOrNull(Key.tokenvalue));

        Map<Key, String> map = new HashMap<>();
        map.put(Key.tokenvalue, "value3");
        map.put(Key.tokenkey, "key1");
        store.putProperties(map);
        assertEquals("value3", store.get(Key.tokenvalue).get());
        assertEquals("value3", store.getOrNull(Key.tokenvalue));
        assertEquals("key1", store.get(Key.tokenkey).get());
        assertEquals("key1", store.getOrNull(Key.tokenkey));
    }

    private static class InMemoryScvConfigStore implements SecureCredentialsVault {

        private Credentials credentials;

        @Override
        public Set<String> getAliases() {
            return Collections.singleton(STORE_PREFIX);
        }
        @Override
        public Credentials getCredentials(String alias) {
            assertEquals(STORE_PREFIX, alias);
            return this.credentials;
        }
        @Override
        public void setCredentials(String alias, Credentials credentials) {
            this.credentials = credentials;
        }
    }

}