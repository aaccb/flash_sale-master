package cn.wolfcode.web.feign.sentinel;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.web.feign.IntergralApi;
import org.springframework.stereotype.Component;

@Component
public class IntergralApiFallback implements IntergralApi {
    @Override
    public Result decrIntegral(OperateIntergralVo vo) {
        return null;
    }

    @Override
    public Result incrIntegral(OperateIntergralVo vo) {
        return null;
    }
}
