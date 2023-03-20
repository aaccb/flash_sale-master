package cn.wolfcode.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "seckill-service")
public interface SeckillProductFeign {

    @RequestMapping("/seckillProduct/queryByTimeForJob")
    public Result<List<SeckillProductVo>> queryByTimeForJob(@RequestParam("time")Integer time);
}
