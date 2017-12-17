package ua.compservice.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import lombok.Data;

@Data
@Parameters(commandNames = {"write-norm-hours"}, commandDescription = "Write norm hours from a file to an url")
public class WriteNormHoursCommand {
	
	@Parameter(names = {"-f", "--file"}, description = "an input file int xlsx format")
	private String file;
	
	@Parameter(names = {"-u", "--url"}, description = "an url to write json based info, for example http://server:port/base_path/hours/20711201")
	private String url;

}
