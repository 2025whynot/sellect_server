package com.sellect.server.tmp.service;

import com.sellect.server.tmp.entity.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProductSearchService {

    List<Product> searchByKeyword(String keyword);
}
