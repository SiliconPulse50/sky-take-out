package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;

import com.sky.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class DishServiceImpl implements DishService {
    /**
     * 新增菜品和他对应的口味
     */
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Transactional
    //两张表同生共死DTO->实体，同名属性直接搬
    public void savewithflavor(DishDTO dishDTO){
        //向菜品表加入一条
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO ,dish);
        //新增语义:主键必须由数据库分配,防止前端误传 id 变成"显式指定主键"
        dish.setId(null);

        dishMapper.insert(dish);
        Long dishId=dish.getId();//拿回填的自增id

        //过滤掉前端漏选的空口味(否则库里会出现 name='' 的脏数据)
        List<DishFlavor> flavors = CollectionUtils.isEmpty(dishDTO.getFlavors())
                ? Collections.emptyList()
                : dishDTO.getFlavors().stream()
                        .filter(f -> f.getName() != null && !f.getName().trim().isEmpty())
                        .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(flavors)) {
            flavors.forEach(dishFlavor -> {
                //给子表补外键
                dishFlavor.setDishId(dishId);

            });
            //向口味表里插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }



    }
}
/*:先插菜拿到 id,再插口味。@Transactional 生效还有个前提:它是通过
Spring 代理调用的(Controller 注入接口调用,"自己调自己"会导致事务失效)。*/