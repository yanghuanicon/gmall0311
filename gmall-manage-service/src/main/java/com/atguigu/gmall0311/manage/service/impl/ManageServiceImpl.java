package com.atguigu.gmall0311.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.*;
import com.atguigu.gmall0311.config.RedisUtil;
import com.atguigu.gmall0311.manage.constant.ManageConst;
import com.atguigu.gmall0311.manage.mapper.*;
import com.atguigu.gmall0311.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.awt.event.ItemEvent;
import java.util.List;

@Service
public class ManageServiceImpl implements ManageService{
    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List <BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List <BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List <BaseAttrInfo> getAttrInfoList(String catalog3Id) {
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//        baseAttrInfo.setCatalog3Id(catalog3Id);
//        return baseAttrInfoMapper.select(baseAttrInfo);
          return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        String id = baseAttrInfo.getId();
        if(id != null){
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        } else {
            //添加平台属性key
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
            BaseAttrValue baseAttrValueDel = new BaseAttrValue();
            baseAttrValueDel.setAttrId(baseAttrInfo.getId());
            baseAttrValueMapper.delete(baseAttrValueDel);
            //添加平台属性value
            List <BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

            if(attrValueList !=null && attrValueList.size()>0){
                for (BaseAttrValue baseAttrValue: attrValueList) {
                    baseAttrValue.setAttrId(baseAttrInfo.getId());
                    baseAttrValueMapper.insertSelective(baseAttrValue);
                }
            }


    }

    @Override
    public List <BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        return baseAttrValueMapper.select(baseAttrValue);
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public List <SpuInfo> getSpuList() {
        return spuInfoMapper.selectAll();
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        //判断是更新还是添加
        if(spuInfo.getId() == null || spuInfo.getId().length() == 0) {
            //添加
            spuInfoMapper.insertSelective(spuInfo);
        } else {
            //修改
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        }

        //添加图片（先删除在添加）
        SpuImage spuImageDel = new SpuImage();
        spuImageDel.setSpuId(spuInfo.getId());
        spuImageMapper.delete(spuImageDel);
        List <SpuImage> spuImageList = spuInfo.getSpuImageList();
        for (SpuImage spuImage: spuImageList) {
            spuImage.setSpuId(spuInfo.getId());
            spuImageMapper.insertSelective(spuImage);
        }
        //添加销售属性（先删除在添加）
        SpuSaleAttr spuSaleAttrDel = new SpuSaleAttr();
        spuSaleAttrDel.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttrDel);

        List <SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        for(SpuSaleAttr spuSaleAttr : spuSaleAttrList){
            spuSaleAttr.setSpuId(spuInfo.getId());
            spuSaleAttrMapper.insertSelective(spuSaleAttr);

            //添加销售属性值
            List <SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();

            if(spuSaleAttrValueList != null && spuSaleAttrList.size()>0){
                //循环遍历进行添加
                for(SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList){
                    spuSaleAttrValue.setSpuId(spuInfo.getId());
                    spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                }
            }
        }
    }

    @Override
    public List <BaseSaleAttr> getSpuSaleAttrList() {
        return baseSaleAttrMapper.selectAll();

    }

    @Override
    public List <SpuSaleAttr> spuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    public List <SpuImage> getSpuImageList(SpuImage spuImage) {

        return spuImageMapper.select(spuImage);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        /*if(skuInfo.getId() == null ||skuInfo.getId().length()==0){
            //设置id自增
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        } else {
            //修改
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }
        //添加image
        SkuImage skuImageDel = new SkuImage();
        skuImageDel.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImageDel);
        List <SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(skuImageList != null && skuImageList.size()>0){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
        //添加sku_attr_value
        SkuAttrValue skuAttrValueDel = new SkuAttrValue();
        skuAttrValueDel.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValueDel);
        List <SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if(skuAttrValueList != null && skuAttrValueList.size()>0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
        //添加sku_sale_attr_value,
        SkuSaleAttrValue skuSaleAttrValueDel = new SkuSaleAttrValue();
        skuSaleAttrValueDel.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValueDel);
        List <SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if(skuSaleAttrValueList != null && skuSaleAttrValueList.size()>0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList){
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }*/

        //保存到sku库存单元表
        skuInfoMapper.insertSelective(skuInfo);
//保存到sku图片表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() > 0){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }
//保存到销售属性值表
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
//保存到平台属性值关联表 {能够通过平台属性值对sku进行筛选}
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size() > 0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }

    }

    @Override
    public SkuInfo getSkuInfoById(String skuId) {
        SkuInfo skuInfo = null;
        //先去缓冲查
        Jedis jedis = null;
        //定义redis的key
        String skuInfoKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
        try {
            String skuLockKey=ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKULOCK_SUFFIX;
            jedis =redisUtil.getJedis();
            //有就取出数据
            String skuInfoJson = jedis.get(skuInfoKey);
            if(skuInfoJson==null || skuInfoJson.length()==0){
                String lockKey  = jedis.set(skuLockKey, "OK", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                if ("OK".equals(lockKey)){
                    System.out.println("获取锁！");
                    // 从数据库中取得数据
                    skuInfo = getSkuInfoDB(skuId);
                    // 将是数据放入缓存
                    // 将对象转换成字符串
                    String skuRedisStr = JSON.toJSONString(skuInfo);
                    jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,skuRedisStr);
                    jedis.del(skuLockKey);
                    return skuInfo;
                }else {
                    System.out.println("等待！");
                    // 等待
                    Thread.sleep(1000);
                    // 自旋
                    return getSkuInfoById(skuId);
                }
            }else {
                // 有数据
                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                return skuInfo;
            }

        } catch (Exception e) {
            skuInfo = getSkuInfoDB(skuId);
            e.printStackTrace();

        } finally {
            if(jedis!=null){
                jedis.close();
            }
            return skuInfo ;
        }

    }

    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);

        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImages = skuImageMapper.select(skuImage);
        //将skuAttrValue封装到skuInfo中
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List <SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValues);
        if(skuInfo!=null){
            skuInfo.setSkuImageList(skuImages);
        }
        return skuInfo;
    }

    @Override
    public List <SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());
    }

    @Override
    public List <SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpuId(spuId);
}

    @Override
    public List <BaseAttrInfo> getAttrList(List <String> attrValueIdList) {

        String valueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        List<BaseAttrInfo> baseAttrInfoList=baseAttrInfoMapper.selectAttrInfoListByIds(valueIds);
        return baseAttrInfoList;
    }


}
