/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.pack.alpha.server.fsm;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import org.apache.servicecomb.pack.common.EventType;
import org.apache.servicecomb.pack.contract.grpc.GrpcTxEvent;

public class OmegaEventSagaSimulator {
  private String serviceName;
  private String instanceId;

  public List<GrpcTxEvent> sagaSuccessfulEvents(String globalTxId, String localTxId_1,
      String localTxId_2, String localTxId_3) {
    List<GrpcTxEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(sagaStartedEvent(globalTxId));
    sagaEvents.add(txStartedEvent(globalTxId, localTxId_1, globalTxId, "service a".getBytes(), "method a"));
    sagaEvents.add(txEndedEvent(globalTxId, localTxId_1, globalTxId, "service a".getBytes(), "method a"));
    sagaEvents.add(txStartedEvent(globalTxId, localTxId_2, globalTxId, "service b".getBytes(), "method b"));
    sagaEvents.add(txEndedEvent(globalTxId, localTxId_2, globalTxId, "service b".getBytes(), "method b"));
    sagaEvents.add(txStartedEvent(globalTxId, localTxId_3, globalTxId, "service c".getBytes(), "method c"));
    sagaEvents.add(txEndedEvent(globalTxId, localTxId_3, globalTxId, "service c".getBytes(), "method c"));
    sagaEvents.add(sagaEndedEvent(globalTxId));
    return sagaEvents;
  }

  public List<GrpcTxEvent> lastTxAbortedEvents(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
    List<GrpcTxEvent> sagaEvents = new ArrayList<>();
    sagaEvents.add(sagaStartedEvent(globalTxId));
    sagaEvents.add(txStartedEvent(globalTxId, localTxId_1, globalTxId, "service a".getBytes(), "method a"));
    sagaEvents.add(txEndedEvent(globalTxId, localTxId_1, globalTxId, "service a".getBytes(), "method a"));
    sagaEvents.add(txStartedEvent(globalTxId, localTxId_2, globalTxId, "service b".getBytes(), "method b"));
    sagaEvents.add(txEndedEvent(globalTxId, localTxId_2, globalTxId, "service b".getBytes(), "method b"));
    sagaEvents.add(txStartedEvent(globalTxId, localTxId_3, globalTxId, "service c".getBytes(), "method c"));
    sagaEvents.add(txAbortedEvent(globalTxId, localTxId_3, globalTxId, "service c".getBytes(), "method c"));
    sagaEvents.add(sagaAbortedEvent(globalTxId));
    return sagaEvents;
  }

  private GrpcTxEvent sagaStartedEvent(String globalTxId) {
    return eventOf(EventType.SagaStartedEvent, globalTxId, globalTxId,
        null, new byte[0], "", 0, "",
        0);
  }

  private GrpcTxEvent sagaEndedEvent(String globalTxId) {
    return eventOf(EventType.SagaEndedEvent, globalTxId, globalTxId,
        null, new byte[0], "", 0, "",
        0);
  }

  private GrpcTxEvent sagaAbortedEvent(String globalTxId) {
    return eventOf(EventType.SagaAbortedEvent, globalTxId, globalTxId,
        null, new byte[0], "", 0, "",
        0);
  }

  private GrpcTxEvent txStartedEvent(String globalTxId,
      String localTxId, String parentTxId, byte[] payloads, String compensationMethod) {
    return eventOf(EventType.TxStartedEvent, globalTxId, localTxId,
        parentTxId, payloads, compensationMethod, 0, "",
        0);
  }

  private GrpcTxEvent txEndedEvent(String globalTxId,
      String localTxId, String parentTxId, byte[] payloads, String compensationMethod) {
    return eventOf(EventType.TxEndedEvent, globalTxId, localTxId,
        parentTxId, payloads, compensationMethod, 0, "",
        0);
  }

  private GrpcTxEvent txAbortedEvent(String globalTxId,
      String localTxId, String parentTxId, byte[] payloads, String compensationMethod) {
    return eventOf(EventType.TxAbortedEvent, globalTxId, localTxId,
        parentTxId, payloads, compensationMethod, 0, "",
        0);
  }

  public GrpcTxEvent txCompensatedEvent(String globalTxId,
      String localTxId, String parentTxId) {
    return eventOf(EventType.TxCompensatedEvent, globalTxId, localTxId,
        parentTxId,  new byte[0], "", 0, "",
        0);
  }

  private GrpcTxEvent eventOf(EventType eventType,
      String globalTxId,
      String localTxId,
      String parentTxId,
      byte[] payloads,
      String compensationMethod,
      int timeout,
      String retryMethod,
      int retries) {

    return GrpcTxEvent.newBuilder()
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .setTimestamp(System.currentTimeMillis())
        .setGlobalTxId(globalTxId)
        .setLocalTxId(localTxId)
        .setParentTxId(parentTxId == null ? "" : parentTxId)
        .setType(eventType.name())
        .setCompensationMethod(compensationMethod)
        .setTimeout(timeout)
        .setRetryMethod(retryMethod)
        .setRetries(retries)
        .setPayloads(ByteString.copyFrom(payloads))
        .build();
  }


  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String serviceName;
    private String instanceId;

    private Builder() {
    }

    public Builder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public OmegaEventSagaSimulator build() {
      OmegaEventSagaSimulator omegaEventSagaSimulator = new OmegaEventSagaSimulator();
      omegaEventSagaSimulator.serviceName = this.serviceName;
      omegaEventSagaSimulator.instanceId = this.instanceId;
      return omegaEventSagaSimulator;
    }
  }
}
