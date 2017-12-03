package ua.compservice;

import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.compservice.config.*;
import ua.compservice.util.TimeSheetsAppUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class TimeSheetsApp {

    private static final Logger logger = LoggerFactory.getLogger(TimeSheetsApp.class);

    //region Constants
    private static final String PROGRAM_NAME = "time-sheet-transformer [options] [option arguments]";
    private static final String HOME_DIR = System.getProperty("user.dir").toString();
    private static final String SUBFOLDER = "src/files";
    private static final String DEFAULT_OUTPUT_FILE_NAME = "common.xlsx";
    private static final String DEFAULT_SHEET_NAME = "timesheet";
    //endregion

    public static void main(String[] args) {


        TimeSheetsApp app = new TimeSheetsApp();

        MergeCommand mergeCommand = new MergeCommand();
        CreateTimesheetCommand createTimesheetCommand = new CreateTimesheetCommand();
        CheckDoublesCommand checkDoublesCommand = new CheckDoublesCommand();
        CheckPersonnelNumberCommand checkPersonnelNumberCommand = new CheckPersonnelNumberCommand();
        CreateNormHoursCommand createNormHoursCommand = new CreateNormHoursCommand();
        HelpCommand helpCommand = new HelpCommand();

        JCommander commander = JCommander.newBuilder()
                .addObject(app)
                .addCommand(mergeCommand)
                .addCommand(createTimesheetCommand)
                .addCommand(checkDoublesCommand)
                .addCommand(checkPersonnelNumberCommand)
                .addCommand(createNormHoursCommand)
                .addCommand(helpCommand)
                .args(args)
                .build();

        //handler of merge command
        String parsedCommand = commander.getParsedCommand();

        //Merge
        if ("merge".equals(parsedCommand)) {

            //region merge
            logger.debug("Merge command {}", mergeCommand);

            Path homeDir = Paths.get(HOME_DIR);


            Path[] files = mergeCommand.getFiles()
                    .stream()
                    .map(s -> homeDir.resolve(Paths.get(s)))
                    .collect(Collectors.toList())
                    .toArray(new Path[0]);

            Path to = homeDir.resolve(Paths.get(mergeCommand.getOutput()));

            TimeSheetsAppUtils.merge(to, mergeCommand.withTeam(), files);
            //endregion

        } else if ("create-timesheet".equals(parsedCommand)) {

            //region Create-timesheet
            logger.debug("Create-timesheet command {} " + createTimesheetCommand);

            Path homeDir = Paths.get(HOME_DIR);

            Path source = homeDir.resolve(createTimesheetCommand.getFile());

            if (!Files.exists(source)) {

                String message = "Creating timesheet: File " + source + " doesn't exist";
                logger.error("{}", message);
                throw new TimeSheetsException(message);
            }

            Path to = homeDir.resolve(createTimesheetCommand.getOutput());

            TimeSheetsAppUtils.createTimeSheet(to, source);

            //endregion

        } else if ("check-doubles".equals(parsedCommand)) {
            //region Check-doubles
            logger.debug("check-doubles command {} " + checkDoublesCommand);

            Path currentDir = Paths.get(HOME_DIR);
            Path file = currentDir.resolve(checkDoublesCommand.getFile());

            if (!Files.exists(file)) {
                String message = String.format("File %s doesn't exist", file.toString());
                logger.error("{}", message);
                throw  new TimeSheetsException(message);
            }

            TimeSheetsAppUtils.checkDoubles(file);

            //endregion

        } else if ("check-personnel-number".equals(parsedCommand)) {

            //region Check-personnel-number
            logger.debug("check-personal-number command {} " + checkPersonnelNumberCommand);

            Path homeDir = Paths.get(HOME_DIR);

            Path file = homeDir.resolve(checkPersonnelNumberCommand.getFile());

            if (!Files.exists(file)) {
                String message = String.format("File %s doesn't exist", file.toString());
                logger.error("{}", message);
                throw  new TimeSheetsException(message);

            }

            TimeSheetsAppUtils.checkPersonnelNumber(file);

            //endregion
        } else if ("create-norm-hours".equals(parsedCommand)) {

            //region Create-norm-hours
            logger.debug("Create-norm-hours command {} " + createNormHoursCommand);

            Path homeDir = Paths.get(HOME_DIR);

            Path from = homeDir.resolve(createNormHoursCommand.getFile());

            if (!Files.exists(from)) {

                String message = "Creating norm-hours command: File " + from + " doesn't exist";
                logger.error("{}", message);
                throw new TimeSheetsException(message);
            }

            Path to = homeDir.resolve(createNormHoursCommand.getOutput());

            TimeSheetsAppUtils.createNormHours(from, to);

            //endregion


        } else if ("help".equals(parsedCommand)) {
            commander.usage();
        } else {
            commander.usage();
        }
    }


}
