package com.atguigu.gmall0311.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0311.bean.OrderDetail;
import com.atguigu.gmall0311.bean.OrderInfo;
import com.atguigu.gmall0311.bean.enums.ProcessStatus;
import com.atguigu.gmall0311.config.ActiveMQUtil;
import com.atguigu.gmall0311.config.RedisUtil;
import com.atguigu.gmall0311.order.mapper.OrderDetailMapper;
import com.atguigu.gmall0311.order.mapper.OrderInfoMapper;
import com.atguigu.gmall0311.service.OrderService;
import com.atguigu.gmall0311.util.HttpClientUtil;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;


import javax.jms.*;
import javax.jms.Queue;
import java.util.*;


@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Override
    public String saveOrder(OrderInfo orderInfo) {
        // 设置创建时间
        orderInfo.setCreateTime(new Date());
        // 设置失效时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // 生成第三方支付编号
        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfoMapper.insertSelective(orderInfo);

        //插入订单详细信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        // 为了跳转到支付页面使用。支付会根据订单id进行支付。
        String orderId = orderInfo.getId();
        return orderId;
    }

    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode = UUID.randomUUID().toString();
        jedis.setex(tradeNoKey,10*60,tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode  = jedis.get(tradeNoKey);

        if (tradeCode!=null && tradeCode.equals(tradeCodeNo)){
            return  true;
        }else{
            return false;
        }
    }

    @Override
    public void delTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey =  "user:"+userId+":tradeCode";
        jedis.del(tradeNoKey);
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)){
            return  true;
        }else {
            return  false;
        }
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        // 将orderDetai 放入orderInfo 中
        orderInfo.setOrderDetailList(orderDetailMapper.select(orderDetail));
        return orderInfo ;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus  processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = activeMQUtil.getConnection();
        String orderJson = initWareOrder(orderId);
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(order_result_queue);

            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText(orderJson);
            producer.send(textMessage);
            session.commit();
            session.close();
            producer.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }
    // 设置初始化仓库信息方法
    public Map  initWareOrder (OrderInfo orderInfo){
        Map<String,Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getTradeBody());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());

        // 组合json
        List detailList = new ArrayList();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map detailMap = new HashMap();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            detailList.add(detailMap);
        }
        map.put("details",detailList);
        return map;
    }
}
