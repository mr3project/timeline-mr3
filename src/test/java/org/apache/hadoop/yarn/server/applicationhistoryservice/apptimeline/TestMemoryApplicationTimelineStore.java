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

package org.apache.hadoop.yarn.server.applicationhistoryservice.apptimeline;

import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class TestMemoryApplicationTimelineStore
    extends ApplicationTimelineStoreTestUtils {

  @Before
  public void setup() throws Exception {
    store = new MemoryApplicationTimelineStore();
    store.init(new YarnConfiguration());
    store.start();
    loadTestData();
    loadVerificationData();
  }

  @After
  public void tearDown() throws Exception {
    store.stop();
  }

  public ApplicationTimelineStore getApplicationTimelineStore() {
    return store;
  }

  @Test
  public void testGetSingleEntity() {
    super.testGetSingleEntity();
  }

  @Test
  public void testGetEntities() {
    super.testGetEntities();
  }

  @Test
  public void testGetEntitiesWithPrimaryFilters() {
    super.testGetEntitiesWithPrimaryFilters();
  }

  @Test
  public void testGetEntitiesWithSecondaryFilters() {
    super.testGetEntitiesWithSecondaryFilters();
  }

  @Test
  public void testGetEvents() {
    super.testGetEvents();
  }

}
