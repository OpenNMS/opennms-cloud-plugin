/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.plugins.cloud.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.opennms.plugins.cloud.config.ConfigStore.Key;
import static org.opennms.plugins.cloud.config.ConfigStore.STORE_PREFIX;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.opennms.integration.api.v1.distributed.KeyValueStore;

public class KvConfigStoreTest {

    @Test
    public void shouldFailForNullStore() {
        assertThrows(NullPointerException.class, () -> new KvConfigStore(null));
    }

    @Test
    public void shouldRunCrud() {
        KvConfigStore store = new KvConfigStore(new InMemoryKvConfigStore());
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

    private static class InMemoryKvConfigStore implements KeyValueStore<String> {

        private Map<String, String> store = new HashMap<>();

        @Override
        public long put(String key, String value, String context) {
            assertEquals(STORE_PREFIX, context);
            store.put(key, value);
            return System.currentTimeMillis();
        }

        @Override
        public long put(String key, String value, String context, Integer ttlInSeconds) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public Optional<String> get(String key, String context) {
            assertEquals(STORE_PREFIX, context);
            return Optional.ofNullable(this.store.get(key));
        }

        @Override
        public Optional<String> getIfStale(String key, String context, long timestamp) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public OptionalLong getLastUpdated(String key, String context) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public Map<String, String> enumerateContext(String context) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public void delete(String key, String context) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public void truncateContext(String context) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public CompletableFuture<Long> putAsync(String key, String value, String context) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public CompletableFuture<Long> putAsync(String key, String value, String context, Integer ttlInSeconds) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public CompletableFuture<Optional<String>> getAsync(String key, String context) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public CompletableFuture<Optional<String>> getIfStaleAsync(String key, String context, long timestamp) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public CompletableFuture<OptionalLong> getLastUpdatedAsync(String key, String context) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public CompletableFuture<Map<String, String>> enumerateContextAsync(String context) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public CompletableFuture<Void> deleteAsync(String key, String context) {
            throw new UnsupportedOperationException("implement me");
        }

        @Override
        public CompletableFuture<Void> truncateContextAsync(String context) {
            throw new UnsupportedOperationException("implement me");
        }
    }

}