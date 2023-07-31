package quotes;

import org.springframework.web.client.RestTemplate;

public final class StockQuoteServiceFactory {

    public final static StockQuoteServiceFactory INSTANCE = new StockQuoteServiceFactory();
    private StockQuoteServiceFactory(){

    }
    public StockQuotesService getService(String provider, RestTemplate restTemplate) {
        if(provider == null){
            return new AlphavantageService(restTemplate);
        } else if(provider.equalsIgnoreCase("tiingo")){
            return new TiingoService(restTemplate);
        } else {
            return new AlphavantageService(restTemplate);
        }
    }
}

