package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.ShoppingCartMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public interface ShoppingCartService {

    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> listShoppingCart();

    void cleanShoppingCart();

    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
