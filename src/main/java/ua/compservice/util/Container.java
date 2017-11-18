package ua.compservice.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ua.compservice.model.ImmutableCell;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.file.Paths.get;
import static ua.compservice.util.WorkbookUtils.TABLE_NUMBER_SEARCH_TEXT;
import static ua.compservice.util.WorkbookUtils.from;

public class Container {
    public static void main(String[] args) {
        Path currentDir = get(System.getProperty("user.dir").toString()).resolve("src/files");

        List<ImmutableCell> cells = from(currentDir.resolve("out.xlsx"));

        int headerRowNumber = cells.stream()
                .filter(c -> c.getValue().contains(TABLE_NUMBER_SEARCH_TEXT))
                .map(c -> c.getRow())
                .findFirst()
                .orElse(-1);

        List<ImmutableCell> headerCells = cells.stream()
                .filter(c -> c.getRow() == headerRowNumber)
                .collect(Collectors.toList());


        List<Integer> columns = headerCells.stream()
                .filter(c -> c.getValue().chars().allMatch(ch -> Character.isDigit(ch)))
                .map(c -> c.getColumn())
                .collect(Collectors.toList());


        Set<String> uniqueValues = cells.stream()
                .filter(c -> (columns.contains(c.getColumn()) && (c.getRow() > headerRowNumber)) && !c.getValue().isEmpty())
                .flatMap(c -> Arrays.stream(c.getValue().split("\n")))
                .collect(Collectors.toSet());


        List<Utils2.TimeHolder> holders = uniqueValues.stream()
                .filter(s -> Utils2.hasDigit(s))
                .map(s -> Utils2.from(s))
                .collect(Collectors.toList());


        System.out.println(holders);







    }
}

class Utils2 {

    public static final String TIME_REGEX_EXPRESSION
            = "(?<hours>\\d{1})\\/(?<workshift>\\d{1})\\((?<hoursminutes>\\d{1}:\\d{2})\\)";

    public static final String DIGIT_FIND_REGEX_EXPRESSION = "\\d{1,}";


    private static final Pattern DIGIT_FOUNDER_PATTERN = Pattern.compile(DIGIT_FIND_REGEX_EXPRESSION);
    private static final Pattern TIME_FOUNDER_PATTERN = Pattern.compile(TIME_REGEX_EXPRESSION);


    static boolean hasDigit(String aString) {
        return DIGIT_FOUNDER_PATTERN.matcher(aString).find();
    }


    static TimeHolder from(String aString) {

        int hours = 0;
        int workShift = 1; //
        int hoursForWorkShift = 0;
        int minutesForWorkShift = 0;

        Matcher m = TIME_FOUNDER_PATTERN.matcher(aString);

        while (m.find()) {

            //1.Try to find hours

            try {
                hours = Integer.parseInt(m.group("hours"));
            } catch (NumberFormatException e) {}
            catch (IllegalStateException e) {}

            //2. Try to find work shift
            try {
                workShift = Integer.parseInt(m.group("workshift"));
            } catch (NumberFormatException e) {}
            catch (IllegalStateException e) {}

            //3. Hours and minutes for workshift

            try {
                String hm = m.group("hoursminutes");

                hoursForWorkShift = Integer.parseInt(hm.split(":")[0]);
                minutesForWorkShift = Integer.parseInt(hm.split(":")[1]);

            } catch (NumberFormatException e) {}
            catch (IllegalStateException e) {}


        }

        TimeHolder.TimeHolderBuilder builder = TimeHolder.builder();

        return builder
                .hours(hours)
                .workShift(workShift)
                .hoursForWorkShift(hoursForWorkShift)
                .minutesForWorkShift(minutesForWorkShift).build();



    }

    @Data
    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    public static class TimeHolder {

        static final int SECONDS_IN_MINUTE = 60;
        public static final double PRESISION = 0.5;

        int hours;
        int workShift;
        int hoursForWorkShift;
        int minutesForWorkShift;

        int toHours() {

            float part = minutesForWorkShift / SECONDS_IN_MINUTE;

            return hoursForWorkShift + (part < PRESISION ? 0 : 1);
        }

    }


}