package com.atguigu.gmall0311.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.SkuInfo;
import com.atguigu.gmall0311.bean.SkuSaleAttrValue;
import com.atguigu.gmall0311.bean.SpuSaleAttr;
import com.atguigu.gmall0311.config.LoginRequire;
import com.atguigu.gmall0311.service.ListService;
import com.atguigu.gmall0311.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;
    //@LoginRequire(autoRedirect = true) // 表示访问商品详情的时候，必须要登录！
    @RequestMapping("{skuId}.html")
    public String skuInfoPage(@PathVariable String skuId, Map<String ,Object> map){


        SkuInfo skuInfo = manageService.getSkuInfoById(skuId);
        map.put("skuInfo",skuInfo);
        //查询销售属性结果集
        List<SpuSaleAttr> spuSaleAttrList=manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        map.put("spuSaleAttrList",spuSaleAttrList);
        //获取销售属性组成的skuid集合
        List<SkuSaleAttrValue> skuSaleAttrValueList=manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        String key = "";
        HashMap <String, String> map1 = new HashMap <>();
        // 拼接规则：skuId 与 下一个skuId 不相等的时候，不拼接！ 当拼接到集合末尾则不拼接
        for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
            // 第一次拼接： key=118
            // 第二次拼接： key=118|
            // 第三次拼接： key=118|120
            // 第四次拼接： key=""
            // 什么时候拼接|
            if(key.length()>0){
                key+="|" ;
            }
            key+=skuSaleAttrValue.getSaleAttrValueId();
            if((i+1) == skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())){
                //将其放入到map集合中
                map1.put(key,skuSaleAttrValue.getSkuId());
                //清空key
                key = "";
            }
        }
        //将map1转换成json字符串
        String valuesSkuJson = JSON.toJSONString(map1);
        map.put("valuesSkuJson",valuesSkuJson);

        //更新热度
        listService.incrHotScore(skuId);

        return "item" ;
    }
}
