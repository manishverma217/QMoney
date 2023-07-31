package main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dto.*;
import log.UncaughtExceptionHandler;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;
import portfolio.PortfolioManager;
import portfolio.PortfolioManagerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

public class PortfolioManagerApplication {

    public static final String TOKEN = "8dc5b141f8ac2f63fd979620fe8c8550938eb0e0";
    public static RestTemplate restTemplate = new RestTemplate();
    public static PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager("tiingo", restTemplate);

    public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
        File file = resolveFileFromResources(args[0]);
        ObjectMapper om = getObjectMapper();
        PortfolioTrade[] trade = om.readValue(file, PortfolioTrade[].class);
        List<String> list = new ArrayList<String>();
        for(PortfolioTrade obj: trade){
            list.add(obj.getSymbol());
        }
        return list;
    }

    private static void printJsonObject(Object object) throws IOException {
        Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
        ObjectMapper mapper = new ObjectMapper();
        logger.info(mapper.writeValueAsString(object));
    }

    private static File resolveFileFromResources(String filename) throws URISyntaxException {
        return Paths.get(
                Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    public static List<String> debugOutputs() {

        String valueOfArgument0 = "trades.json";
        String resultOfResolveFilePathArgs0 = "trades.json";
        String toStringOfObjectMapper = "ObjectMapper";
        String functionNameFromTestFileInStackTrace = "mainReadFile";
        String lineNumberFromTestFileInStackTrace = "";


        return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
                toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
                lineNumberFromTestFileInStackTrace});
    }
    // Note:
    // Remember to confirm that you are getting same results for annualized returns as in Module 3.
    public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
        ObjectMapper objectMapper = getObjectMapper();
        List<PortfolioTrade> trades = Arrays.asList(objectMapper.readValue(resolveFileFromResources(args[0]), PortfolioTrade[].class));
        List<TotalReturnsDto> sortedByValue = mainReadQuotesHelper(args, trades);
        Collections.sort(sortedByValue , TotalReturnsDto.closingComparator);
        List<String> stocks = new ArrayList<>();
        for(TotalReturnsDto trade : sortedByValue){
            stocks.add(trade.getSymbol());
        }
        return stocks;
    }

    public static List<PortfolioTrade> readTradesFromJson(String filename)
            throws IOException, URISyntaxException {
        File file = resolveFileFromResources(filename);
        ObjectMapper om = getObjectMapper();
        PortfolioTrade[] trade = om.readValue(file, PortfolioTrade[].class);
        List<PortfolioTrade> list = new ArrayList<>();
        for(PortfolioTrade obj: trade){
            list.add(obj);
        }
        return list;
    }

    public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {

        StringBuilder sb = new StringBuilder();
        sb.append("https://api.tiingo.com/tiingo/daily/");
        sb.append(trade.getSymbol());
        sb.append("/prices?");
        sb.append("startDate=" + trade.getPurchaseDate() + "&endDate=" + endDate +
                "&token=" + token);
        return sb.toString();

    }

    public static List<TotalReturnsDto> mainReadQuotesHelper(String[] args ,
                                                             List<PortfolioTrade> trades){
        RestTemplate restTemplate = new RestTemplate();
        List<TotalReturnsDto> tests = new ArrayList<>();
        for(PortfolioTrade trade : trades){
            String uri = "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() +
                    "/prices?startDate=" + trade.getPurchaseDate().toString() +
                    "&endDate=" + args[1] + "&token=" + TOKEN;
            TiingoCandle[] results = restTemplate.getForObject(uri, TiingoCandle[].class);
            if(results != null){
                tests.add(new TotalReturnsDto(trade.getSymbol(), results[results.length - 1].getClose()));
            }
        }
        return tests;
    }
    public static Double getOpeningPriceOnStartDate(List<Candle> candles) {
        Candle candle = candles.get(0);
        if(candle != null){
            return candle.getOpen();
        }
        return 0.0;
    }

    public static Double getClosingPriceOnEndDate(List<Candle> candles) {
        Candle candle = candles.get(candles.size() - 1);
        if(candle != null){
            return candle.getClose();
        }
        return 0.0;
    }


    public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
        List<Candle> candleList = new ArrayList<>();
        String url = prepareUrl(trade, endDate, token);
        RestTemplate restTemplate = new RestTemplate();
        TiingoCandle[] stocksStartToEndDate = restTemplate.getForObject(url, TiingoCandle[].class);
        for(TiingoCandle candle : stocksStartToEndDate){
            candleList.add(candle);
        }
        return candleList;
    }

    public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
            throws IOException, URISyntaxException {
        List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
        LocalDate endLocalDate = LocalDate.parse(args[1]);

        File trades = resolveFileFromResources(args[0]);
        ObjectMapper objectMapper = getObjectMapper();

        PortfolioTrade[] tradeJsons = objectMapper.readValue(trades, PortfolioTrade[].class);
        for(int i = 0 ; i < tradeJsons.length ; i++){
            annualizedReturns.add(getAnnualizedReturn(tradeJsons[i] , endLocalDate));
        }
        //Sort in Descending Order
        Comparator<AnnualizedReturn> sortByAnnReturn = Comparator.comparing(AnnualizedReturn::getAnnualizedReturn)
                .reversed();
        Collections.sort(annualizedReturns , sortByAnnReturn);
        return annualizedReturns;
    }

    public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
                                                              PortfolioTrade trade, Double buyPrice, Double sellPrice) {
        double totalReturn = (sellPrice - buyPrice) / buyPrice;
        double numYears = ChronoUnit.DAYS.between(trade.getPurchaseDate() , endDate) / 365.24;
        double annualizedReturns = Math.pow((1 + totalReturn),(1 / numYears)) - 1;
        return new AnnualizedReturn(trade.getSymbol() , annualizedReturns , totalReturn);
    }
    public static AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade , LocalDate endLocalDate){
        String ticker = trade.getSymbol();
        LocalDate startLocalDate = trade.getPurchaseDate();
        if(startLocalDate.compareTo(endLocalDate) >= 0){
            throw new RuntimeException();
        }

        //Create URL Object for the API Call
        //TOKEN a class variable
        String url = String.format("https://api.tiingo.com/tiingo/daily/%s/prices?" +
                        "startDate=%s&endDate=%s&token=%s" ,
                ticker , startLocalDate , endLocalDate , TOKEN);

        RestTemplate restTemplate = new RestTemplate();

        //API returns a list of results for each day's closing details
        TiingoCandle[] stocksStartToEndDate = restTemplate.getForObject(url, TiingoCandle[].class);

        //Extract stocks for Start Date and End Date
        if(stocksStartToEndDate != null){
            TiingoCandle stockStartDate = stocksStartToEndDate[0];
            TiingoCandle stockLatest = stocksStartToEndDate[stocksStartToEndDate.length - 1];

            Double buyPrice = stockStartDate.getOpen();
            Double sellPrice = stockLatest.getClose();

            AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(endLocalDate, trade, buyPrice, sellPrice);
            return annualizedReturn;
        }
        return new AnnualizedReturn("symbol", Double.NaN, Double.NaN);
    }

    public static String getToken(){
        return TOKEN;
    }

    public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
            throws Exception {
        File file = resolveFileFromResources(args[0]);
        LocalDate endDate = LocalDate.parse(args[1]);
        ObjectMapper objectMapper = getObjectMapper();
        PortfolioTrade[] portfolioTrades = objectMapper.readValue(file, PortfolioTrade[].class);
        return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
    }

    public static List<AnnualizedReturn> mainCalculateReturnsAfterConcurrency(String[] args)
            throws Exception{
        int numThreads = Integer.valueOf(args[2]);
        File file = resolveFileFromResources(args[0]);
        LocalDate endDate = LocalDate.parse(args[1]);
        ObjectMapper objectMapper = getObjectMapper();
        PortfolioTrade[] portfolioTrades = objectMapper.readValue(file, PortfolioTrade[].class);
        return portfolioManager.calculateAnnualizedReturnParallel(Arrays.asList(portfolioTrades), endDate, numThreads);
    }

    public static void main(String[] args) throws Exception {

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
        ThreadContext.put("runId", UUID.randomUUID().toString());

        printJsonObject(mainReadFile(args));

        printJsonObject(mainReadQuotes(args));

        printJsonObject(mainCalculateSingleReturn(args));

        printJsonObject(mainCalculateReturnsAfterRefactor(args));

        printJsonObject(mainCalculateReturnsAfterConcurrency(args));

    }
}
