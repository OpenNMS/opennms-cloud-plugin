package org.opennms.plugins.cloud.srv.tsaas.grpc.comp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

import io.grpc.Codec;

public class ZStdGrpcCodec implements Codec {

    public static final String ZSTD = "zstd";

    @Override
    public String getMessageEncoding() {
        return ZSTD;
    }

    @Override
    public InputStream decompress(final InputStream inputStream) throws IOException {
        return new ZstdInputStream(inputStream);
    }

    @Override
    public OutputStream compress(final OutputStream outputStream) throws IOException {
        return new ZstdOutputStream(outputStream);
    }
}
