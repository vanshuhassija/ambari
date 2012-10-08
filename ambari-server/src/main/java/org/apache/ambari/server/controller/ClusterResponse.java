/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller;

import java.util.Set;

public class ClusterResponse {

  private final Long clusterId;

  private final String clusterName;

  private Set<String> hostNames;

  public ClusterResponse(Long clusterId, String clusterName, Set<String> hostNames) {
    super();
    this.clusterId = clusterId;
    this.clusterName = clusterName;
    this.hostNames = hostNames;
  }

  /**
   * @return the clusterId
   */
  public Long getClusterId() {
    return clusterId;
  }

  /**
   * @return the clusterName
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * @return the host names
   */
  public Set<String> getHostNames() {
    return hostNames;
  }
}
