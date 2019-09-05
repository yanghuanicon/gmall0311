package com.atguigu.gmall0311.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.BaseAttrInfo;
import com.atguigu.gmall0311.bean.BaseAttrValue;
import com.atguigu.gmall0311.bean.SkuLsParams;
import com.atguigu.gmall0311.bean.SkuLsResult;
import com.atguigu.gmall0311.service.ListService;
import com.atguigu.gmall0311.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import sun.awt.SunHints;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {
    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    // http://list.gmall.com/list.html?catalog3Id=61

    @RequestMapping("list.html")
    public String getSearch(SkuLsParams skuLsParams,Model model){
        // 设置每页显示的条数
        skuLsParams.setPageSize(2);
        SkuLsResult skuLsResult = listService.search(skuLsParams);

        model.addAttribute("skuLsInfoList",skuLsResult.getSkuLsInfoList());
        //显示平台属性
        List <String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> attrList=manageService.getAttrList(attrValueIdList);
        model.addAttribute("attrList",attrList);
        //拼接url中的参数
        // 已选的属性值列表\
        String urlParam = makeUrlParam(skuLsParams);
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("keyword",skuLsParams.getKeyword());
        //itco

        // 声明一个面包屑集合
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();
        for (Iterator <BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo = iterator.next();
            List <BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0){
                    for (String valueId : skuLsParams.getValueId()) {
                        if(valueId.equals(baseAttrValue.getId())){
                            iterator.remove();
                            // 构造面包屑列表
                            BaseAttrValue baseAttrValueSelected = new BaseAttrValue();
                            baseAttrValueSelected.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            //去除重复数据
                            String makeUrlParam = makeUrlParam(skuLsParams, valueId);
                            baseAttrValueSelected.setUrlParam(makeUrlParam);
                            baseAttrValueArrayList.add(baseAttrValueSelected);
                        }

                    }
                }
            }
        }
        model.addAttribute("totalPages", skuLsResult.getTotalPages());
        model.addAttribute("pageNo",skuLsParams.getPageNo());
        // 保存面包屑
        model.addAttribute("baseAttrValueArrayList",baseAttrValueArrayList);
        return "list" ;
    }

    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String urlParam = "";
        if(skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            urlParam+="keyword="+skuLsParams.getKeyword();
        }
        if(skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            if(urlParam!=null && urlParam.length()>0){
                urlParam+="&" ;
            }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        //平台属性值
        if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length>0){
            for (String attrValueId : skuLsParams.getValueId()) {
                if(urlParam!=null && urlParam.length()>0){
                    urlParam+="&";
                }
                if(excludeValueIds!=null && excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if(excludeValueId.equals(attrValueId)){
                        //剔除这个attrValueId
                        continue;
                    }
                }

                urlParam+="valueId="+ attrValueId;
            }
        }
        return urlParam;
    }
}
