package com.atguigu.gmall0311.service;

import com.atguigu.gmall0311.bean.*;
import tk.mybatis.mapper.common.base.BaseInsertMapper;

import java.util.List;

public interface ManageService {

    /**
     * 查询一级分类
     * @return
     */
    List<BaseCatalog1> getCatalog1();

    /**
     * 查询二级分类
     * @return
     */
    List<BaseCatalog2> getCatalog2(String catalog1Id);
    /**
     * 查询三级分类
     * @return
     */
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    /**
     * 查询平台属性
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(String catalog3Id);

    /**
     * 添加平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 查询平台values
     * @param attrId
     * @return
     */
    List<BaseAttrValue> getAttrValueList(String attrId);

    BaseAttrInfo getAttrInfo(String attrId);

    /**
     *
     * @return
     */
    List<SpuInfo> getSpuList();

    /**
     * 添加spu
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 显示销售属性
     * @return
     */
    List<BaseSaleAttr> getSpuSaleAttrList();

    /**
     * 显示销售属性以及销售属性值
     * @return
     */
    List<SpuSaleAttr> spuSaleAttrList(String spuId);

    /**
     * 查询spu图片
     * @return
     */
    List<SpuImage> getSpuImageList(SpuImage spuImage);

    /**
     * 保存sku
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    SkuInfo getSkuInfoById(String skuId);

    /**
     * 获取销售属性
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    /**
     * 根据attrValueId 查询attrList
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
