package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.AccountTransaction;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.mapper.AccountTransactionMapper;
import cn.wolfcode.mapper.UsableIntegralMapper;
import cn.wolfcode.service.IUsableIntegralService;
import cn.wolfcode.web.msg.IntergralCodeMsg;
import com.alibaba.fastjson.JSON;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * Created by lanxw
 */
@Service
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Resource
    private UsableIntegralMapper usableIntegralMapper;
    @Resource
    private AccountTransactionMapper accountTransactionMapper;

    @Override
    public void decrIntegral(OperateIntergralVo vo) {
        int count = usableIntegralMapper.decrIntergral(vo.getUserId(), vo.getValue());
        if (count == 0){
            //支付失败
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
    }

    @Override
    public void incrIntegral(OperateIntergralVo vo) {
        usableIntegralMapper.incrIntergral(vo.getUserId(), vo.getValue());
    }

    @Override
    @Transactional
    public void decrIntegralTry(OperateIntergralVo vo, BusinessActionContext context) {
        System.out.println("执行TRY方法");
        //插入事务控制表
        AccountTransaction log=new AccountTransaction();
        log.setTxId(context.getXid());  //全局事务id
        log.setActionId(context.getBranchId()); //分支事务id
        log.setUserId(vo.getUserId());
        log.setAmount(vo.getValue());
        Date date=new Date();
        log.setGmtModified(date);
        log.setGmtCreated(date);
        accountTransactionMapper.insert(log);
        //执行业务逻辑 减积分
        int count = usableIntegralMapper.decrIntergral(vo.getUserId(), vo.getValue());
        if(count ==0){
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
    }

    @Override
    public void decrIntegralCommit(BusinessActionContext context) {
        System.out.println("执行COMMIT方法");
        //查询事务记录
        AccountTransaction account = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if (account == null) {
            //如果为空，写MQ通知管理员
        } else if (AccountTransaction.STATE_TRY== account.getState()) {
            //如果不为空
            //如果状态为TRY，执行逻辑代码
            //更新日志状态
            accountTransactionMapper.updateAccountTransactionState(account.getTxId(),account.getActionId(), AccountTransaction.STATE_COMMIT,AccountTransaction.STATE_TRY);
        } else if (AccountTransaction.STATE_COMMIT == account.getState()) {
            //如果状态为COMMIT不作任何操作
        }else {
            //如果状态为其他，写MQ通知管理员
        }

    }

    @Override
    @Transactional
    public void decrIntegralRollback(BusinessActionContext context) {
        System.out.println("执行rollback方法");
        AccountTransaction account = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if (account != null) {
            //存在日志记录
            if (AccountTransaction.STATE_TRY== account.getState()){ ////判断是否初始化
                //将状态改为Cancel
                accountTransactionMapper.updateAccountTransactionState(account.getTxId(),account.getActionId(),AccountTransaction.STATE_CANCEL,AccountTransaction.STATE_TRY);
                //继续执行Cancel
                int count = usableIntegralMapper.incrIntergral(account.getUserId(), account.getAmount());
                if (count == 0) {
                    //
                }
            }else if(AccountTransaction.STATE_CANCEL== account.getState()){ //判断状态是否为回滚
                //运行幂等，返回成功
            }else {
                //通知管理员
            }

        }else {
            //不存在日志记录
            //插入事务记录
            String str = (String) context.getActionContext("vo");
            System.out.println("str = " + str);
            OperateIntergralVo vo = JSON.parseObject(str, OperateIntergralVo.class);
            AccountTransaction log=new AccountTransaction();
            log.setTxId(context.getXid());  //全局事务id
            log.setActionId(context.getBranchId()); //分支事务id
            log.setUserId(vo.getUserId());
            log.setAmount(vo.getValue());
            Date date=new Date();
            log.setGmtModified(date);
            log.setGmtCreated(date);
            log.setState(AccountTransaction.STATE_CANCEL);
            accountTransactionMapper.insert(log);
        }
    }

}
