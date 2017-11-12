package ua.compservice.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class UtilsTest {

    @DisplayName("Group test for matching a person's unique number")
    @ParameterizedTest(name= "{index} -> Tab №: {0} matches as {1}")
    @MethodSource("uniqueNumberProvider")
    void testUniqueNumberMathcing(String aNumber, boolean result) {
        Assertions.assertEquals(result, Utils.matches(aNumber));
    }

    static Stream<Arguments> uniqueNumberProvider() {
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

}
