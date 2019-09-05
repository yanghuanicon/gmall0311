package com.atguigu.gmall0311.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.CartInfo;
import com.atguigu.gmall0311.bean.SkuInfo;
import com.atguigu.gmall0311.config.CookieUtil;
import com.atguigu.gmall0311.service.ManageService;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.templateresolver.TTLTemplateResolutionValidity;
import tk.mybatis.mapper.util.StringUtil;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {
    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;

    @Reference
    private ManageService manageService;

    /**
     *
     * @param request
     * @param response
     * @param skuId 要添加商品的id
     * @param userId 用户的id
     * @param skuNum
     */
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {

        //判断cookie中是否有购物车
        String cartJson = CookieUtil.getCookieValue(request, cookieCartName, true);
        List<CartInfo> cartInfoList = new ArrayList<>();
        boolean ifExist = false;
        if(StringUtil.isNotEmpty(cartJson)){
            //将其转换为集合
            cartInfoList = JSON.parseArray(cartJson,CartInfo.class);
            //遍历集合和添加商品的id进行比较
            for (CartInfo cartInfo : cartInfoList) {
                if(cartInfo.getSkuId().equals(skuId)){
                    //修改购物车中商品的数量
                    cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                    //更新实时价格
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());
                    ifExist = true ;

                }
            }
        }
        if(!ifExist){
            //cookie中没有购物车，或者购物车中没有该商品
            //在数据库中查询该商品的信息
            SkuInfo skuInfo = manageService.getSkuInfoById(skuId);

            CartInfo cartInfo = new CartInfo();

            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            //将其放到集合中
            cartInfoList.add(cartInfo);
        }
        //将购物车写入到cookie中
        String cartJson1 = JSON.toJSONString(cartInfoList);
        CookieUtil.setCookie(request,response,cookieCartName,cartJson1,COOKIE_CART_MAXAGE,true);
    }

    /**
     * 从cookie中获取cart
     * @param request
     * @return
     */
    public List <CartInfo> getCartList(HttpServletRequest request) {
        String cartjsonList = CookieUtil.getCookieValue(request, cookieCartName, true);
        List <CartInfo> cartInfoList = JSON.parseArray(cartjsonList, CartInfo.class);
        return cartInfoList;
    }

    /**
     * 删除cookie中的购物车
     * @param request
     * @param response
     */
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        //取出购物车中的商品
        List <CartInfo> cartList = getCartList(request);
        //循环比较
        for (CartInfo cartInfo : cartList) {
            if(cartInfo.getSkuId().equals(skuId)){
                cartInfo.setIsChecked(isChecked);
            }
        }
        //更新完成之后保存到cookie
        String cartListJson = JSON.toJSONString(cartList);
        CookieUtil.setCookie(request,response,cookieCartName,cartListJson,COOKIE_CART_MAXAGE,true);
    }
}
