package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.domain.RefundVo;
import cn.wolfcode.web.feign.sentinel.PayOnlineFeignApiFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "pay-service",fallback = PayOnlineFeignApiFallback.class)
public interface PayOnlineFeignApi {

    @RequestMapping("/alipay/payOnline")
    Result<String> payOnline(@RequestBody PayVo vo);

    @RequestMapping("/alipay/rsaCheckV1")
    Result<Boolean> rsaCheckV1(@RequestParam Map<String, String> params);

    @RequestMapping("/alipay/refundOnline")
    Result<Boolean> refundOnline(@RequestBody RefundVo vo);
}
