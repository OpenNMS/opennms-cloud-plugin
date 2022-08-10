package org.opennms.plugins.cloud.srv.tsaas.grpc.comp;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;

public class ZStdCodecRegisterUtil {

    public static DecompressorRegistry createDecompressorRegistry() {
        return DecompressorRegistry.getDefaultInstance().with(new ZStdGrpcCodec(), true);
    }

    public static CompressorRegistry createCompressorRegistry() {
        final CompressorRegistry  registry = CompressorRegistry.getDefaultInstance();
        registry.register(new ZStdGrpcCodec());
        return registry;
    }

}
