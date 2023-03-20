package cn.wolfcode.mq;

import cn.wolfcode.ws.OrderWebSocketServer;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.util.concurrent.TimeUnit;


@Component
@RocketMQMessageListener(consumerGroup = "orderResultGroup", topic = MQConstants.ORDER_RESULT_TOPIC)
public class OrderResultQueueListener implements RocketMQListener<OrderMQResult> {

    @Override
    public void onMessage(OrderMQResult message) {
        Session session = null;
        int count = 3;
        while (count-- > 0) {
            session = OrderWebSocketServer.clients.get(message.getToken());
            if (session != null) {
                try {
                    session.getBasicRemote().sendText(JSON.toJSONString(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
