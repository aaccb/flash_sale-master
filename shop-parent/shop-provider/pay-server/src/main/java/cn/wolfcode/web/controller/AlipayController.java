package cn.wolfcode.web.controller;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.config.AlipayProperties;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.domain.RefundVo;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/alipay")
public class AlipayController {
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private AlipayProperties alipayProperties;

    @RequestMapping("/payOnline")
    public Result<String> payOnline(@RequestBody PayVo vo) throws AlipayApiException {
        //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(vo.getReturnUrl());
        alipayRequest.setNotifyUrl(vo.getNotifyUrl());
        alipayRequest.setBizContent("{\"out_trade_no\":\""+ vo.getOutTradeNo() +"\","
                + "\"total_amount\":\""+ vo.getTotalAmount() +"\","
                + "\"subject\":\""+ vo.getSubject() +"\","
                + "\"body\":\""+ vo.getBody() +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
        //请求
        String result = alipayClient.pageExecute(alipayRequest).getBody();
        return Result.success(result);
    }

    @RequestMapping("/rsaCheckV1")
    Result<Boolean> rsaCheckV1(@RequestParam Map<String, String> params) throws AlipayApiException {
        boolean signVerified = AlipaySignature.rsaCheckV1(params,
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getCharset(),
                alipayProperties.getSignType()); //调用SDK验证签名
       return Result.success(signVerified);
    }

    @RequestMapping("/refundOnline")
    public Result<Boolean> refundOnline(@RequestBody RefundVo vo) throws AlipayApiException {
        AlipayTradeRefundRequest alipayRequest=new AlipayTradeRefundRequest();
        alipayRequest.setBizContent("{\"out_trade_no\":\""+ vo.getOutTradeNo() +"\","
                + "\"trade_no\":\"\","
                + "\"refund_amount\":\""+ vo.getRefundAmount() +"\","
                + "\"refund_reason\":\""+ vo.getRefundReason() +"\","
                + "\"out_request_no\":\"\"}");
        AlipayTradeRefundResponse response = alipayClient.execute(alipayRequest);
        return Result.success(response.isSuccess());
    }
}
