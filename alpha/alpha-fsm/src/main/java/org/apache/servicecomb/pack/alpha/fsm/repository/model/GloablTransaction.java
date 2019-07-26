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

package org.apache.servicecomb.pack.alpha.fsm.repository.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.servicecomb.pack.alpha.fsm.SagaActorState;
import org.apache.servicecomb.pack.alpha.fsm.TransactionType;
import org.apache.servicecomb.pack.alpha.fsm.event.base.BaseEvent;

public class GloablTransaction {
  private String globalTxId;
  private TransactionType type;
  private String serviceName;
  private String instanceId;
  private Date beginTime;
  private Date endTime;
  private SagaActorState state;
  private Integer subTxSize;
  private Long durationTime;
  private List<SagaSubTransaction> subTransactions = new ArrayList<>();
  private List<Map<String,Object>> events = new LinkedList<>();

  public String getGlobalTxId() {
    return globalTxId;
  }

  public TransactionType getType() {
    return type;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public Date getBeginTime() {
    return beginTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public SagaActorState getState() {
    return state;
  }

  public Integer getSubTxSize() {
    return subTxSize;
  }

  public Long getDurationTime() {
    return durationTime;
  }

  public List<SagaSubTransaction> getSubTransactions() {
    return subTransactions;
  }

  public List<Map<String,Object>> getEvents() {
    return events;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String globalTxId;
    private TransactionType type;
    private String serviceName;
    private String instanceId;
    private Date beginTime;
    private Date endTime;
    private SagaActorState state;
    private Integer subTxSize;
    private List<SagaSubTransaction> subTransactions;
    private List<BaseEvent> events;

    private Builder() {
    }

    public Builder globalTxId(String globalTxId) {
      this.globalTxId = globalTxId;
      return this;
    }

    public Builder type(TransactionType type) {
      this.type = type;
      return this;
    }

    public Builder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder beginTime(Date beginTime) {
      this.beginTime = beginTime;
      return this;
    }

    public Builder endTime(Date endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder state(SagaActorState state) {
      this.state = state;
      return this;
    }

    public Builder subTxSize(Integer subTxSize) {
      this.subTxSize = subTxSize;
      return this;
    }

    public Builder subTransactions(List<SagaSubTransaction> subTransactions) {
      this.subTransactions = subTransactions;
      return this;
    }

    public Builder events(List<BaseEvent> events) {
      this.events = events;
      return this;
    }

    public GloablTransaction build() {
      GloablTransaction gloablTransaction = new GloablTransaction();
      gloablTransaction.instanceId = this.instanceId;
      gloablTransaction.state = this.state;
      gloablTransaction.type = this.type;
      gloablTransaction.serviceName = this.serviceName;
      gloablTransaction.beginTime = this.beginTime;
      gloablTransaction.endTime = this.endTime;
      gloablTransaction.globalTxId = this.globalTxId;
      gloablTransaction.subTxSize = this.subTxSize;
      gloablTransaction.durationTime = this.endTime.getTime() - this.beginTime.getTime();
      gloablTransaction.subTransactions = this.subTransactions;
      for(BaseEvent event : events){
        try {
          gloablTransaction.events.add(event.toMap());
        } catch (Exception e) {
          new RuntimeException(e.getMessage(),e);
        }
      }
      return gloablTransaction;
    }
  }
}
