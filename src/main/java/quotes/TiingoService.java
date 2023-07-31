package quotes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dto.Candle;
import dto.TiingoCandle;
import exception.StockQuoteServiceException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TiingoService implements StockQuotesService {
    public static final String TOKEN = "8dc5b141f8ac2f63fd979620fe8c8550938eb0e0";

    private RestTemplate restTemplate;

    protected TiingoService(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    @Override
    public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
            throws JsonProcessingException, StockQuoteServiceException {
        if(from.compareTo(to) >= 0){
            throw new RuntimeException();
        }
        try {
            String url = buildUri(symbol, from, to);
            String stocks = restTemplate.getForObject(url, String.class);

            ObjectMapper objectMapper = getObjectMapper();

            TiingoCandle[] stocksStartToEndDate = objectMapper.readValue(stocks, TiingoCandle[].class);

            if(stocksStartToEndDate == null){
                return new ArrayList<Candle>();
            }
            List<Candle> stocksList = Arrays.asList(stocksStartToEndDate);
            return stocksList;
        } catch (NullPointerException e){
            throw new StockQuoteServiceException("Error occurred when calling tiingo API" ,
                    e.getCause());
        }
    }
    protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {

        String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
                + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
        String url = uriTemplate.replace("$SYMBOL" , symbol).replace("$STARTDATE" , startDate.toString())
                .replace("$ENDDATE" , endDate.toString()).replace("$APIKEY", TOKEN);
        return url;

    }
    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}

