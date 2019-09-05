package com.atguigu.gmall0311.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall0311.bean.OrderInfo;
import com.atguigu.gmall0311.bean.PaymentInfo;
import com.atguigu.gmall0311.config.ActiveMQUtil;
import com.atguigu.gmall0311.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall0311.service.OrderService;
import com.atguigu.gmall0311.service.PaymentInfoService;
import com.atguigu.gmall0311.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.Connection;

import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import java.util.HashMap;
import java.util.Map;


@Service
public class PaymentInfoServiceImpl implements PaymentInfoService {

    @Reference
    private OrderService orderService;
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {

        return   paymentInfoMapper.selectOne(paymentInfo);
    }

    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfoUpd,example);
    }

    @Override
    public boolean refund(String orderId) {
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        // 根据orderId 查询OrderInfo
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        // 设置map 封装参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("refund_amount",orderInfo.getTotalAmount());
        map.put("refund_reason","不够绿");
        map.put("out_request_no","HZ01RF001");
        request.setBizContent(JSON.toJSONString(map));
//        request.setBizContent("{" +
//                "\"out_trade_no\":\"20150320010101001\"," +
//                "\"trade_no\":\"2014112611001004680073956707\"," +
//                "\"refund_amount\":200.12," +
//                "\"refund_currency\":\"USD\"," +
//                "\"refund_reason\":\"正常退款\"," +
//                "\"out_request_no\":\"HZ01RF001\"," +
//                "\"operator_id\":\"OP001\"," +
//                "\"store_id\":\"NJ_S_001\"," +
//                "\"terminal_id\":\"NJ_T_001\"," +
//                "      \"goods_detail\":[{" +
//                "        \"goods_id\":\"apple-01\"," +
//                "\"alipay_goods_id\":\"20010001\"," +
//                "\"goods_name\":\"ipad\"," +
//                "\"quantity\":1," +
//                "\"price\":2000," +
//                "\"goods_category\":\"34543238\"," +
//                "\"categories_tree\":\"124868003|126232002|126252004\"," +
//                "\"body\":\"特价手机\"," +
//                "\"show_url\":\"http://www.alipay.com/xxx.jpg\"" +
//                "        }]," +
//                "      \"refund_royalty_parameters\":[{" +
//                "        \"royalty_type\":\"transfer\"," +
//                "\"trans_out\":\"2088101126765726\"," +
//                "\"trans_out_type\":\"userId\"," +
//                "\"trans_in_type\":\"userId\"," +
//                "\"trans_in\":\"2088101126708402\"," +
//                "\"amount\":0.1," +
//                "\"amount_percentage\":100," +
//                "\"desc\":\"分账给2088101126708402\"" +
//                "        }]," +
//                "\"org_pid\":\"2088101117952222\"" +
//                "  }");
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){

            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }

    @Override
    public Map createNative(String orderId, String total_fee){
    //1.创建参数
    Map<String,String> param=new HashMap();//创建参数
    param.put("appid", appid);//公众号
    param.put("mch_id", partner);//商户号
    param.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
    param.put("body", "尚硅谷");//商品描述
    param.put("out_trade_no", orderId);//商户订单号
    param.put("total_fee",total_fee);//总金额（分）
    param.put("spbill_create_ip", "127.0.0.1");//IP
    param.put("notify_url", "http://order.gmall.com/trade");//回调地址(随便写)
    param.put("trade_type", "NATIVE");//交易类型
    try {
        //2.生成要发送的xml
        String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
        System.out.println(xmlParam);
        HttpClient client=new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
        client.setHttps(true);
        client.setXmlParam(xmlParam);
        client.post();
        //3.获得结果
        String result = client.getContent();
        System.out.println(result);
        Map<String, String> resultMap = WXPayUtil.xmlToMap(result);
        Map<String, String> map=new HashMap<>();
        map.put("code_url", resultMap.get("code_url"));//支付地址
        map.put("total_fee", total_fee);//总金额
        map.put("out_trade_no",orderId);//订单号
        return map;
    } catch (Exception e) {
        e.printStackTrace();
        return new HashMap<>();
    }

    }

    //添加发送方法
    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue paymentResultQueue  = session.createQueue("PAYMENT_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(paymentResultQueue );
            ActiveMQMapMessage mapMessage  = new ActiveMQMapMessage();
            mapMessage.setString("orderId",paymentInfo.getOrderId());
            mapMessage.setString("result",result);
            producer.send(mapMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
