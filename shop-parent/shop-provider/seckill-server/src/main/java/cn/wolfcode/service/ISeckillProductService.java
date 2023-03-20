package cn.wolfcode.service;

import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;

import java.util.List;

/**
 * Created by lanxw
 */
public interface ISeckillProductService {
    List<SeckillProductVo> queryCurrentlySeckillProduct(Integer time);

    SeckillProductVo find(Integer time, Long seckillId);

    int doStockCount(Long id);

    OrderInfo doSeckill(String userPhone, SeckillProductVo seckillProductVo);

    List<SeckillProductVo> queryByTimeFromCache(Integer time);

    SeckillProductVo findFromCache(Integer time, Long seckillId);

    void syncStockRedis(Integer time, Long seckillId);

    void incrStock(Long seckillId);
}
