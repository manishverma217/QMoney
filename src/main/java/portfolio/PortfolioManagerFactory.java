package portfolio;

import org.springframework.web.client.RestTemplate;
import quotes.StockQuoteServiceFactory;
import quotes.StockQuotesService;

public class PortfolioManagerFactory {

    public static PortfolioManager getPortfolioManager(RestTemplate restTemplate) {

        return new PortfolioManagerImpl(restTemplate);
    }
    public static PortfolioManager getPortfolioManager(String provider,
                                                       RestTemplate restTemplate) {
        StockQuotesService stockQuotesService = StockQuoteServiceFactory.INSTANCE.getService(provider , restTemplate);
        return new PortfolioManagerImpl(stockQuotesService);
    }

}
