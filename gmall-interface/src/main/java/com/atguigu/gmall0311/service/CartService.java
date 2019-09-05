package com.atguigu.gmall0311.service;

import com.atguigu.gmall0311.bean.CartInfo;

import java.util.List;

public interface CartService {
    void addToCart(String skuId, String userId, int skuNum);

    List<CartInfo> getCartList(String userId);

    List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId);

    void checkCart(String skuId, String isChecked, String userId);

    List<CartInfo> getCartCheckedList(String userId);
}
