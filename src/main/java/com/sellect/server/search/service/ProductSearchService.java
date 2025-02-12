package com.sellect.server.search.service;


import com.sellect.server.product.domain.Product;

import java.util.List;

public interface ProductSearchService {

    List<Product> searchByKeyword(String keyword);
}
