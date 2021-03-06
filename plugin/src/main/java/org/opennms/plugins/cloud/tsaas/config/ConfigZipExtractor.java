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

package org.opennms.plugins.cloud.tsaas.config;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * The Zip file looks like this:
 * cloud-credentials.zip/
 *     cert.crt
 *     key.key
 *     token.txt
 */

public class ConfigZipExtractor {

    private final Path configZipFile;

    public ConfigZipExtractor(Path configZipFile) {
        this.configZipFile = Objects.requireNonNull(configZipFile);
        if (!Files.exists(configZipFile)) {
            throw new IllegalArgumentException("Zip file does not exist: " + configZipFile.toAbsolutePath());
        }
    }

    public String getPrivateKey() throws IOException {
        return extractFile("key.key");
    }

    public String getPublicKey() throws IOException {
        return extractFile("cert.crt");
    }

    public String getJwtToken() throws IOException {
        return extractFile("token.txt");
    }

    public String extractFile(String fileName) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(configZipFile, null)) {
            Path fileToExtract = fileSystem.getPath(fileName);
            byte[] bytes = Files.readAllBytes(fileToExtract);
            return new String(bytes).trim();
        }
    }
}
