package org.example;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord; 


public class MarketDataReader {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static List<MarketRecord> readMarketData(String pathString){
        List<MarketRecord> marketRecords = new ArrayList<>();
        try (Reader reader = new InputStreamReader(new FileInputStream(pathString), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setDelimiter(';')
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build())) {

            for (CSVRecord record : parser) {
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(record.get(0), DATE_TIME_FORMATTER);
                    String ticker = record.get(1);
                    double price = Double.parseDouble(record.get(2));
                    int volume = Integer.parseInt(record.get(3));
                    
                    if (price <= 0 || volume < 0 || ticker == null || ticker.isEmpty()) {
                        System.out.println("Invalid record found, skipping line: " + record);
                    } else{
                        MarketRecord marketRecord = new MarketRecord(dateTime, ticker, price, volume);
                        marketRecords.add(marketRecord);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid record: " + record);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error: File not found - " + pathString);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return marketRecords;
    }
}
