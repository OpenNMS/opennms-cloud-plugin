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

package org.opennms.plugins.cloud.grpc;

import static io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import com.google.protobuf.Message;
import com.google.rpc.context.AttributeContext;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;

public class CloudLogServiceTestUtil {

    protected void fillOutLogEntryQueue(int batchSize, GrpcLogEntryQueue grpcLogEntryQueue) {
        IntStream.range(0, batchSize).forEach(el -> {
            try {
                grpcLogEntryQueue.insertElementInQueue(0, ThreadLocalRandom.current().nextInt(1, 100), createMethodDescriptor(), Status.Code.OK);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private MethodDescriptor createMethodDescriptor() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        AttributeContext.Response value = AttributeContext.Response.getDefaultInstance();
        MethodDescriptor.Marshaller marshaller = ProtoUtils.marshaller((Message) value.getClass()
                .getMethod("getDefaultInstance", null).invoke(null, null));

        return MethodDescriptor.newBuilder()
                .setType(CLIENT_STREAMING).setFullMethodName("test-method")
                .setRequestMarshaller(marshaller)
                .setResponseMarshaller(marshaller)
                .build();
    }
}