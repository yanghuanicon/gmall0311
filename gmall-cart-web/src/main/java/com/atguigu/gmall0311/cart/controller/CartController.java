package com.atguigu.gmall0311.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0311.bean.CartInfo;
import com.atguigu.gmall0311.bean.SkuInfo;
import com.atguigu.gmall0311.config.LoginRequire;
import com.atguigu.gmall0311.service.CartService;
import com.atguigu.gmall0311.service.ManageService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response) {
        //获取userId skuId skuNum
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        String userId = (String) request.getAttribute("userId");
        System.out.println("userId:" + skuId + "skuNum:" + skuNum + "userId:" + userId);
        //判断用户是否登陆
        if (userId != null && userId.length() > 0) {
            //用户以登陆
            cartService.addToCart(skuId, userId, Integer.parseInt(skuNum));
        } else {
            //用户未登陆
            cartCookieHandler.addToCart(request, response, skuId, userId, Integer.parseInt(skuNum));
        }
        //获取添加商品信息进行回显
        SkuInfo skuInfo = manageService.getSkuInfoById(skuId);
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuInfo", skuInfo);
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response) {
        // 判断用户是否登录，登录了从redis中，redis中没有，从数据库中取
        // 没有登录，从cookie中取得
        String userId = (String) request.getAttribute("userId");
        if (userId != null) {
            // 从cookie中查找购物车
            List <CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
            List <CartInfo> cartList = null;
            if (cartListFromCookie != null && cartListFromCookie.size() > 0) {
                // 开始合并
                cartList = cartService.mergeToCartList(cartListFromCookie, userId);
                // 删除cookie中的购物车
                cartCookieHandler.deleteCartCookie(request, response);
            } else {
                // 从redis中取得，或者从数据库中
                cartList = cartService.getCartList(userId);
            }
            request.setAttribute("cartList", cartList);
        } else {
            List <CartInfo> cartList = cartCookieHandler.getCartList(request);
            request.setAttribute("cartList", cartList);
            return "cartList";
        }
        return "cartList";
    }
    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request ,HttpServletResponse response){
        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        String userId = (String) request.getAttribute("userId");
        if (userId!=null){
            cartService.checkCart(skuId,isChecked,userId);
        }else{
           cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }


    }
    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");
        List <CartInfo> cookieHandlerCartList  = cartCookieHandler.getCartList(request);
        if(cookieHandlerCartList!=null && cookieHandlerCartList.size()>0){
            cartService.mergeToCartList(cookieHandlerCartList, userId);
            cartCookieHandler.deleteCartCookie(request,response);
        }
        return "redirect://order.gmall.com/trade";
    }

}