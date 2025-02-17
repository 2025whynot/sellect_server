package com.sellect.server.search.application;


import com.sellect.server.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductSearchService {

    Page<Product> searchByKeyword(String keyword, Pageable pageable);

}
