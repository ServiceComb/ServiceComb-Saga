/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.alpha.tcc.server;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.servicecomb.saga.alpha.server.AlphaApplication;
import org.apache.servicecomb.saga.alpha.server.tcc.GrpcOmegaTccCallback;
import org.apache.servicecomb.saga.alpha.server.tcc.OmegaCallbacksRegistry;
import org.apache.servicecomb.saga.alpha.server.tcc.TransactionEventRegistry;
import org.apache.servicecomb.saga.alpha.server.tcc.event.ParticipatedEvent;
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccParticipatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionEndedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionStartedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc.TccEventServiceBlockingStub;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc.TccEventServiceStub;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AlphaApplication.class},
    properties = {
        "alpha.server.host=0.0.0.0",
        "alpha.server.port=8090"
    })
public class AlphaTccServerTest {

  private static final int port = 8090;
  protected static ManagedChannel clientChannel;

  private final TccEventServiceStub asyncStub = TccEventServiceGrpc.newStub(clientChannel);

  private final TccEventServiceBlockingStub blockingStub = TccEventServiceGrpc.newBlockingStub(clientChannel);

  private final Queue<GrpcTccCoordinateCommand> receivedCommands = new ConcurrentLinkedQueue<>();

  private final TccCoordinateCommandStreamObserver commandStreamObserver =
      new TccCoordinateCommandStreamObserver(this::onCompensation, receivedCommands);

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String confirmMethod = "confirm";
  private final String cancelMethod = "cancel";


  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  private final GrpcServiceConfig serviceConfig = GrpcServiceConfig.newBuilder()
      .setServiceName(serviceName)
      .setInstanceId(instanceId)
      .build();

  @BeforeClass
  public static void setupClientChannel() {
    clientChannel = NettyChannelBuilder.forAddress("localhost", port).usePlaintext().build();
  }

  @AfterClass
  public static void tearDown() {
    clientChannel.shutdown();
    clientChannel = null;
  }

  @Before
  public void before() {
    System.out.println(" globalTxId " + globalTxId);
  }

  @After
  public void after() {
    blockingStub.onDisconnected(serviceConfig);
  }

  @Test
  public void assertOnConnect() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();
    assertThat(
        OmegaCallbacksRegistry.retrieve(serviceName, instanceId), is(instanceOf(GrpcOmegaTccCallback.class))
    );
  }

  private void awaitUntilConnected() {
    await().atMost(2, SECONDS).until(() -> null != (OmegaCallbacksRegistry.getRegistry().get(serviceName)));
  }

  @Test
  public void assertOnParticipated() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();
    blockingStub.participate(newParticipatedEvent("Succeed"));
    assertThat(TransactionEventRegistry.retrieve(globalTxId).size(),  is(1));
    ParticipatedEvent event = TransactionEventRegistry.retrieve(globalTxId).iterator().next();
    assertThat(event.getGlobalTxId(), is(globalTxId));
    assertThat(event.getLocalTxId(), is(localTxId));
    assertThat(event.getInstanceId(), is(instanceId));
    assertThat(event.getServiceName(), is(serviceName));
    assertThat(event.getConfirmMethod(), is(confirmMethod));
    assertThat(event.getCancelMethod(), is(cancelMethod));
    assertThat(event.getStatus(), is(TransactionStatus.Succeed));
  }

  @Test
  public void assertOnTccTransactionSucceedEnded() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();
    blockingStub.onTccTransactionStarted(newTxStart());
    blockingStub.participate(newParticipatedEvent("Succeed"));
    blockingStub.onTccTransactionEnded(newTxEnd("Succeed"));

    await().atMost(2, SECONDS).until(() -> !receivedCommands.isEmpty());
    assertThat(receivedCommands.size(), is(1));
    GrpcTccCoordinateCommand command = receivedCommands.poll();
    assertThat(command.getMethod(), is("confirm"));
    assertThat(command.getGlobalTxId(), is(globalTxId));
    assertThat(command.getServiceName(), is(serviceName));
  }

  @Test
  public void assertOnTccTransactionFailedEnded() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();
    blockingStub.onTccTransactionStarted(newTxStart());
    blockingStub.participate(newParticipatedEvent("Succeed"));
    blockingStub.onTccTransactionEnded(newTxEnd("Failed"));

    await().atMost(2, SECONDS).until(() -> !receivedCommands.isEmpty());
    assertThat(receivedCommands.size(), is(1));
    GrpcTccCoordinateCommand command = receivedCommands.poll();
    assertThat(command.getMethod(), is("cancel"));
    assertThat(command.getGlobalTxId(), is(globalTxId));
    assertThat(command.getServiceName(), is(serviceName));
  }

  private GrpcTccParticipatedEvent newParticipatedEvent(String status) {
    return GrpcTccParticipatedEvent.newBuilder()
        .setGlobalTxId(globalTxId)
        .setLocalTxId(localTxId)
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .setCancelMethod(cancelMethod)
        .setConfirmMethod(confirmMethod)
        .setStatus(status)
        .build();
  }

  private GrpcTccTransactionStartedEvent newTxStart() {
    return GrpcTccTransactionStartedEvent.newBuilder()
        .setGlobalTxId(globalTxId)
        .setLocalTxId(localTxId)
        .build();
  }

  private GrpcTccTransactionEndedEvent newTxEnd(String status) {
    return GrpcTccTransactionEndedEvent.newBuilder()
        .setGlobalTxId(globalTxId)
        .setLocalTxId(localTxId)
        .setStatus(status)
        .build();
  }

  private GrpcAck onCompensation(GrpcTccCoordinateCommand command) {
    return GrpcAck.newBuilder().setAborted(false).build();
  }

}
