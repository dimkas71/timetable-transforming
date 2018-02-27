package ua.compservice.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
@Parameters(commandNames = {"merge-sheets"}, commandDescription = "Merge sheets from an input file to the output file")
public class MergeSheetsCommand {
	
	@Parameter(names = {"-f", "--file"}, description = "input file", required = true)
	private String input;
	
	@Parameter(names = { "-wt", "--with-team" }, description = "if used then column TEAM will be "
			+ "inserted before the first timesheet's day(1). The team name will get from a sheet's name", required = false)
	@Getter(AccessLevel.NONE)
	private boolean withTeam;

	public boolean withTeam() {
		return this.withTeam;
	}

	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	private String output;

}
