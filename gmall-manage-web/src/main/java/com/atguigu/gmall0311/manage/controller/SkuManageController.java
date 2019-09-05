package com.atguigu.gmall0311.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0311.bean.SkuInfo;
import com.atguigu.gmall0311.bean.SkuLsInfo;
import com.atguigu.gmall0311.service.ListService;
import com.atguigu.gmall0311.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;
    //http://localhost:8082/saveSkuInfo
    @RequestMapping("saveSkuInfo")
    public String saveSkuInfo(@RequestBody SkuInfo skuInfo){
        if(skuInfo != null) {
            manageService.saveSkuInfo(skuInfo);
        }
        return "ok" ;
    }

    // http://localhost:8082/onSale?skuId=33
    @RequestMapping("onSale")
    public String onSale(String skuId){
        //创建保存数据的对象
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        //通过skuId获取skuInfo对象
        SkuInfo skuInfo = manageService.getSkuInfoById(skuId);
        //将数据拷贝给skuLsInfo
        BeanUtils.copyProperties(skuInfo,skuLsInfo);

        listService.saveSkuLsInfo(skuLsInfo);
        return "ok" ;
    }

}
