package cn.wolfcode.mq;

import cn.wolfcode.common.web.CommonCodeMsg;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RocketMQMessageListener(consumerGroup = "peddingGroup",topic = MQConstant.ORDER_PEDDING_TOPIC)
public class OrderPeddingQueueListener implements RocketMQListener<OrderMessage> {
    @Autowired
    private ISeckillProductService seckillProductService;

    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(OrderMessage orderMessage) {
        OrderMQResult result=new OrderMQResult();
        result.setToken(orderMessage.getToken());
        String tag;
        try {
            SeckillProductVo vo = seckillProductService.findFromCache(orderMessage.getTime(), orderMessage.getSeckillId());
            OrderInfo orderInfo = orderInfoService.doSeckill(String.valueOf(orderMessage.getUserPhone()), vo);
            result.setOrderNo(orderInfo.getOrderNo());
            tag= MQConstant.ORDER_RESULT_SUCCESS_TAG;
            Message<OrderMQResult> message = MessageBuilder.withPayload(result).build();
            //发送延时消息
            rocketMQTemplate.syncSend(MQConstant.ORDER_PAY_TIMEOUT_TOPIC,message,3000,MQConstant.ORDER_PAY_TIMEOUT_DELAY_LEVEL);
        }catch (Exception e){
            e.printStackTrace();
            result.setCode(SeckillCodeMsg.SECKILL_ERROR.getCode());
            result.setTime(orderMessage.getTime());
            result.setSeckillId(orderMessage.getSeckillId());
            result.setMsg(SeckillCodeMsg.SECKILL_ERROR.getMsg());
            tag=MQConstant.ORDER_RESULT_FAIL_TAG;
        }
        rocketMQTemplate.syncSend(MQConstant.ORDER_RESULT_TOPIC+":"+tag,result);
    }
}
