package com.sellect.server.product.repository;

import com.sellect.server.product.domain.Product;
import java.util.List;

public interface ProductRepository {

    List<Product> saveAll(List<Product> products);
}
