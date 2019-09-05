package com.atguigu.gmall0311.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.CartInfo;
import com.atguigu.gmall0311.bean.SkuInfo;
import com.atguigu.gmall0311.cart.constant.CartConst;
import com.atguigu.gmall0311.cart.mapper.CartMapper;
import com.atguigu.gmall0311.config.RedisUtil;
import com.atguigu.gmall0311.service.CartService;
import com.atguigu.gmall0311.service.ManageService;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import tk.mybatis.mapper.util.StringUtil;

import java.time.chrono.IsoEra;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
public class CartServiceImpl implements CartService{

    @Autowired
    private CartMapper cartMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, int skuNum) {
        /**
         * 1.添加购物车先更新数据库在将其放到redis中
         * 2.先看购物车中是否该商品，有就跟新数量，没有就添加
         */
        //先看数据库中的cart中是否有该商品
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        CartInfo cartInfoExist = cartMapper.selectOne(cartInfo);
        if(cartInfoExist!=null){
            //更新数量
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //给实时价格赋值
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            //更新数据库信息
            cartMapper.updateByPrimaryKeySelective(cartInfoExist);
        }else {
            //购物车中没有
            SkuInfo skuInfo = manageService.getSkuInfoById(skuId);
            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            //放入数据库
            cartMapper.insertSelective(cartInfo1);
            cartInfoExist = cartInfo1;
        }
        //将其放入到redis中
        // 构建key user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();

        jedis.hset(userCartKey,skuId, JSON.toJSONString(cartInfoExist));
        //更新购物车过去时间（购物车的过期时间要与用户信息的过期时间一致）
        String userInfoKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey,ttl.intValue());
        jedis.close();

    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        //如果缓存中没有这从数据库中查找
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        List <String> cartJsonList = jedis.hvals(cartKey);
        List <CartInfo> cartInfoList = new ArrayList <>();
        if(cartJsonList!=null && cartJsonList.size()>0){
            for (String cartJson : cartJsonList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            // 排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return Long.compare(Long.parseLong(o2.getId()),Long.parseLong(o1.getId()));
                }
            });
        }else{
            // 从数据库中查询，其中cart_price 可能是旧值，所以需要关联sku_info 表信息。
            cartInfoList = loadCartCache(userId);

        }
        return cartInfoList;
    }

    /**
     *   合并购物车
     * @param cartListFromCookie
     * @param userId
     * @return
     */
    @Override
    public List <CartInfo> mergeToCartList(List <CartInfo> cartListFromCookie, String userId) {
        List <CartInfo> cartInfoList = cartInfoList = cartMapper.selectCartListWithCurPrice(userId) ;
        for (CartInfo cartInfo : cartListFromCookie) {

            boolean tag =false ;

            for (CartInfo info : cartInfoList) {
                if(cartInfo.getSkuId().equals(info.getSkuId())){
                    info.setSkuNum(info.getSkuNum()+cartInfo.getSkuNum());
                    info.setSkuPrice(info.getCartPrice());
                    cartMapper.updateByPrimaryKeySelective(info);
                    tag = true ;
                }
            }
            if(!tag){
                cartInfo.setUserId(userId);
                cartInfo.setSkuPrice(cartInfo.getCartPrice());
                cartInfoList.add(cartInfo);
                cartMapper.insertSelective(cartInfo);

            }
        }
        // 最后再查询一次更新之后，新添加的所有数据
        List<CartInfo> cartInfoListDB = loadCartCache(userId);
        for (CartInfo cartInfo : cartInfoListDB) {
            for (CartInfo info : cartListFromCookie) {
                if (cartInfo.getSkuId().equals(info.getSkuId())) {
                    // 只有被勾选的才会进行更改
                    if (info.getIsChecked().equals("1")) {
                        cartInfo.setIsChecked(info.getIsChecked());
                        // 更新redis中的isChecked
                        checkCart(cartInfo.getSkuId(), info.getIsChecked(), userId);
                    }
                }
            }
        }
        return cartInfoListDB;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        //跟新购物车中的标识isChecked
        //从缓存中获取到该商品
        Jedis jedis = redisUtil.getJedis();
        String usercartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = jedis.hget(usercartKey, skuId);
        //将其转换为对象
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        //更新标识
        cartInfo.setIsChecked(isChecked);
        String cartcheckdJsON = JSON.toJSONString(cartInfo);
        jedis.hset(usercartKey,skuId,cartcheckdJsON);
        //新增到已选中的购物车中
        String cartCheckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        if("1".equals(isChecked)){
            jedis.hset(cartCheckedKey,skuId,cartcheckdJsON);
        }else {
            jedis.hdel(cartCheckedKey,skuId);
        }

        jedis.close();
    }

    @Override
    public List <CartInfo> getCartCheckedList(String userId) {
        //获取redis中选中购物车列表的key
        String userCheckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        List <String> hvals = jedis.hvals(userCheckedKey);
        List<CartInfo> newCartList = new ArrayList<>();
        for (String cartInfoJson : hvals) {
            CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
            newCartList.add(cartInfo);
        }
        return newCartList;
    }

    private List <CartInfo> loadCartCache(String userId) {
        List<CartInfo> cartInfoList = cartMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList==null && cartInfoList.size()==0){
            return null;
        }
        //将其放到缓存中
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        //遍历集合将其转换为map
        HashMap <String, String> map = new HashMap <>();
        for (CartInfo cartInfo : cartInfoList) {
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        jedis.hmset(userCartKey,map);
        jedis.close();
        return cartInfoList;
    }
}
