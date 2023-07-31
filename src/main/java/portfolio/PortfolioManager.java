package portfolio;

import dto.AnnualizedReturn;
import dto.PortfolioTrade;
import exception.StockQuoteServiceException;

import java.time.LocalDate;
import java.util.List;

public interface PortfolioManager {

    List<AnnualizedReturn> calculateAnnualizedReturnParallel(
            List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
            throws InterruptedException, StockQuoteServiceException;

    List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
                                                     LocalDate endDate)
            throws StockQuoteServiceException;
}

