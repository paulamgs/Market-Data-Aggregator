package org.example;

import java.time.LocalDateTime;

public record MarketRecord(LocalDateTime dateTime, String ticker, double price, int volume) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(dateTime)
          .append(" | Ticker: ").append(ticker)
          .append(" | Price: ").append(price)
          .append(" | Volume: ").append(volume);
        return sb.toString();
    }
}
