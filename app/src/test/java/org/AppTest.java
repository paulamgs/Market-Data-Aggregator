package org;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.example.MarketDataAggregator;
import org.example.MarketRecord;
import org.example.MarketWeights;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;

class MarketDataAggregatorTest {

    private MarketRecord record1;
    private MarketRecord record2;
    private MarketRecord record3;
    private MarketRecord record4;
    private MarketRecord record5;
    private MarketRecord record6;
    private MarketDataAggregator aggregator;
    private final Map<String, Double> WEIGHTS = Map.of("ABC", 0.1, "RST", 0.3);

    @BeforeEach
    void setUp() {
        record1 = new MarketRecord(LocalDateTime.of(2025, 2, 14, 9, 30), "MEGA", 150.0, 1000); 
        record2 = new MarketRecord(LocalDateTime.of(2025, 2, 15, 10, 30), "ABC", 155.0, 2000);
        record3 = new MarketRecord(LocalDateTime.of(2025, 2, 15, 11, 30), "MEGA", 2500.0, 1500);
        record4 = new MarketRecord(LocalDateTime.of(2025, 2, 17, 9, 30), "ABC", 160.0, 1200);
        record5 = new MarketRecord(LocalDateTime.of(2025, 2, 15, 11, 30), "NGL", 2500.0, 1500);
        record6 = new MarketRecord(LocalDateTime.of(2025, 2, 15, 9, 32), "TRX", 170.0, 1200);

        aggregator = new MarketDataAggregator();
    }

    @Test
    void testComputeDailyIndexNotAllWeightedTickers() {
        List<MarketRecord> recordsDay1 = Arrays.asList(record1);  

        aggregator.processData(recordsDay1);

        assertTrue(Double.isNaN(aggregator.getLastKnownIndex().get()));
    }


    @Test
    void testComputeDailyIndexAllWeightedTickersForFeb15() {
        List<MarketRecord> recordsFeb15 = Arrays.asList(record2, record3, record5, record6);
        aggregator.processData(recordsFeb15);

        assertEquals(1799.5, aggregator.getLastKnownIndex().get());
    }
}
