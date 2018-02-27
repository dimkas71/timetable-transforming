package ua.compservice.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import lombok.Data;

@Data
@Parameters(commandNames = {"merge-sheets"}, commandDescription = "Merge sheets from an input file to the output file")
public class MergeSheetsCommand {
	
	@Parameter(names = {"-f", "--file"}, description = "input file", required = true)
	private String input;
	
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	private String output;

}
