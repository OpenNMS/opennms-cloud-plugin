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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.scv.immutables.ImmutableCredentials;

/** ConfigStore backed by SecureCredentialsVault. */
public class ScvConfigStore implements ConfigStore {
    private final SecureCredentialsVault scv;

    public ScvConfigStore(SecureCredentialsVault scv) {
        this.scv = Objects.requireNonNull(scv);
    }

    private Optional<Credentials> getCredentials() {
        return Optional.ofNullable(this.scv.getCredentials(STORE_PREFIX));
    }

    public String getOrNull(Key type) {
        return get(type)
                .orElse(null);
    }

    public Optional<String> get(Key type) {
        return getCredentials()
                .map(c -> c.getAttribute(type.name()));
    }

    public void putProperty(final Key key, String value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        putProperties(Collections.singletonMap(key, value));
    }

    public void putProperties(final Map<Key, String> properties) {
        Objects.requireNonNull(properties);

        // retain old values if present
        Map<String, String> propertiesToSave = new HashMap<>();
        getCredentials()
                .map(Credentials::getAttributes)
                .map(Map::entrySet)
                .stream()
                .flatMap(Set::stream)
                .forEach(e -> propertiesToSave.put(e.getKey(), e.getValue()));
        properties
                .forEach((key, value) -> propertiesToSave.put(key.name(), value));

        // Store modified credentials
        Credentials newCredentials = new ImmutableCredentials("", "", propertiesToSave);
        scv.setCredentials(STORE_PREFIX, newCredentials);
    }
}
