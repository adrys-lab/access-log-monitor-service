package com.adrian.rebollo.parser;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.adrian.rebollo.exception.LogLineParsingException;
import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.model.HttpMethod;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AccessLogLineParser implements Function<String, AccessLogLine> {

	//This REGEX expression matches with the Apache Common Access Log pattern, used -> https://www.regexpal.com/
	private static final String HTTP_LOG_LINE_REGEX = "^(\\S+) (\\S+) (\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}) (\\d+)";

	private static final Pattern HTTP_LOG_LINE_PATTERN = Pattern.compile(HTTP_LOG_LINE_REGEX);

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");

	@Override
	public AccessLogLine apply(String line) {

		final Matcher lineMatcher = HTTP_LOG_LINE_PATTERN.matcher(line);

		try {
			if(lineMatcher.find()) {
				LOG.debug("Proceeding to parse line={}", line);
				return AccessLogLine.builder()
						.host(safeParseString(lineMatcher.group(1)))
						.identifier(StringUtils.defaultIfBlank(safeParseString(lineMatcher.group(2)), "NO_IDENTIFIER"))
						.user(StringUtils.defaultIfBlank(safeParseString(lineMatcher.group(3)), "NO_USER"))
						.insertTime(LocalDateTime.now())
						.dateTime(ZonedDateTime.parse(lineMatcher.group(4), DATE_TIME_FORMATTER).toLocalDateTime())
						.httpMethod(HttpMethod.valueOf(lineMatcher.group(5)))
						.resource(safeParseString(lineMatcher.group(6)))
						.protocol(safeParseString(lineMatcher.group(7)))
						.returnedStatus(Integer.parseInt(lineMatcher.group(8)))
						.contentSize(Long.parseLong(lineMatcher.group(9)))
						.build();
			} else {
				throw new LogLineParsingException(String.format("An error occurred parsing the input line=%s", line));
			}
		} catch (Exception exception) {
			throw new LogLineParsingException(String.format("An error occurred parsing the input line=%s", line));
		}
	}

	private String safeParseString(String group) {
		final String safeLine = StringUtils.defaultIfBlank(group, "");
		return safeLine.equals("-") ? "" : safeLine;
	}
}
