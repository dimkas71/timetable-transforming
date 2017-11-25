package ua.compservice.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Data;

@Data
@Parameters(commandNames = {"check-doubles"}, commandDescription = "Check personal number doubles")
public class CheckDoublesCommand {

    @Parameter(description = "file")
    private String file;

}
