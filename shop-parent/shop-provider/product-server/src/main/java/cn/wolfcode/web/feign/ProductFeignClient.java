package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/product")
@Slf4j
public class ProductFeignClient {
    @Autowired
    private IProductService productService;

    @RequestMapping("/queryProductByIds")
    public Result<List<Product>> queryProductByIds(@RequestParam("ids") List<Long> ids){
        return Result.success(productService.queryProductByIds(ids));
    }
}
