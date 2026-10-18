package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/*

定时任务类

 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    /*
    处理超时订单的表达式
     */
    @Scheduled(cron = "0 0/1 * * * ?")     // 每分钟第 0 秒
    public void processTimeOrder(){
        log.info("定时处理订单：{}", LocalDateTime.now());
        LocalDateTime time= LocalDateTime.now().plusMinutes(-15);
        List<Orders> orderList = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT, time);
        if(orderList != null && orderList.size() > 0 ){
            for(Orders orders: orderList){
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消") ;
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);

            }
        }
    }
    /*
    处理这种一直处于派送中的这种订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processDeliveryOrder(){

        log.info("定时处理这种派送中的订单：{}",LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> orderList2 = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, time);
        if(orderList2 != null && orderList2.size() > 0 ){
            for(Orders orders:orderList2){
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);

            }

        }
    }


}
