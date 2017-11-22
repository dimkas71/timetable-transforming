package ua.compservice;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        Option mergeOption = Option.builder("m")
                .hasArgs()
                .argName("files")
                .desc("Merge files")
                .longOpt("merge")
                .type(String[].class)
                .optionalArg(true)
                .build();

        Option helpOption = Option.builder("h")
                .desc("print help usage")
                .longOpt("help")
                .hasArg(false)
                .optionalArg(true)
                .build();

        Option outputFileOption = Option.builder("o")
                .hasArg(true)
                .longOpt("output")
                .desc("output file name")
                .type(String.class)
                .argName("file")
                .optionalArg(true)
                .build();

        Option inputFileBuilder = Option.builder("f")
                .hasArg(true)
                .longOpt("file")
                .desc("input file")
                .type(String.class)
                .argName("inputFile")
                .optionalArg(true)
                .build();

        Option sheetNameOption = Option.builder("sn")
                .hasArg(true)
                .longOpt("sheet-name")
                .desc("sheet name")
                .type(String.class)
                .argName("sheet-name")
                .optionalArg(true)
                .build();

        Option checkDoublesOption = Option.builder("cd")
                .hasArg(false)
                .longOpt("check-doubles")
                .desc("check uniqueness personnel number")
                .optionalArg(true)
                .build();

        Option checkPersonnelNumberOption = Option.builder("cp")
                .hasArg(false)
                .longOpt("check-personnal-numbers")
                .desc("check personnal numbers for correctness")
                .optionalArg(true)
                .build();


        Options options = new Options();

        options.addOption(mergeOption);
        options.addOption(helpOption);
        options.addOption(outputFileOption);
        options.addOption(inputFileBuilder);

        options.addOption(sheetNameOption);
        options.addOption(checkDoublesOption);
        options.addOption(checkPersonnelNumberOption);

        CommandLineParser parser = new DefaultParser();

        //String[] testArgs = {"-m", "11.xlsx","12.xlsx","13.xlsx","21.xlsx","22.xlsx","23.xlsx","31.xlsx","32.xlsx","33.xlsx","41.xlsx","42.xlsx","43.xlsx","44.xlsx","45.xlsx","46.xlsx","47.xlsx","49.xlsx","50.xlsx", "-o", "out.xlsx"};

        //String[] testArgs = {"-f", "out.xlsx", "-sn", "timesheet"};
        //String[] testArgs = {"-f", "out2.xlsx", "-cd"};

        String[] testArgs = {"-f", "out2.xlsx", "-cp"};



        try {
            CommandLine commandLine = parser.parse(options, testArgs);
            if (commandLine.hasOption("m")) {

                String[] files = commandLine.getOptionValues("m");
                logger.info("merge the list of [{}] files", files);

                final Path currentDir = Paths.get(HOME_DIR).resolve(SUBFOLDER);

                Path[] paths = Stream.of(files)
                        .map(f -> currentDir.resolve(Paths.get(f)))
                        .collect(Collectors.toList())
                        .toArray(new Path[0]);

                logger.info("List of paths {}", paths);

                Path to = currentDir.resolve(Paths.get(DEFAULT_OUTPUT_FILE_NAME));

                if (commandLine.hasOption("o")) {
                    String output = commandLine.getOptionValue("o", DEFAULT_OUTPUT_FILE_NAME);
                    to = currentDir.resolve(Paths.get(output));
                }


                TimeSheetsAppUtils.merge(to, paths);


            } else if (commandLine.hasOption("f") && commandLine.hasOption("sn")) {

                Path currentDir = Paths.get(HOME_DIR).resolve(SUBFOLDER);

                Path source = currentDir.resolve(commandLine.getOptionValue("f", DEFAULT_OUTPUT_FILE_NAME));

                String sheetName = commandLine.getOptionValue("sh", DEFAULT_SHEET_NAME);


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
                        .map(c -> {

                            String value = c.getValue();
                            if (value.isEmpty())
                                return new Cell(c.getRow(), c.getColumn(), value);

                            String[] values = value.split("\n");

                            int hours = 0;

                            for (String v : values) {
                                hours += TimeSheetsAppUtils.toWorkingHours(v.trim());
                            }

                            return new Cell(c.getRow(), c.getColumn(), String.valueOf(hours));

                        })
                        .collect(Collectors.toList());

                List<Cell> all = Stream.of(header, left, others)
                        .flatMap(l -> l.stream())
                        .collect(Collectors.toList());

                TimeSheetsAppUtils.save(currentDir.resolve("out2.xlsx"), all, sheetName);

            } else if (commandLine.hasOption("f") && commandLine.hasOption("cd")) {
                Path currentDir = Paths.get(HOME_DIR).resolve(SUBFOLDER);
                Path file = currentDir.resolve(commandLine.getOptionValue("f", DEFAULT_OUTPUT_FILE_NAME));

                if (!Files.exists(file)) {
                    logger.error("file {} doesn't exist", file);
                } else {

                    List<Cell> cells = TimeSheetsAppUtils.from(file);

                    int personnelColumn = cells.stream()
                            .filter(c -> c.getValue().contains(TimeSheetsAppUtils.PERSONNEL_NUMBER_SEARCH_TEXT))
                            .map(c -> c.getColumn())
                            .findFirst()
                            .orElse(TimeSheetsAppUtils.NO_VALUE);

                    Map<String, List<Cell>> aMap = cells.stream()
                            .filter(c -> (c.getColumn() == personnelColumn) && !c.getValue().isEmpty())
                            .collect(Collectors.groupingBy(Cell::getValue));

                    for (Map.Entry<String, List<Cell>> entry : aMap.entrySet()) {
                        if (entry.getValue().size() > 1) {
                            //Log it...
                            logger.info("For the PN {} there are rows with doubled values. List rows -> {}", entry.getKey(), entry.getValue().stream().map(c -> c.getRow() + 1).collect(Collectors.toList()));
                        }

                    }



                }


            } else if (commandLine.hasOption("f") && commandLine.hasOption("cp")) {
                Path currentDir = Paths.get(HOME_DIR).resolve(SUBFOLDER);
                Path file = currentDir.resolve(commandLine.getOptionValue("f", DEFAULT_OUTPUT_FILE_NAME));

                if (!Files.exists(file)) {
                    logger.error("file {} doesn't exist", file);
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
                                logger.info("Row: {}, the personal number {} isn't correct", c.getRow() + 1, c.getValue());
                            });
                }

            } else {
                //Print help information
                HelpFormatter formatter = new HelpFormatter();

                formatter.printHelp(PROGRAM_NAME, options, false);
            }

        } catch (UnrecognizedOptionException e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();
        } catch (ParseException e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();
        }





    }
}
