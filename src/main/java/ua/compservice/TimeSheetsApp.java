package ua.compservice;

import com.beust.jcommander.JCommander;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.compservice.config.CheckDoublesCommand;
import ua.compservice.config.CheckPersonnelNumberCommand;
import ua.compservice.config.CreateTimesheetCommand;
import ua.compservice.config.MergeCommand;
import ua.compservice.model.Cell;
import ua.compservice.util.TimeSheetsAppUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimeSheetsApp {

    private static final Logger logger = LoggerFactory.getLogger(TimeSheetsApp.class);

    //region Constants
    private static final String PROGRAM_NAME = "time-sheet-transformer [options] [option arguments]";
    private static final String HOME_DIR = System.getProperty("user.dir").toString();
    private static final String SUBFOLDER = "src/files";
    public static final String DEFAULT_OUTPUT_FILE_NAME = "common.xlsx";
    public static final String DEFAULT_SHEET_NAME = "timesheet";
    //endregion

    public static void main(String[] args) {


        TimeSheetsApp app = new TimeSheetsApp();


        MergeCommand mergeCommand = new MergeCommand();
        CreateTimesheetCommand createTimesheetCommand = new CreateTimesheetCommand();
        CheckDoublesCommand checkDoublesCommand = new CheckDoublesCommand();
        CheckPersonnelNumberCommand checkPersonnelNumberCommand = new CheckPersonnelNumberCommand();

        JCommander commander = JCommander.newBuilder()
                .addObject(app)
                .addCommand(mergeCommand)
                .addCommand(createTimesheetCommand)
                .addCommand(checkDoublesCommand)
                .addCommand(checkPersonnelNumberCommand)
                .args(args)
                .build();

        //handler of merge command
        String parsedCommand = commander.getParsedCommand();

        //Merge command
        if ("merge".equals(parsedCommand)) {

            logger.debug("Merge command {}", mergeCommand);

            Path homeDir = Paths.get(HOME_DIR);


            Path[] files = mergeCommand.getFiles()
                    .stream()
                    .map(s -> homeDir.resolve(Paths.get(s)))
                    .collect(Collectors.toList())
                    .toArray(new Path[0]);

            Path to = homeDir.resolve(Paths.get(mergeCommand.getOutput()));
            TimeSheetsAppUtils.merge(to, files);

         //Create-timesheet
        } else if ("create-timesheet".equals(parsedCommand)) {

            logger.debug("Create-timesheet command {} " + createTimesheetCommand);

            Path homeDir = Paths.get(HOME_DIR);

            Path source = homeDir.resolve(createTimesheetCommand.getFile());

            if (!Files.exists(source)) {

                String message = "Creating timesheet: File " + source + " doesn't exist";
                logger.error("{}", message);
                throw new TimeSheetsException(message);
            }

            List<Cell> cells = TimeSheetsAppUtils.from(source);

            List<Cell> header = TimeSheetsAppUtils.extractHeader(cells);

            List<Cell> left = TimeSheetsAppUtils.extractLeftExceptOf(cells, header);


            //all others should be converted into time sheet cells

            List<Cell> others = cells.stream()
                    .filter(c -> !header.contains(c) && !left.contains(c))
                    .map(TimeSheetsAppUtils::newCell)
                    .collect(Collectors.toList());

            List<Cell> all = Stream.of(header, left, others)
                    .flatMap(l -> l.stream())
                    .collect(Collectors.toList());

            Path to = homeDir.resolve(createTimesheetCommand.getOutput());

            TimeSheetsAppUtils.save(to, all);
        //Check-doubles
        } else if ("check-doubles".equals(parsedCommand)) {
            logger.debug("check-doubles command {} " + createTimesheetCommand);

            Path currentDir = Paths.get(HOME_DIR);
            Path file = currentDir.resolve(checkDoublesCommand.getFile());

            if (!Files.exists(file)) {
                String message = String.format("File %s doesn't exist", file.toString());
                logger.error("{}", message);
                throw  new TimeSheetsException(message);
            } else {

                List<Cell> cells = TimeSheetsAppUtils.from(file);

                int personnelColumn = cells.stream()
                        .filter(c -> c.getValue().contains(TimeSheetsAppUtils.PERSONNEL_NUMBER_SEARCH_TEXT))
                        .map(c -> c.getColumn())
                        .findFirst()
                        .orElse(TimeSheetsAppUtils.NO_VALUE);

                Map<String, List<Cell>> doubles = cells.stream()
                        .filter(c -> (c.getColumn() == personnelColumn) && !c.getValue().isEmpty())
                        .collect(Collectors.groupingBy(Cell::getValue));


                doubles.entrySet()
                        .stream()
                        .filter(e -> e.getValue().size() > 1)
                        .forEach(e -> {

                            final List<Integer> rows = e.getValue().stream()
                                    .map(c -> c.getRow() + 1)
                                    .collect(Collectors.toList());

                            final String personnalNumber = e.getKey();
                            logger.debug("For the PN {} there are rows with doubled values. List rows -> {}",
                                    personnalNumber, rows);

                            System.out.println(String.format("For the PN %s there are rows with doubled values. List rows -> %s",
                                                        personnalNumber, rows));
                        });
            }

        //Check-personnel-number

        } else if ("check-personnel-number".equals(parsedCommand)) {

            logger.debug("check-personal-number command {} " + checkPersonnelNumberCommand);

            Path homeDir = Paths.get(HOME_DIR);

            Path file = homeDir.resolve(checkPersonnelNumberCommand.getFile());

            if (!Files.exists(file)) {
                String message = String.format("File %s doesn't exist", file.toString());
                logger.error("{}", message);
                throw  new TimeSheetsException(message);

            } else {

                List<Cell> cells = TimeSheetsAppUtils.from(file);

                int personnelColumn = cells.stream()
                        .filter(c -> c.getValue().contains(TimeSheetsAppUtils.PERSONNEL_NUMBER_SEARCH_TEXT))
                        .map(c -> c.getColumn())
                        .findFirst()
                        .orElse(TimeSheetsAppUtils.NO_VALUE);

                cells.stream()
                        .filter(c -> (c.getColumn() == personnelColumn) && !c.getValue().isEmpty() && !c.getValue().contains(TimeSheetsAppUtils.PERSONNEL_NUMBER_SEARCH_TEXT))
                        .filter(c -> !TimeSheetsAppUtils.matches(c.getValue()))
                        .forEach(c -> {
                            int row = c.getRow() + 1;
                            final String personnelNumber = c.getValue();
                            logger.info("Row: {}, the personal number {} isn't correct", row, personnelNumber);
                            System.out.println(String.format("Row: %d, the personal number %s isn't correct", row, personnelNumber));
                        });
            }
        }


    }


}
