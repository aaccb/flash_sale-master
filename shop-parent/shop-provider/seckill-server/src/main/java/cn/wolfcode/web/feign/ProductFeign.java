package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.web.feign.sentinel.ProductFallbackSentinel;
import org.apache.ibatis.annotations.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service",fallback = ProductFallbackSentinel.class)
public interface ProductFeign {

    @RequestMapping("/product/queryProductByIds")
    Result<List<Product>> queryProductByIds(@RequestParam("ids")List<Long> ids);
}
