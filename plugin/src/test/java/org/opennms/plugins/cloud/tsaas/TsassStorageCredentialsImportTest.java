package org.opennms.plugins.cloud.tsaas;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.SCV_ALIAS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.plugins.cloud.tsaas.config.InMemoryScv;

public class TsassStorageCredentialsImportTest {

    private final static String OPENNMS_HOME = "opennms.home";

    private Path openNmsHome;
    private String originalOpenNmsHome;

    @Before
    public void setUp() {
        this.originalOpenNmsHome = System.getProperty("opennms.home");
    }

    @Test
    public void shouldImportCloudCredentialsIfPresent() throws Exception {

        InMemoryScv scv = new InMemoryScv();
        TsaasStorage storage = new TsaasStorage(TsaasConfig.builder().build(), scv);
        openNmsHome = Files.createTempDirectory(this.getClass().getSimpleName());
        System.setProperty(OPENNMS_HOME, openNmsHome.toString());
        Files.createDirectory(Path.of(openNmsHome.toString(), "etc"));
        Path credentialsFile = Path.of(openNmsHome.toString(), "etc", "cloud-credentials.zip");
        Files.copy(Path.of("src/test/resources/cert/cloud-credentials.zip"), credentialsFile);
        assertTrue(Files.exists(credentialsFile));
        storage.importCloudCredentialsIfPresent(scv);
        Credentials credentials = scv.getCredentials(SCV_ALIAS);
        assertNotNull(credentials);
        String value = credentials.getAttribute(SecureCredentialsVaultUtil.Type.publickey.name());
        assertNotNull(value);
        assertTrue(value.startsWith("-----BEGIN CERTIFICATE-----"));

        // Check if file has been deleted. we expect that the file is deleted after a successful import:
        assertFalse(Files.exists(credentialsFile));
    }

    @After
    public void tearDown() throws IOException {
        if(this.openNmsHome != null) {
            Files.walk(this.openNmsHome)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        if (originalOpenNmsHome != null) {
            System.setProperty(OPENNMS_HOME, originalOpenNmsHome);
        }
    }
}
