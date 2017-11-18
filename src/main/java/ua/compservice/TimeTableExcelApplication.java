package ua.compservice;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.compservice.util.WorkbookUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimeTableExcelApplication {

    private static final Logger logger = LoggerFactory.getLogger(TimeTableExcelApplication.class);

    private static final String PROGRAM_NAME = "excel-helper";
    private static final String HOME_DIR = System.getProperty("user.dir").toString();
    private static final String SUBFOLDER = "src/files";
    public static final String DEFAULT_OUTPUT_FILE_NAME = "common.xlsx";

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

        Options options = new Options();
        options.addOption(mergeOption);
        options.addOption(helpOption);
        options.addOption(outputFileOption);

        CommandLineParser parser = new DefaultParser();

        String[] testArgs = {"-m", "11.xlsx","12.xlsx","13.xlsx","21.xlsx","22.xlsx","23.xlsx","31.xlsx","32.xlsx","33.xlsx","41.xlsx","42.xlsx","43.xlsx","44.xlsx","45.xlsx","46.xlsx","47.xlsx","49.xlsx","50.xlsx", "-o", "out.xlsx"};


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


                WorkbookUtils.merge(to, paths);


            } else {
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
