package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * This class processes market data records, computes daily statistics for each ticker,
 * and calculates a weighted market index based on the available market records.
 * <p>
 * It allows processing of multiple market records across different days and computes the following:
 * - Open, close, highest, lowest price and traded volume for each ticker.
 * - The market index based on the weighted closing prices of specific tickers.
 * </p>
 */
public class MarketDataAggregator {

    private Set<String> allTickersSet;
    private final AtomicReference<Double> lastKnownIndex = new AtomicReference<>(Double.NaN);
    private final Map<String, Double> dailyClosingPrice = new ConcurrentHashMap<>();

    /**
     * Processes a list of market records and computes daily statistics and index values.
     * <p>
     * The records are grouped by date, and for each day, the statistics for each ticker are calculated.
     * If all weighted tickers are present for the day, the market index is calculated.
     * </p>
     *
     * @param marketRecords A list of market records to process.
     */
    public void processData(List<MarketRecord> marketRecords) {
        this.allTickersSet = getUniqueTickers(marketRecords);
        marketRecords.stream()
            .collect(Collectors.groupingBy(
                record -> record.dateTime().toLocalDate(),
                TreeMap::new,
                Collectors.toList()))
            .forEach((date, records) -> processDailyRecords(date, records));
    }

    /**
     * Processes the market records for a specific day, calculates daily statistics for each ticker,
     * and computes the daily market index.
     *
     * @param date    The date of the market records.
     * @param records The list of market records for that day.
     */
    private void processDailyRecords(LocalDate date, List<MarketRecord> records) {
        System.out.printf("Date %s%n", date);
        var groupedByTicker = groupByTicker(records);
        groupedByTicker.entrySet().stream()
            .forEach(entry -> computeDailyStats(entry.getKey(), entry.getValue()));
        computeDailyIndex(records);
        groupedByTicker.clear();
    }

    /**
     * Computes the daily statistics (open, close, highest, lowest prices, and traded volume)
     * for a specific ticker using the provided market records.
     *
     * @param ticker       The ticker symbol for the market record.
     * @param marketRecords A list of market records for the specified ticker.
     */
    private void computeDailyStats(String ticker, List<MarketRecord> marketRecords) {
        System.out.println(String.format("Ticker: %s", ticker));

        if (marketRecords.isEmpty()) {
            printDailyStats(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0);
        } else if (marketRecords.size() == 1) {
            MarketRecord record = marketRecords.get(0);
            double price = record.price();
            double tradedVolume = record.price() * record.volume();
            updateDailyClosingPrice(ticker, price);
            printDailyStats(Optional.of(price), Optional.of(price), Optional.of(price), Optional.of(price), tradedVolume);
        } else {
            MarketRecord firstTrade = marketRecords.get(0);
            MarketRecord lastTrade = firstTrade;

            double highestPrice = firstTrade.price();
            double lowestPrice = firstTrade.price();
            double tradedVolume = 0.0;

            for (MarketRecord record : marketRecords) {
                double price = record.price();
                double volume = record.volume();
                LocalDateTime time = record.dateTime();

                if (time.isBefore(firstTrade.dateTime())) {
                    firstTrade = record;
                }
                if (time.isAfter(lastTrade.dateTime())) {
                    lastTrade = record;
                }

                if (price > highestPrice) highestPrice = price;
                if (price < lowestPrice) lowestPrice = price;

                tradedVolume += price * volume;
            }

            updateDailyClosingPrice(ticker, lastTrade.price());
            printDailyStats(Optional.of(firstTrade.price()), Optional.of(lastTrade.price()), 
                Optional.of(highestPrice), Optional.of(lowestPrice), tradedVolume);
        }
    }

    /**
     * Computes the market index for the day based on the weighted closing prices of the required tickers.
     * <p>
     * If all the weighted tickers are present for the day, the index is calculated based on their respective weights.
     * If any weighted tickers are missing, the last known index is used instead.
     * </p>
     *
     * @param marketRecords A list of market records for the day.
     */
    private void computeDailyIndex(List<MarketRecord> marketRecords) {
        Set<String> weightedTickers = MarketWeights.WEIGHTS.keySet();
        Set<String> tickersInRecords = marketRecords.stream()
            .map(MarketRecord::ticker)
            .collect(Collectors.toSet());
        if (tickersInRecords.containsAll(weightedTickers)){
            double indexValue = 0.0;
            for (String ticker : weightedTickers) {
                if (dailyClosingPrice.containsKey(ticker)) {
                    double closingPrice = dailyClosingPrice.get(ticker);
                    double weight = MarketWeights.WEIGHTS.get(ticker);
                    indexValue += weight * closingPrice;
                }
            }
            System.out.println(String.format("Daily Index: %.2f%n", indexValue));
            updateLastKnownIndex(indexValue);
        } else{
            if (Double.isNaN(lastKnownIndex.get())){
                System.out.println("Some weighted tickers are missing. Cannot calculate the index.");
            } else {
                System.out.println(String.format("Some weighted tickers are missing. Using last known index: %.2f", lastKnownIndex.get()));
            }
        }
        dailyClosingPrice.clear();
    }

    /**
     * Groups the provided market records by ticker symbol.
     * <p>
     * If a ticker does not have any market records, an empty list is inserted for it.
     * </p>
     *
     * @param marketRecords A list of market records to be grouped.
     * @return A map of tickers and their respective market records.
     */
    private Map<String, List<MarketRecord>> groupByTicker(List<MarketRecord> marketRecords) {
        Map<String, List<MarketRecord>> groupedByTicker = marketRecords.stream()
            .collect(Collectors.groupingBy(
                MarketRecord::ticker,
                TreeMap::new,
                Collectors.toList()));
        allTickersSet.forEach(ticker -> groupedByTicker.putIfAbsent(ticker, new ArrayList<>()));
        return groupedByTicker;
    }

    /**
     * Extracts the unique set of tickers from the provided market records.
     *
     * @param marketRecords A list of market records to extract tickers from.
     * @return A set of unique tickers.
     */
    private static Set<String> getUniqueTickers(List<MarketRecord> marketRecords) {
        return marketRecords.stream()
                .map(MarketRecord::ticker)
                .collect(Collectors.toSet());
    }

    /**
     * Prints the value for a given label, formatted to one decimal point.
     *
     * @param label The label describing the value.
     * @param value The value to be printed.
     */
    private void printValue(String label, Optional<Double> value) {
        if (value.isPresent()) {
            System.out.printf("%s: %.1f%n", label, value.get());
        } else {
            System.out.printf("%s: N/A%n", label);
        }
    }

    /**
     * Prints the daily statistics for open, close, highest, and lowest prices, as well as traded volume.
     *
     * @param openPrice   The opening price of the ticker.
     * @param closePrice  The closing price of the ticker.
     * @param highestPrice The highest price of the ticker during the day.
     * @param lowestPrice The lowest price of the ticker during the day.
     * @param tradedVolume The total traded volume for the ticker.
     */
    private void printDailyStats(Optional<Double> openPrice, Optional<Double> closePrice,
                             Optional<Double> highestPrice, Optional<Double> lowestPrice,
                             double tradedVolume) {
        printValue("Open price", openPrice);
        printValue("Close price", closePrice);
        printValue("Highest price", highestPrice);
        printValue("Lowest price", lowestPrice);
        System.out.printf("Traded volume: %.1f%n", tradedVolume);
    }

    /**
     * Updates the closing price for a specified ticker.
     *
     * @param ticker The ticker symbol to update.
     * @param price The closing price to update for the ticker.
     */
    public void updateDailyClosingPrice(String ticker, double price) {
        dailyClosingPrice.put(ticker, price);
    }

    /**
     * Updates the last known index value.
     *
     * @param index The new index value to be set.
     */
    public void updateLastKnownIndex(double index) {
        lastKnownIndex.set(index);
    }

    /**
     * Retrieves the last known market index value.
     *
     * @return An {@link AtomicReference} containing the last known index value.
     */
    public AtomicReference<Double> getLastKnownIndex() {
        return lastKnownIndex;
    }

    /**
     * Retrieves the daily closing prices for all tickers.
     *
     * @return A map containing tickers as keys and their respective closing prices as values.
     */
    public Map<String, Double> getDailyClosingPrice() {
        return dailyClosingPrice;
    }
}
