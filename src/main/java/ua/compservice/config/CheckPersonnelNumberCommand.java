package ua.compservice.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Data;

@Data
@Parameters(commandNames = {"check-personnel-number"}, commandDescription = "Check if a personnel number matches a pattern '00/1001'")
public class CheckPersonnelNumberCommand {

    @Parameter(description = "file")
    private String file;

}
