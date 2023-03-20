package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.Product;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mapper.SeckillProductMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.feign.ProductFeign;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by lanxw
 */
@Service
public class SeckillProductServiceImpl implements ISeckillProductService {
    @Resource
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private IOrderInfoService orderInfoService;

    @Resource
    private ProductFeign productFeign;

    @Override
    public List<SeckillProductVo> queryCurrentlySeckillProduct(Integer time) {
        List<SeckillProduct> seckillProducts = seckillProductMapper.queryCurrentlySeckillProduct(time);

        if (seckillProducts.size()==0) {
            return Collections.EMPTY_LIST;
        }
        List<Long> productIds=new ArrayList<>();
        for (SeckillProduct seckillProduct:seckillProducts) {
            productIds.add(seckillProduct.getProductId());
        }

       Result<List<Product>> result=productFeign.queryProductByIds(productIds);
        if (result == null || result.hasError()) {
            throw new BusinessException(CommonCodeMsg.ILLEGAL_OPERATION);
        }

        List<Product> products = result.getData();

        Map<Long,Product> productMap =new HashMap<>();
        for (Product product:products) {
            productMap.put(product.getId(),product);
        }

        List<SeckillProductVo> listVo=new ArrayList<>();
        for (SeckillProduct seckillProduct :seckillProducts) {
            SeckillProductVo vo=new SeckillProductVo();
            Product product = productMap.get(seckillProduct.getProductId());
            BeanUtils.copyProperties(product,vo);
            BeanUtils.copyProperties(seckillProduct,vo);
            vo.setCurrentCount(seckillProduct.getStockCount());

            listVo.add(vo);
        }

        return listVo;
    }

    @Override
    public SeckillProductVo find(Integer time, Long seckillId) {
        SeckillProduct seckillProduct = seckillProductMapper.getSeckillProductBySeckillId(seckillId);

        List<Long> productIds=new ArrayList<>();
        productIds.add(seckillProduct.getProductId());

        Result<List<Product>> result=productFeign.queryProductByIds(productIds);
        if (result == null || result.hasError()) {
            throw new BusinessException(CommonCodeMsg.ILLEGAL_OPERATION);
        }
        Product product = result.getData().get(0);

        SeckillProductVo vo=new SeckillProductVo();
        BeanUtils.copyProperties(product,vo);
        BeanUtils.copyProperties(seckillProduct,vo);
        vo.setCurrentCount(seckillProduct.getStockCount());

        return vo;
    }

    @Override
    public int doStockCount(Long id) {
        return seckillProductMapper.decrStock(id);
    }

    @Override
    @Transactional
    public OrderInfo doSeckill(String userPhone, SeckillProductVo seckillProductVo) {
        //4.扣减库存
        int count = seckillProductMapper.getStockCount(seckillProductVo.getId());
        if (count == 0) {
            throw new BusinessException(SeckillCodeMsg.REPEAT_SECKILL);
        }
        //5.创建秒杀订单
        OrderInfo orderInfo=orderInfoService.createSeckill(userPhone,seckillProductVo);
        String realKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(seckillProductVo.getId()));
        redisTemplate.opsForSet().add(realKey,userPhone);
        return orderInfo;
    }

    @Override
    public List<SeckillProductVo> queryByTimeFromCache(Integer time) {
        String key = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        List<Object> values = redisTemplate.opsForHash().values(key);
        List<SeckillProductVo> listVo=new ArrayList<>();
        for (Object obj:values){
            listVo.add(JSON.parseObject((String)obj,SeckillProductVo.class));
        }
        return listVo;
    }

    @Override
    public SeckillProductVo findFromCache(Integer time, Long seckillId) {
        String key = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        Object obj = redisTemplate.opsForHash().get(key, String.valueOf(seckillId));
        SeckillProductVo vo = JSON.parseObject((String)obj, SeckillProductVo.class);
        return vo;
    }

    @Override
    public void syncStockRedis(Integer time, Long seckillId) {
        //数据同步到redis中
        SeckillProduct product = seckillProductMapper.getSeckillProductBySeckillId(seckillId);
        if (product.getStockCount() >0) {
            String key = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
            redisTemplate.opsForHash().put(key,String.valueOf(seckillId),String.valueOf(product.getStockCount()));
        }
    }

    @Override
    public void incrStock(Long seckillId) {
        seckillProductMapper.incrStock(seckillId);
    }
}
