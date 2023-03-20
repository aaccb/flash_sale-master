package cn.wolfcode.feign.sentinel;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.feign.SeckillProductFeign;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class SeckillProductFeignSentinel implements SeckillProductFeign {
    @Override
    public Result<List<SeckillProductVo>> queryByTimeForJob(Integer time) {
        return null;
    }
}
