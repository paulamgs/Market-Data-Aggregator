/*
 * This source file was generated by the Gradle 'init' task
 */
package org.example;

import java.util.List;

public class App {
    public static void main(String[] args) {
         String pathString = "src/main/resources/market_data.ssv";
         
         List<MarketRecord> marketRecords = MarketDataReader.readMarketData(pathString);
         MarketDataAggregator aggregator = new MarketDataAggregator();
         aggregator.processData(marketRecords);

    }
}
