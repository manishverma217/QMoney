package portfolio;

import com.fasterxml.jackson.core.JsonProcessingException;
import dto.AnnualizedReturn;
import dto.Candle;
import dto.PortfolioTrade;
import exception.StockQuoteServiceException;
import org.springframework.web.client.RestTemplate;
import quotes.StockQuotesService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

public class PortfolioManagerImpl implements PortfolioManager {
    public static final String TOKEN = "8dc5b141f8ac2f63fd979620fe8c8550938eb0e0";

    private StockQuotesService stockQuotesService;

    PortfolioManagerImpl(StockQuotesService stockQuotesService2){
        this.stockQuotesService = stockQuotesService2;
    }
    RestTemplate restTemplate = null;

    protected PortfolioManagerImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private Comparator<AnnualizedReturn> getComparator() {
        return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
    }

    public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
            throws JsonProcessingException , StockQuoteServiceException {
        return stockQuotesService.getStockQuote(symbol, from, to);
    }

    protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
        String token = getToken();
        String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
                + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
        String url = uriTemplate.replace("$SYMBOL" , symbol).replace("$STARTDATE" , startDate.toString())
                .replace("$ENDDATE" , endDate.toString()).replace("$APIKEY", token);
        return url;

    }

    @Override
    public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> portfolioTrades,
                                                                    LocalDate endDate , int numThreads) throws StockQuoteServiceException, InterruptedException{
        List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
        List<Future<AnnualizedReturn>> futureReturnsList = new ArrayList<Future<AnnualizedReturn>>();
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        for(int i = 0 ; i < portfolioTrades.size() ; i++){
            PortfolioTrade trade = portfolioTrades.get(i);
            Callable<AnnualizedReturn> callableTask = () -> {
                return getAnnualizedReturn(trade , endDate);
            };
            Future<AnnualizedReturn> futureReturns = pool.submit(callableTask);
            futureReturnsList.add(futureReturns);
        }
        for(int i = 0 ; i < portfolioTrades.size() ; i++){
            Future<AnnualizedReturn> futureReturns = futureReturnsList.get(i);
            try{
                AnnualizedReturn annualizedReturn  = futureReturns.get();
                annualizedReturns.add(annualizedReturn);
            } catch(ExecutionException e){
                throw new StockQuoteServiceException("Error when calling the API", e);
            }
        }
        Comparator<AnnualizedReturn> SortByAnnReturn = Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
        Collections.sort(annualizedReturns, SortByAnnReturn);
        return annualizedReturns;
    }

    @Override
    public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
                                                            LocalDate endDate) throws StockQuoteServiceException {
        AnnualizedReturn annualizedReturn;
        List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
        int size = portfolioTrades.size();
        for(int i = 0 ; i < size ; i++){
            annualizedReturn = getAnnualizedReturn(portfolioTrades.get(i) , endDate);
            annualizedReturns.add(annualizedReturn);
        }
        Comparator<AnnualizedReturn> SortByAnnReturn = Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
        Collections.sort(annualizedReturns , SortByAnnReturn);
        return annualizedReturns;
    }

    public AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade , LocalDate endLocalDate)
            throws StockQuoteServiceException{
        AnnualizedReturn annualizedReturn;
        String symbol = trade.getSymbol();
        LocalDate startLocalDate = trade.getPurchaseDate();
        try{
            // Fetch Data
            List<Candle> stocksStartToEndDate;
            stocksStartToEndDate = getStockQuote(symbol, startLocalDate, endLocalDate);

            //Extract stocks for StartDate and EndDate
            Candle stockStartDate = stocksStartToEndDate.get(0);
            Candle stockLatest = stocksStartToEndDate.get(stocksStartToEndDate.size() - 1);

            Double buyPrice = stockStartDate.getOpen();
            Double sellPrice = stockLatest.getClose();

            //Calculate Total Returns
            Double totalReturn = (sellPrice - buyPrice) / buyPrice;

            //Calculate Years
            Double numYears = (double) ChronoUnit.DAYS.between(startLocalDate, endLocalDate) / 365;

            //Calculate Annualized Return using formula
            Double annualizedReturns = Math.pow((1 + totalReturn) , (1 / numYears)) - 1;

            annualizedReturn = new AnnualizedReturn(symbol, annualizedReturns, totalReturn);
        } catch (JsonProcessingException e){
            annualizedReturn = new AnnualizedReturn(symbol, Double.NaN, Double.NaN);
        }
        return annualizedReturn;
    }

    public static String getToken(){
        return TOKEN;
    }
}
