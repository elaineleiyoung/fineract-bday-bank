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
package org.apache.fineract.portfolio.savings.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SavingsAccountDataValidatorTest {

    private final SavingsAccountDataValidator validator = new SavingsAccountDataValidator(new FromJsonHelper());

    private static Stream<Arguments> provideValidBirthFilters() {
        return Stream.of(Arguments.of(null, null, null), Arguments.of("", " ", ""), Arguments.of("12", "8", "1990"),
                Arguments.of("1", "31", null), Arguments.of(null, "31", null), Arguments.of("2", "29", null),
                Arguments.of("2", "29", "2024"));
    }

    private static Stream<Arguments> provideInvalidBirthFilters() {
        return Stream.of(Arguments.of("dec", null, null, "birthMonth.not.a.number"),
                Arguments.of("99999999999", null, null, "birthMonth.not.a.number"),
                Arguments.of("0", null, null, "birthMonth.is.not.within.expected.range"),
                Arguments.of("13", null, null, "birthMonth.is.not.within.expected.range"),
                Arguments.of(null, "0", null, "birthDay.is.not.within.expected.range"),
                Arguments.of(null, "32", null, "birthDay.is.not.within.expected.range"),
                Arguments.of(null, null, "0", "birthYear.is.not.within.expected.range"),
                Arguments.of(null, null, "10000", "birthYear.is.not.within.expected.range"),
                Arguments.of("2", "30", null, "birthDay.invalid.month"), Arguments.of("4", "31", null, "birthDay.invalid.month"),
                Arguments.of("2", "29", "2023", "birthDay.invalid.month"));
    }

    @ParameterizedTest
    @MethodSource("provideValidBirthFilters")
    public void validateForRetrieveAllBirthdayAcceptsValidBirthFilters(final String birthMonth, final String birthDay,
            final String birthYear) {
        assertDoesNotThrow(() -> validator.validateForRetrieveAllBirthday(birthMonth, birthDay, birthYear));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidBirthFilters")
    public void validateForRetrieveAllBirthdayRejectsInvalidBirthFilter(final String birthMonth, final String birthDay,
            final String birthYear, final String expectedError) {
        final List<ApiParameterError> errors = validateAndGetErrors(birthMonth, birthDay, birthYear);

        assertEquals(1, errors.size());
        assertEquals("validation.msg.savingsaccount." + expectedError, errors.get(0).getUserMessageGlobalisationCode());
    }

    @Test
    public void validateForRetrieveAllBirthdayReportsAllErrorsTogether() {
        final List<ApiParameterError> errors = validateAndGetErrors("dec", "32", "abc");

        assertEquals(3, errors.size());
        assertEquals("birthMonth", errors.get(0).getParameterName());
        assertEquals("birthDay", errors.get(1).getParameterName());
        assertEquals("birthYear", errors.get(2).getParameterName());
    }

    @Test
    public void validateForRetrieveAllBirthdaySkipsMonthDayCheckWhenMonthIsInvalid() {
        final List<ApiParameterError> errors = validateAndGetErrors("13", "31", null);

        assertEquals(1, errors.size());
        assertEquals("birthMonth", errors.get(0).getParameterName());
    }

    private List<ApiParameterError> validateAndGetErrors(final String birthMonth, final String birthDay, final String birthYear) {
        final PlatformApiDataValidationException exception = assertThrows(PlatformApiDataValidationException.class,
                () -> validator.validateForRetrieveAllBirthday(birthMonth, birthDay, birthYear));
        return exception.getErrors();
    }
}
