package ua.compservice.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import ua.compservice.util.TimeSheetsAppUtils.Cell;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


class TimeSheetsAppUtilsTest {

    @DisplayName("Group test for matching a personnel number")
    @ParameterizedTest(name= "{index} -> PN: \"{0}\" -> {1}")
    @MethodSource("personnelNumberProvider")
    void test_personnel_number(String aNumber, boolean result) {
        Assertions.assertEquals(result, TimeSheetsAppUtils.matches(aNumber));
    }

    @DisplayName("For transforming the string('8/2(08:44)') to 9 hours for example'")
    @ParameterizedTest(name = "{index}, \"{0}\" -> {1}")
    @CsvFileSource(resources = "/timesheet.csv")
    void test_transforming_time_from_a_string_to_hours(String fromHours, String toHours) {
        Assertions.assertEquals(toHours, String.valueOf(TimeSheetsAppUtils.toWorkingHours(fromHours)));
    }

    @DisplayName("For defining workshift the string('8/2(08:44)') to 2'")
    @ParameterizedTest(name = "{index}, \"{0}\" -> {1}")
    @CsvFileSource(resources = "/workShift.csv")
    void test_transforming_time_from_a_string_to_workshift(String fromHours, String workshift) {
        Assertions.assertEquals(workshift, String.valueOf(TimeSheetsAppUtils.toWorkShift(fromHours)));
    }


    @DisplayName("hasDigit test")
    @ParameterizedTest(name = "{index}, \"{0}\" -> {1}")
    @CsvSource({", false", "0ddd,true", "8/1(8:00), true", "d, false", "ddd1ss, true"})
    void test_hasDirit(String toMatch, boolean result) {
        Assertions.assertEquals(result, TimeSheetsAppUtils.hasDigit(toMatch));
    }


    static Stream<Arguments> personnelNumberProvider() {
        return Stream.of(
                    Arguments.of("10/0051", true),
                    Arguments.of("10/0377", true),
                    Arguments.of("10/0361", true),
                    Arguments.of("10/0317", true),
                    Arguments.of("10/0089", true),
                    Arguments.of("10/0053", true),
                    Arguments.of("10/0092", true),
                    Arguments.of("00/1056", true),
                    Arguments.of("10/0352", true),
                    Arguments.of("10/0309", true),
                    Arguments.of("10/0339", true),
                    Arguments.of("10/0085", true),
                    Arguments.of("10/0464", true),
                    Arguments.of("10/0076", true),
                    Arguments.of("10/0272", true),
                    Arguments.of("10/0084", true),
                    Arguments.of("d0/0084", false),
                    Arguments.of("100084", false)
                );
    }

    @DisplayName("Find a header from a collection of cells")
    @ParameterizedTest
    @MethodSource({"cellsProvider"})
    void testFindHeader(List<Cell> cells) {
        int actual = TimeSheetsAppUtils.findHeader(cells);
        Assertions.assertEquals(3, actual, () -> "Should be equal to 3 at this time");
    }

    @DisplayName("If a header from a collection of cells not found should NO_VALUE returned")
    @Test
    void testHeaderNotFound() {
        int actual = TimeSheetsAppUtils.findHeader(Arrays.asList(
                new Cell(1, 1, "Some text here")
        ));

        Assertions.assertEquals(TimeSheetsAppUtils.NO_VALUE, actual, () -> "Should be returned NO_VALUE(-1)");
    }

    static Stream<List<Cell>> cellsProvider() {
        return Stream.of(Arrays.asList(

                new Cell(3, 0, "Фамилия И. О.       "),
                new Cell(3, 1, "Таб. №    "),
                new Cell(3, 2, "Должность"),
                new Cell(5, 0, "1 бригада  "),
                new Cell(24, 0, "Фамилия И. О.       "),
                new Cell(24, 1, "Таб. №    "),
                new Cell(24, 2, "Должность")
                ));
    }

}
