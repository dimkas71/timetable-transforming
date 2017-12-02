package ua.compservice.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Parameters(commandNames = {"merge"}, commandDescription = "Merge xlsx files into an one output file")
public class MergeCommand {

    @Parameter(description = "Merge files", required = true)
    private List<String> files;

    @Parameter(names = {"-with-team"}, description = "if used then column TEAM will be " +
            "inserted before the first timesheet's day(1)" , required = false)
    @Getter(AccessLevel.NONE) private boolean withTeam;

    public boolean withTeam() {
        return this.withTeam;
    }

    @Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
    private String output;


}
