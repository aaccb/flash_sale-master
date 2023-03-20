package cn.wolfcode.job;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.feign.SeckillProductFeign;
import cn.wolfcode.redis.JobRedisKey;
import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by lanxw
 */
@Component
@Setter
@Getter
@RefreshScope
public class SeckillProductCacheJob implements SimpleJob {
    @Value("${jobCron.initSeckillProduct}")
    private String cron;
    @Resource
    private SeckillProductFeign seckillProductFeignApi;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public void execute(ShardingContext shardingContext) {
        String time = shardingContext.getShardingParameter();
        System.out.println("执行定时上架....");
        Result<List<SeckillProductVo>> result = seckillProductFeignApi.queryByTimeForJob(Integer.parseInt(time));
        if(result==null||result.hasError()){
            System.out.println("通知运维人员");
            return;
        }
        List<SeckillProductVo> seckillProductVoList = result.getData();
        //获取秒杀商品key
        String key = JobRedisKey.SECKILL_PRODUCT_HASH.getRealKey(time);
        //获取秒杀商品数量的key
        String countKey = JobRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(time);
        //删除缓存中商品
        redisTemplate.delete(key);
        //删除缓存中商品数量
        redisTemplate.delete(countKey);
        for(SeckillProductVo vo:seckillProductVoList){
            vo.setCurrentCount(vo.getStockCount());
            //更新缓存
            redisTemplate.opsForHash().put(key,String.valueOf(vo.getId()), JSON.toJSONString(vo));
            redisTemplate.opsForHash().put(countKey,String.valueOf(vo.getId()),String.valueOf(vo.getStockCount()));
        }
    }
}
