package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.web.feign.sentinel.IntergralApiFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "intergral-service",fallback = IntergralApiFallback.class)
public interface IntergralApi {

    @RequestMapping("/integral/decrIntegral")
    Result decrIntegral(@RequestBody OperateIntergralVo vo);

    @RequestMapping("/integral/incrIntegral")
    Result incrIntegral(@RequestBody OperateIntergralVo vo);

}
