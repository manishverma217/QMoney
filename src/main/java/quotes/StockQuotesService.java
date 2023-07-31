package quotes;

import com.fasterxml.jackson.core.JsonProcessingException;
import dto.Candle;
import exception.StockQuoteServiceException;

import java.time.LocalDate;
import java.util.List;

public interface StockQuotesService {
    List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
            throws JsonProcessingException , StockQuoteServiceException;

}