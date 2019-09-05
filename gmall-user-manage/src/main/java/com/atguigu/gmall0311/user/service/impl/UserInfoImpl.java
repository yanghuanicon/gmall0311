package com.atguigu.gmall0311.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.UserAddress;
import com.atguigu.gmall0311.bean.UserInfo;
import com.atguigu.gmall0311.config.RedisUtil;
import com.atguigu.gmall0311.service.UserInfoService;
import com.atguigu.gmall0311.user.mapper.UserAddressMapper;
import com.atguigu.gmall0311.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;


import java.util.List;

@Service
public class UserInfoImpl implements UserInfoService{

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;
    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public UserInfo getUserInfoByName(String name) {
        return null;
    }

    @Override
    public List <UserInfo> getUserInfoListByName(UserInfo userInfo) {
        return null;
    }

    @Override
    public List <UserInfo> getUserInfoListByNickName(UserInfo userInfo) {
        return null;
    }

    @Override
    public void addUser(UserInfo userInfo) {

    }

    @Override
    public void updUser(UserInfo userInfo) {

    }

    @Override
    public void delUser(UserInfo userInfo) {

    }

    @Override
    public List <UserAddress> getUserAddressByUserId(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return userAddressMapper.select(userAddress);
    }

    @Override
    public List <UserAddress> getUserAddressByUserId(UserAddress userAddress) {

        return userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //将密码进行加密处理(数据库中的密码是密文)
        String password = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(password);
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if(info != null){
            //获得redis将用户数据存到redis中
            Jedis jedis = redisUtil.getJedis();
            jedis.setex(userKey_prefix+info.getId()+userinfoKey_suffix,userKey_timeOut, JSON.toJSONString(info));
            jedis.close();
            return info ;
        }
        return null;
    }

    /**
     * 查询redis看里面是否有用户信息
     * @param userId
     * @return
     */
    @Override
    public UserInfo verify(String userId) {
        //查询缓存看是否有对应用户信息
        Jedis jedis = redisUtil.getJedis();
        String key = userKey_prefix+userId+userinfoKey_suffix;
        String userInfoJson = jedis.get(key);
        // 延长时效
        jedis.expire(key,userKey_timeOut);
        if(userInfoJson!=null && userInfoJson.length()>0){
            UserInfo info = JSON.parseObject(userInfoJson, UserInfo.class);
            return info;
        }
        return null;
    }
}
