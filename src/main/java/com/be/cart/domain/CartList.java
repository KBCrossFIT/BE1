package com.be.cart.domain;

import lombok.Data;

import java.util.List;

@Data
public class CartList {
    private List<CartItemVO> cartItems;
    private int userNum;
    private int totalCount;

}