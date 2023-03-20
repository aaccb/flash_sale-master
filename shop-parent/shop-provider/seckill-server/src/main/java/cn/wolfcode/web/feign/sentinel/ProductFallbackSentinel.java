package cn.wolfcode.web.feign.sentinel;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.web.feign.ProductFeign;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Component
public class ProductFallbackSentinel implements ProductFeign {
    @Override
    public Result<List<Product>> queryProductByIds(@RequestParam("ids")List<Long> ids) {
        return null;
    }
}
