package org.opennms.plugins.cloud.tsaas.testserver;

import org.opennms.dataplatform.access.AuthenticateGrpc;
import org.opennms.dataplatform.access.AuthenticateOuterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.BindableService;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;

public class ConfigGrpcImpl extends AuthenticateGrpc.AuthenticateImplBase implements BindableService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigGrpcImpl.class);

    @Override
    public void authenticateKey(AuthenticateOuterClass.AuthenticateKeyRequest request, StreamObserver<AuthenticateOuterClass.AuthenticateKeyResponse> responseObserver) {
        LOG.info("authenticateKey() called.");
        String authKey = request.getAuthenticationKey();
        AuthenticateOuterClass.AuthenticateKeyResponse response = AuthenticateOuterClass.AuthenticateKeyResponse.newBuilder()
//                .setCertificate()
//                .setPrivateKey()
//                .setGrpcEndpoint()
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    public void getServices(AuthenticateOuterClass.GetServicesRequest request, StreamObserver<AuthenticateOuterClass.GetServicesResponse> responseObserver) {
        ServerCalls.asyncUnimplementedUnaryCall(AuthenticateGrpc.getGetServicesMethod(), responseObserver);
    }
}
