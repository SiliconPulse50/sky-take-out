package com.sky.mapper;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);


    Orders getByNumber(String outTradeNo);

    void update(Orders orders);

    Orders getByNumberAndUserId(@Param("orderNumber")String orderNumber,@Param("userId") Long userId);
}
