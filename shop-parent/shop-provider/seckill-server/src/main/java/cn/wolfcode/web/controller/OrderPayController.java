package cn.wolfcode.web.controller;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.web.feign.PayOnlineFeignApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/orderPay")
@RefreshScope
public class OrderPayController {
    @Autowired
    private IOrderInfoService orderInfoService;

    @Resource
    private PayOnlineFeignApi payOnlineFeignApi;

    @RequestMapping("/pay")
    public Result<String> pay(String orderNo,Integer type){
        if (OrderInfo.PAYTYPE_ONLINE.equals(type)){
            //在线支付
            return orderInfoService.payOnline(orderNo);
        }else {
            //积分支付
            orderInfoService.payIntergral(orderNo);
            return Result.success();
        }
    }

    //异步回调
    @RequestMapping("/notifyUrl")
    public String notifyUrl(@RequestParam Map<String,String> params){
        System.out.println("异步回调");
        Result<Boolean> result = payOnlineFeignApi.rsaCheckV1(params);
        if (result == null || result.hasError()) {
            return "fail";
        }
        Boolean data = result.getData();//调用SDK验证签名
        if (data) {
            //签名成功
            String orderNo = params.get("out_trade_no");
            //更改订单状态
            int i = orderInfoService.changePayStatus(orderNo,OrderInfo.STATUS_ACCOUNT_PAID,OrderInfo.PAYTYPE_ONLINE);
            if (i == 0) {
                //发送消息给客服，退款
            }
        }
        return "success";
    }

    //同步回调
    @Value("${pay.errorUrl}")
    private String errorUrl;
    @Value("${pay.frontEndPayUrl}")
    private String frontEndPayUrl;
    @RequestMapping("/returnUrl")
    public void returnUrl(@RequestParam Map<String,String> params,HttpServletResponse response) throws IOException {
        System.out.println("同步回调");
        Result<Boolean> result = payOnlineFeignApi.rsaCheckV1(params);
        if (result == null || result.hasError() ||!result.getData()) {
            response.sendRedirect(errorUrl);
            return;
        }
        String orderNo = params.get("out_trade_no");
        response.sendRedirect(frontEndPayUrl+orderNo);
    }

    @RequestMapping("/refund")
    public Result<String> refund(String orderNo){
        OrderInfo order = orderInfoService.findByOrderNo(orderNo);
        if(OrderInfo.PAYTYPE_ONLINE.equals(order.getPayType())){
            //线上支付
            //退款
            orderInfoService.refundOnline(order);
        }else {
            //积分支付
            //退积分
            orderInfoService.refundIntagral(order);
        }
        return Result.success();
    }
}
