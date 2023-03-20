package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.*;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.feign.IntergralApi;
import cn.wolfcode.web.feign.PayOnlineFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * Created by wolfcode-lanxw
 */
@Service
public class OrderInfoSeviceImpl implements IOrderInfoService {
    @Resource
    private ISeckillProductService seckillProductService;
    @Resource
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private PayLogMapper payLogMapper;
    @Resource
    private RefundLogMapper refundLogMapper;
    @Resource
    IntergralApi intergralApi;

    @Resource
    private PayOnlineFeignApi payOnlineFeignApi;

    @Override
    public OrderInfo queryOrderByPhoneAndseckillId(String userPhone, Integer seckillId) {
        return orderInfoMapper.queryOrderByPhoneAndseckillId(userPhone,seckillId);
    }

    @Override
    @Transactional
    public OrderInfo doSeckill(String userPhone, SeckillProductVo seckillProductVo) {
        //4.扣减库存
        int count = seckillProductService.doStockCount(seckillProductVo.getId());
        if (count == 0) {
            throw new BusinessException(SeckillCodeMsg.REPEAT_SECKILL);
        }
        //5.创建秒杀订单
        OrderInfo orderInfo=createSeckill(userPhone,seckillProductVo);
        String realKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(seckillProductVo.getId()));
        redisTemplate.opsForSet().add(realKey,userPhone);
        return orderInfo;
    }

    /**
     * 根据订单编号查询订单
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo findByOrderNo(String orderNo) {
        return orderInfoMapper.find(orderNo);
    }

    @Override
    public OrderInfo createSeckill(String userPhone, SeckillProductVo seckillProductVo) {
        OrderInfo orderInfo=new OrderInfo();
        BeanUtils.copyProperties(seckillProductVo,orderInfo);
        orderInfo.setUserId(Long.parseLong(userPhone));//用户id
        orderInfo.setCreateDate(new Date());//创建日期
        orderInfo.setDeliveryAddrId(1L);//收货地址
        orderInfo.setSeckillDate(seckillProductVo.getStartDate());//设置秒杀日期
        orderInfo.setSeckillTime(seckillProductVo.getTime());//设置秒杀场次
        orderInfo.setOrderNo(String.valueOf(IdGenerateUtil.get().nextId()));//订单编号
        orderInfo.setSeckillId(seckillProductVo.getId());
        orderInfoMapper.insert(orderInfo);
        return orderInfo;
    }

    @Override
    public void cancelOrder(String orderNo) {
        System.out.println("超时取消订单逻辑开始...");
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        //判断订单是否处于未支付状态
        if(OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())){
            //修改订单状态
            int count = orderInfoMapper.updateCancelStatus(orderNo,OrderInfo.STATUS_TIMEOUT);
            if (count == 0) {
                return;
            }
            //真实库存回补
            seckillProductService.incrStock(orderInfo.getSeckillId());
            //预库存回补
            seckillProductService.syncStockRedis(orderInfo.getSeckillTime(),orderInfo.getSeckillId());
        }
        System.out.println("超时取消订单逻辑结束...");
    }

    //在线支付
    @Value("${pay.returnUrl}")
    private String returnUrl;
    @Value("${pay.notifyUrl}")
    private String notifyUrl;
    @Override
    @Transactional
    public Result<String> payOnline(String orderNo) {
        OrderInfo order = this.findByOrderNo(orderNo);
        PayVo vo=new PayVo();
        vo.setBody(order.getProductName());
        vo.setSubject(order.getProductName());
        vo.setOutTradeNo(orderNo);
        vo.setTotalAmount(String.valueOf(order.getIntergral()));
        vo.setNotifyUrl(notifyUrl);
        vo.setReturnUrl(returnUrl);
        Result<String> result=payOnlineFeignApi.payOnline(vo);
        return result;
    }

    @Override
    public int changePayStatus(String orderNo, Integer status, int payType) {
        return orderInfoMapper.changePayStatus(orderNo,status,payType);
    }

    @Override
    @Transactional
    public void refundOnline(OrderInfo order) {
        RefundVo vo=new RefundVo();
        vo.setRefundReason("不想要了");
        vo.setRefundAmount(String.valueOf(order.getSeckillPrice()));
        vo.setOutTradeNo(order.getOrderNo());
        Result<Boolean> result = payOnlineFeignApi.refundOnline(vo);
        if(result ==null || result.hasError() || !result.getData()){
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        orderInfoMapper.changeRefundStatus(order.getOrderNo(),OrderInfo.STATUS_REFUND);
    }

    @Override
    @GlobalTransactional
    public void payIntergral(String orderNo) {
        OrderInfo orderInfo = this.findByOrderNo(orderNo);
        //插入日志
        PayLog log=new PayLog();
        log.setOrderNo(orderInfo.getOrderNo());
        log.setPayType(OrderInfo.PAYTYPE_INTERGRAL);
        log.setTotalAmount(String.valueOf(orderInfo.getProductPrice()));
        log.setPayTime(new Date());
        payLogMapper.insert(log);
        OperateIntergralVo vo=new OperateIntergralVo();
        vo.setUserId(orderInfo.getUserId());
        vo.setValue(orderInfo.getIntergral());
        //远程调用积分服务
        Result result=intergralApi.decrIntegral(vo);
        if (result == null || result.hasError()) {
            throw new BusinessException(SeckillCodeMsg.INTERGRAL_SERVER_ERROR);
        }
        //修改订单状态
        int count = orderInfoMapper.changePayStatus(orderNo,OrderInfo.STATUS_ACCOUNT_PAID,OrderInfo.PAYTYPE_INTERGRAL);
        if (count == 0) {
            //修改失败
            throw new BusinessException(SeckillCodeMsg.PAY_ERROR);
        }
    }

    @Override
    @GlobalTransactional
    public void refundIntagral(OrderInfo order) {
        //判断是否已经付款
        if (OrderInfo.STATUS_ACCOUNT_PAID.equals(order.getStatus())){
            //增加退款记录
            RefundLog log=new RefundLog();
            log.setOrderNo(order.getOrderNo());
            log.setRefundTime(new Date());
            log.setRefundType(OrderInfo.PAYTYPE_INTERGRAL);
            log.setRefundAmount(order.getIntergral());
            log.setRefundReason("不想要了");
            refundLogMapper.insert(log);
            OperateIntergralVo vo=new OperateIntergralVo();
            vo.setUserId(order.getUserId());
            vo.setValue(order.getIntergral());
            //远程调用积分服务
            Result result=intergralApi.incrIntegral(vo);
            //修改订单状态
            int count = orderInfoMapper.changeRefundStatus(order.getOrderNo(),OrderInfo.STATUS_REFUND);
            if (count == 0) {
                //修改失败
                throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
            }
            int i=1/0;
        }
    }


}
