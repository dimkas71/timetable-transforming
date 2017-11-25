package ua.compservice.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Data;

@Data
@Parameters(commandNames = {"create-timesheet"}, commandDescription = "create timesheet from a file")
public class CreateTimesheetCommand {

    @Parameter(description = "file", required = true)
    private String file;

    @Parameter(names = {"-o", "--output"}, description = "output file", required = true)
    private String output;


}
