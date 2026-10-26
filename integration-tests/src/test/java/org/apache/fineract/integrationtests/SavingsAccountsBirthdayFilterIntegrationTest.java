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
package org.apache.fineract.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.util.List;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsProductHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SavingsAccountsBirthdayFilterIntegrationTest {

    private static final String ACCOUNT_TYPE_INDIVIDUAL = "INDIVIDUAL";
    private static final String ACTIVATION_DATE = "01 January 2023";
    private static final String UNLIMITED = "limit=0";

    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;
    private SavingsAccountHelper savingsAccountHelper;
    private SavingsAccountHelper savingsAccountHelperValidationError;

    private Integer august28th1990SavingsId;
    private Integer august28th2000SavingsId;
    private Integer august29th1990SavingsId;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelperValidationError = new SavingsAccountHelper(this.requestSpec,
                new ResponseSpecBuilder().expectStatusCode(400).build());
    }

    @Test
    public void filterByMonthAndDayReturnsAccountsForThatBirthdayInAnyYear() {
        createSavingsAccountsForBirthdayClients();

        final List<Integer> savingsIds = getSavingsAccountIds("birthMonth=8&birthDay=28");

        assertTrue(savingsIds.contains(august28th1990SavingsId));
        assertTrue(savingsIds.contains(august28th2000SavingsId));
        assertFalse(savingsIds.contains(august29th1990SavingsId));
    }

    @Test
    public void filterByMonthDayAndYearReturnsOnlyExactDateOfBirth() {
        createSavingsAccountsForBirthdayClients();

        final List<Integer> savingsIds = getSavingsAccountIds("birthMonth=8&birthDay=28&birthYear=1990");

        assertTrue(savingsIds.contains(august28th1990SavingsId));
        assertFalse(savingsIds.contains(august28th2000SavingsId));
        assertFalse(savingsIds.contains(august29th1990SavingsId));
    }

    @Test
    public void filterByYearOnlyReturnsAccountsForClientsBornThatYear() {
        createSavingsAccountsForBirthdayClients();

        final List<Integer> savingsIds = getSavingsAccountIds("birthYear=1990");

        assertTrue(savingsIds.contains(august28th1990SavingsId));
        assertTrue(savingsIds.contains(august29th1990SavingsId));
        assertFalse(savingsIds.contains(august28th2000SavingsId));
    }

    @Test
    public void totalFilteredRecordsMatchesFilteredResults() {
        createSavingsAccountsForBirthdayClients();

        final String query = "birthMonth=8&birthDay=28&" + UNLIMITED;

        final Integer totalFilteredRecords = savingsAccountHelper.getSavingsAccounts(query, "totalFilteredRecords");
        final List<Integer> savingsIds = savingsAccountHelper.getSavingsAccounts(query, "pageItems.id");

        assertEquals(savingsIds.size(), totalFilteredRecords);
    }

    @Test
    public void withoutBirthFiltersAllAccountsAreReturned() {
        createSavingsAccountsForBirthdayClients();

        final List<Integer> savingsIds = getSavingsAccountIds(UNLIMITED);

        assertTrue(savingsIds.contains(august28th1990SavingsId));
        assertTrue(savingsIds.contains(august28th2000SavingsId));
        assertTrue(savingsIds.contains(august29th1990SavingsId));
    }

    @Test
    public void nonNumericBirthMonthReturnsValidationError() {
        assertValidationError("birthMonth=aug", "validation.msg.savingsaccount.birthMonth.not.a.number");
    }

    @Test
    public void outOfRangeBirthMonthReturnsValidationError() {
        assertValidationError("birthMonth=13", "validation.msg.savingsaccount.birthMonth.is.not.within.expected.range");
    }

    @Test
    public void dayNotInMonthReturnsValidationError() {
        assertValidationError("birthMonth=2&birthDay=30", "validation.msg.savingsaccount.birthDay.invalid.month");
    }

    private void createSavingsAccountsForBirthdayClients() {
        final Integer savingsProductId = SavingsProductHelper.createSavingsProduct(new SavingsProductHelper().build(), requestSpec,
                responseSpec);
        august28th1990SavingsId = createSavingsAccountForClientBornOn("28 August 1990", savingsProductId);
        august28th2000SavingsId = createSavingsAccountForClientBornOn("28 August 2000", savingsProductId);
        august29th1990SavingsId = createSavingsAccountForClientBornOn("29 August 1990", savingsProductId);
    }

    private Integer createSavingsAccountForClientBornOn(final String dateOfBirth, final Integer savingsProductId) {
        final Integer clientId = ClientHelper.createClientWithDateOfBirth(requestSpec, responseSpec, ACTIVATION_DATE, dateOfBirth);
        assertNotNull(clientId);
        final Integer savingsId = savingsAccountHelper.applyForSavingsApplicationOnDate(clientId, savingsProductId, ACCOUNT_TYPE_INDIVIDUAL,
                ACTIVATION_DATE);
        assertNotNull(savingsId);
        return savingsId;
    }

    private List<Integer> getSavingsAccountIds(final String query) {
        final String unlimitedQuery = query.contains(UNLIMITED) ? query : query + "&" + UNLIMITED;
        return savingsAccountHelper.getSavingsAccounts(unlimitedQuery, "pageItems.id");
    }

    private void assertValidationError(final String query, final String expectedErrorCode) {
        final List<String> errorCodes = savingsAccountHelperValidationError.getSavingsAccounts(query,
                "errors.userMessageGlobalisationCode");
        assertEquals(List.of(expectedErrorCode), errorCodes);
    }
}
