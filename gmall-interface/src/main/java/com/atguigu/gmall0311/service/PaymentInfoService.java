package com.atguigu.gmall0311.service;

import com.atguigu.gmall0311.bean.PaymentInfo;

import java.util.Map;

public interface PaymentInfoService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);

    boolean refund(String orderId);

    Map createNative(String orderId, String total_fee);

    void sendPaymentResult(PaymentInfo paymentInfo,String result);
}
