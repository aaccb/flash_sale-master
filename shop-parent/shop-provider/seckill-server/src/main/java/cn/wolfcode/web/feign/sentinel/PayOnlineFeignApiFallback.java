package cn.wolfcode.web.feign.sentinel;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.PayVo;
import cn.wolfcode.domain.RefundVo;
import cn.wolfcode.web.feign.PayOnlineFeignApi;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Component
public class PayOnlineFeignApiFallback implements PayOnlineFeignApi {
    @Override
    public Result<String> payOnline(@RequestBody PayVo vo) {
        return null;
    }

    @Override
    public Result<Boolean> rsaCheckV1(Map<String, String> params) {
        return null;
    }

    @Override
    public Result<Boolean> refundOnline(RefundVo vo) {
        return null;
    }
}
