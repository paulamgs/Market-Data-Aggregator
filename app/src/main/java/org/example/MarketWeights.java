package org.example;
import java.util.Map;

public class MarketWeights {
    public static final Map<String, Double> WEIGHTS = Map.of(
        "ABC", 0.1,
        "MEGA", 0.3,
        "NGL", 0.4,
        "TRX", 0.2
    );

    public static double getWeight(String ticker) {
        return WEIGHTS.getOrDefault(ticker, 0.0);
    }
}
