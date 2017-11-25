package ua.compservice.config;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Data;

@Data
@Parameters(commandNames = {"create-norm-hours"}, commandDescription = "Create norm hours command from a file")
public class CreateNormHoursCommand {

    @Parameter(names = {"-f", "--file"}, description = "input file", required = true)
    private String file;

    @Parameter(names = {"-o", "--output"}, description = "output file",required = true)
    private String output;

}
