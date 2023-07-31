package quotes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dto.AlphavantageCandle;
import dto.AlphavantageDailyResponse;
import dto.Candle;
import exception.StockQuoteServiceException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlphavantageService implements StockQuotesService {

    public static final String TOKEN = "PC28CRDC8KVBC117";
    public static final String FUNCTION = "TIME_SERIES_DAILY";
    private RestTemplate restTemplate;

    protected AlphavantageService(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    @Override
    public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
            throws JsonProcessingException, StockQuoteServiceException {
        try {
            String url = buildUri(symbol);
            String apiResponse = restTemplate.getForObject(url, String.class);
            ObjectMapper objectMapper = getObjectMapper();

            Map<LocalDate , AlphavantageCandle> dailyResponses = objectMapper.readValue(apiResponse,
                    AlphavantageDailyResponse.class).getCandles();

            List<Candle> stocks = new ArrayList<>();
            for(LocalDate date = from ; !date.isAfter(to) ; date = date.plusDays(1)){
                AlphavantageCandle candle = dailyResponses.get(date);
                if(candle != null){
                    candle.setDate(date);
                    stocks.add(candle);
                }
            }
            return stocks;
        } catch (NullPointerException e){
            throw new StockQuoteServiceException("Alphavantage returned invalid response");
        }
    }

    protected String buildUri(String symbol) {
        String url = String.format("https://www.alphavantage.co/query?function=%s&symbol=%s&apikey=%s" ,
                FUNCTION , symbol , TOKEN);
        return url;
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
