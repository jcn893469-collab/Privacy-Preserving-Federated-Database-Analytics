package com.fpa.localprocessing;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Local CSV validation and preprocessing. Runs entirely inside the
 * organization's own request — the uploaded file and every raw value it
 * contains are discarded at the end of the HTTP request that uploaded them;
 * nothing here is persisted, logged, or forwarded to the central server.
 *
 * Rules enforced:
 *  - a header row must be present and must contain the requested target column
 *  - the target column's values must parse as numbers
 *  - blank / non-numeric / malformed rows are skipped (recorded as
 *    rejections) rather than crashing the whole upload
 *  - an organization-supplied plausible value range further filters out
 *    obviously invalid entries (e.g. negative ages) before any statistics
 *    are computed
 */
@Component
public class CsvPreprocessor {

    private static final int MAX_ROWS = 200_000;

    public CsvValidationResult parseAndValidate(InputStream csvInputStream,
                                                 String targetColumn,
                                                 double plausibleMin,
                                                 double plausibleMax) throws IOException {

        CsvValidationResult result = new CsvValidationResult();

        try (InputStreamReader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setIgnoreSurroundingSpaces(true)
                 .setTrim(true)
                 .build()
                 .parse(reader)) {

            if (!parser.getHeaderMap().containsKey(targetColumn)) {
                throw new IllegalArgumentException(
                    "CSV does not contain the requested target column: '" + targetColumn + "'. "
                        + "Available columns: " + parser.getHeaderMap().keySet());
            }

            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rowNumber++;
                result.incrementRowsSeen();

                if (rowNumber > MAX_ROWS) {
                    result.reject(rowNumber, "Row limit of " + MAX_ROWS + " exceeded; remaining rows skipped");
                    break;
                }

                String rawCell = record.isMapped(targetColumn) ? record.get(targetColumn) : null;
                if (rawCell == null || rawCell.isBlank()) {
                    result.reject(rowNumber, "Missing value in column '" + targetColumn + "'");
                    continue;
                }

                double value;
                try {
                    value = Double.parseDouble(rawCell.trim());
                } catch (NumberFormatException ex) {
                    result.reject(rowNumber, "Non-numeric value: '" + rawCell + "'");
                    continue;
                }

                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    result.reject(rowNumber, "Non-finite value");
                    continue;
                }

                if (value < plausibleMin || value > plausibleMax) {
                    result.reject(rowNumber, "Value " + value + " outside plausible range ["
                        + plausibleMin + ", " + plausibleMax + "]");
                    continue;
                }

                result.accept(value);
            }
        }

        if (result.getValidRowCount() == 0) {
            throw new IllegalArgumentException(
                "No valid numeric rows found in column '" + targetColumn + "' after validation. "
                    + "Rows seen: " + result.getTotalRowsSeen() + ", rejected: " + result.getRejectedRowCount());
        }

        return result;
    }
}