package cn.wolfcode.web.controller;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.common.web.anno.RequireLogin;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mq.MQConstant;
import cn.wolfcode.mq.OrderMessage;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.DateUtil;
import cn.wolfcode.util.UserUtil;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/order")
@Slf4j
public class OrderInfoController {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private IOrderInfoService orderInfoService;

    @RequestMapping("/doSeckill")
    @RequireLogin
    public Result<String> doSeckill(Integer time, Long seckillId, HttpServletRequest request){
        //1.判断时间是否过期
        SeckillProductVo seckillProductVo = seckillProductService.find(time, seckillId);
        boolean legalTime = DateUtil.isLegalTime(seckillProductVo.getStartDate(),seckillProductVo.getTime());
        if (!legalTime) {
            return Result.error(CommonCodeMsg.ILLEGAL_OPERATION);
        }
        //2.一个用户只能抢购一个商品
        //获取Token信息
        String token = request.getHeader(CommonConstants.TOKEN_NAME);
        //获取用户phone
        String userPhone = UserUtil.getUserPhone(redisTemplate, token);
        //OrderInfo orderInfo=orderInfoService.queryOrderByPhoneAndseckillId(userPhone,seckillId);
        String orderKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(seckillId));
        if (redisTemplate.opsForSet().isMember(orderKey,userPhone)) {
            return Result.error(SeckillCodeMsg.REPEAT_SECKILL);
        }
        //判断库存是否充足
        String realKey = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
        Long increment = redisTemplate.opsForHash().increment(realKey, String.valueOf(seckillId), -1);
        if (increment < 0) {
            return Result.error(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        OrderMessage orderMessage = new OrderMessage(time,seckillId,token,Long.valueOf(userPhone));
        rocketMQTemplate.syncSend(MQConstant.ORDER_PEDDING_TOPIC,orderMessage);
        //OrderInfo orderInfo=orderInfoService.doSeckill(userPhone,seckillProductVo);
        //3.创建秒杀订单
        //4.扣减库存


        return Result.success("进入抢购队列,请等待结果");
    }

    @RequestMapping("/find")
    @RequireLogin
    public Result<OrderInfo> find(String orderNo,HttpServletRequest request){
        OrderInfo order=orderInfoService.findByOrderNo(orderNo);
        //判断该订单是否是该用户的
        String token = request.getHeader(CommonConstants.TOKEN_NAME);
        String phone = UserUtil.getUserPhone(redisTemplate, token);
        if (!phone.equals(String.valueOf(order.getUserId()))){
            Result.error(CommonCodeMsg.ILLEGAL_OPERATION);
        }
        return Result.success(order);
    }

}
