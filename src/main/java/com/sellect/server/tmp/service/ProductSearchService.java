package com.sellect.server.tmp.service;

import com.sellect.server.tmp.entity.Product;

import java.util.List;

public interface ProductSearchService {

    List<Product> searchByKeyword(String keyword);
}
