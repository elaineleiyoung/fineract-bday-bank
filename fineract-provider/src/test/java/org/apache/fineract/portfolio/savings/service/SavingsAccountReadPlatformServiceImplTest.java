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
package org.apache.fineract.portfolio.savings.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.useradministration.domain.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
public class SavingsAccountReadPlatformServiceImplTest {

    private static final String HIERARCHY = ".";

    @Mock
    private PlatformSecurityContext context;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PaginationHelper paginationHelper;
    @Mock
    private DatabaseSpecificSQLGenerator sqlGenerator;
    @Mock
    private AppUser appUser;
    @Mock
    private Office office;
    @InjectMocks
    private SavingsAccountReadPlatformServiceImpl underTest;

    @BeforeEach
    public void setUp() {
        when(context.authenticatedUser()).thenReturn(appUser);
        when(appUser.getOffice()).thenReturn(office);
        when(office.getHierarchy()).thenReturn(HIERARCHY);
        when(sqlGenerator.calcFoundRows()).thenReturn("");
    }

    @Test
    public void retrieveAllWithoutBirthFiltersDoesNotFilterOnDateOfBirth() {
        retrieveAll(null, null, null);

        final QueryCapture query = captureQuery();

        assertFalse(query.getSql().contains("date_of_birth"));
        assertArrayEquals(new Object[] { HIERARCHY + "%" }, query.getArgs());
    }

    @Test
    public void retrieveAllWithMonthAndDayFiltersOnBoth() {
        retrieveAll(12, 8, null);

        final QueryCapture query = captureQuery();

        assertTrue(query.getSql().contains(" and extract(month from c.date_of_birth) = ? and extract(day from c.date_of_birth) = ?"));
        assertFalse(query.getSql().contains("extract(year"));
        assertArrayEquals(new Object[] { HIERARCHY + "%", 12, 8 }, query.getArgs());
    }

    @Test
    public void retrieveAllWithAllBirthFiltersAndExternalIdBindsValuesInOrder() {
        final SearchParameters searchParameters = SearchParameters.forSavings(null, "ext-1", null, null, null, null, 12, 8, 1990);

        underTest.retrieveAll(searchParameters);

        final QueryCapture query = captureQuery();

        assertTrue(query.getSql().contains(" and sa.external_id = ? and extract(month from c.date_of_birth) = ?"
                + " and extract(day from c.date_of_birth) = ? and extract(year from c.date_of_birth) = ?"));
        assertArrayEquals(new Object[] { HIERARCHY + "%", "ext-1", 12, 8, 1990 }, query.getArgs());
    }

    @Test
    public void retrieveAllWithYearOnlyFiltersOnYear() {
        retrieveAll(null, null, 1990);

        final QueryCapture query = captureQuery();

        assertTrue(query.getSql().contains(" and extract(year from c.date_of_birth) = ?"));
        assertFalse(query.getSql().contains("extract(month"));
        assertFalse(query.getSql().contains("extract(day"));
        assertArrayEquals(new Object[] { HIERARCHY + "%", 1990 }, query.getArgs());
    }

    private void retrieveAll(final Integer birthMonth, final Integer birthDay, final Integer birthYear) {
        underTest.retrieveAll(SearchParameters.forSavings(null, null, null, null, null, null, birthMonth, birthDay, birthYear));
    }

    private QueryCapture captureQuery() {
        final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(paginationHelper).fetchPage(eq(jdbcTemplate), sql.capture(), args.capture(), any(RowMapper.class));
        return new QueryCapture(sql.getValue(), args.getValue());
    }

    private static final class QueryCapture {

        private final String sql;
        private final Object[] args;

        QueryCapture(final String sql, final Object[] args) {
            this.sql = sql;
            this.args = args;
        }

        String getSql() {
            return this.sql;
        }

        Object[] getArgs() {
            return this.args;
        }
    }
}
