package com.sellect.server.product.application;

import com.sellect.server.product.repository.StockHistoryEntity;
import com.sellect.server.product.repository.StockHistoryJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockHistoryService {

    private final StockHistoryJpaRepository stockHistoryJpaRepository;

    public void saveStockHistory(StockHistoryEntity stockHistoryEntity) {
        stockHistoryJpaRepository.save(stockHistoryEntity);
    }

    public void saveAllStockHistory(List<StockHistoryEntity> stockHistoryEntities) {
        stockHistoryJpaRepository.saveAll(stockHistoryEntities);
    }

}
