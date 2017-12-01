package ua.compservice.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Data;

import java.util.List;

@Data
@Parameters(commandNames = {"merge"}, commandDescription = "Merge xlsx files into an one output file")
public class MergeCommand {

    @Parameter(description = "Merge files", required = true)
    private List<String> files;

    @Parameter(description = "insert a column with team name", required = false)
    private boolean useTeam;

    @Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
    private String output;


}
