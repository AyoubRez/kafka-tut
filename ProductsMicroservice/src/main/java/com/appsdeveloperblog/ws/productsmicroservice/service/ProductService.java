package com.appsdeveloperblog.ws.productsmicroservice.service;

import com.appsdeveloperblog.ws.productsmicroservice.rest.Product;

import java.util.concurrent.ExecutionException;

public interface ProductService  {
    String createProduct(Product product) throws Exception;
}
