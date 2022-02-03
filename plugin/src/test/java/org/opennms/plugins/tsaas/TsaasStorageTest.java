package org.opennms.plugins.tsaas;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.StorageException;

public class TsaasStorageTest {
    @Test
    @Ignore
    public void testClient() throws StorageException {
        TsaasStorage client = new TsaasStorage("grpc-server.7760e3a2553b4cc7ac31.eastus.aksapp.io", 443, "my-client");
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            client.store(samples);
        }
    }
}
