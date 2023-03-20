package cn.wolfcode.service;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * Created by wolfcode-lanxw
 */
public interface IOrderInfoService {

    OrderInfo queryOrderByPhoneAndseckillId(String userPhone, Integer seckillId);

    OrderInfo doSeckill(String userPhone, SeckillProductVo seckillProductVo);

    OrderInfo findByOrderNo(String orderNo);

    OrderInfo createSeckill(String userPhone, SeckillProductVo seckillProductVo);

    void cancelOrder(String orderNo);

    /**
     * 获取支付服务返回的字符串
     * @param orderNo
     * @return
     */
    Result<String> payOnline(String orderNo);

    /**
     * 修改订单状态
     * @param orderNo
     * @param status
     * @param payType
     * @return
     */
    int changePayStatus(String orderNo,Integer status,int payType);

    void refundOnline(OrderInfo order);

    void payIntergral(String orderNo);

    void refundIntagral(OrderInfo order);
}
