/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class SearchParametersTest {

    @Test
    public void forSavingsSetsBirthFilters() {
        final SearchParameters searchParameters = SearchParameters.forSavings("sa.id > 0", "ext-1", 10, 50, "sa.id", "DESC", 12, 8, 1990);

        assertEquals(12, searchParameters.getBirthMonth());
        assertEquals(8, searchParameters.getBirthDay());
        assertEquals(1990, searchParameters.getBirthYear());
        assertEquals("sa.id > 0", searchParameters.getSqlSearch());
        assertEquals("ext-1", searchParameters.getExternalId());
        assertEquals(10, searchParameters.getOffset());
        assertEquals(50, searchParameters.getLimit());
        assertEquals("sa.id", searchParameters.getOrderBy());
        assertEquals("DESC", searchParameters.getSortOrder());
    }

    @Test
    public void forSavingsLeavesOmittedBirthFiltersNull() {
        final SearchParameters searchParameters = SearchParameters.forSavings(null, null, null, null, null, null, 12, null, null);

        assertEquals(12, searchParameters.getBirthMonth());
        assertNull(searchParameters.getBirthDay());
        assertNull(searchParameters.getBirthYear());
    }

    @Test
    public void otherFactoriesHaveNoBirthFilters() {
        final SearchParameters forLoans = SearchParameters.forLoans(null, null, null, null, null, null, null);
        final SearchParameters forClients = SearchParameters.forClients(null, null, null, null, null, null, null, null, null, null, null,
                null, false, false);

        assertNull(forLoans.getBirthMonth());
        assertNull(forLoans.getBirthDay());
        assertNull(forLoans.getBirthYear());
        assertNull(forClients.getBirthMonth());
        assertNull(forClients.getBirthDay());
        assertNull(forClients.getBirthYear());
    }
}
