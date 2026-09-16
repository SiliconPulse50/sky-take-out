package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;

import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class DishServiceImpl implements DishService {
    /**
     * 新增菜品和他对应的口味
     */
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;


   
    @Transactional
    //两张表同生共死DTO->实体，同名属性直接搬
    public void savewithflavor(DishDTO dishDTO){
        //向菜品表加入一条
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO ,dish);
        //新增语义:主键必须由数据库分配,防止前端误传 id 变成"显式指定主键"
        dish.setId(null);
        //强制清空
        dishMapper.insert(dish);//执行mysql，得到分配的id
        // 在 savewithflavor 的 insert 之后临时加一行
        //log.info("回填 id = {}, DTO 里的 id = {}", dish.getId(), dishDTO.getId());
        Long dishId=dish.getId();//拿回填的自增id

        //过滤掉前端漏选的空口味(否则库里会出现 name='' 的脏数据)
        List<DishFlavor> flavors = CollectionUtils.isEmpty(dishDTO.getFlavors())
                ? Collections.emptyList()
                : dishDTO.getFlavors().stream()
                        .filter(f -> f.getName() != null && !f.getName().trim().isEmpty())
                        .collect(Collectors.toList());
/*
    <insert id="insert" useGeneratedKeys="true" keyProperty="id" >mybatis依靠这个把数据库生成的值回填回dish对象*/
        if (!CollectionUtils.isEmpty(flavors)) {
            flavors.forEach(dishFlavor -> {
                //给子表补外键
                dishFlavor.setDishId(dishId);

            });
            //向口味表里插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }



    }
    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // ① 开启分页:pageNum、pageSize 放进 ThreadLocal,只对"下一条查询"生效
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // ② 执行查询:PageHelper 会自动加 limit,并额外执行一条 count
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        // ③ 组装返回:total = 总数,getResult() = 当前页数据
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品的批量删除功能
     * @param ids
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能删除，存在起售中的菜品
        for (Long  id:ids) {
            Dish dish=dishMapper.getById(id);
            if(dish.getStatus()==1){
                //当前菜品起售中，想卖就要先停售
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }

        }
        //判断菜品是否关联套餐
        List<Long> setmealIds=setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds!=null && setmealIds.size()>0){
            //当前菜品不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);

        }

        //删除后连带的口味数据也要删除
        for(Long id: ids){

            //删除菜品相关的口味数据
            //连带删除子表
            dishFlavorMapper.deleteByDishId(id);
            dishMapper.deleteById(id);
        }
    }
    /**
     *
     * 根据id来查询菜品和口味
     * @param id
     * @return
     */

    public DishVO getByIdwithFlavor(Long id){
        Dish dish=dishMapper.getById(id);
        List<DishFlavor> dishFlavors=dishFlavorMapper.getByDishId(id);
        DishVO dishVO=new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO ;
    }
    /**
     * 根据id修改菜品基本信息和其对应的口味信息
     */
    @Transactional
    public void updatewithFlavor(DishDTO dishDTO){
        //口味这么多怎么修改呢
        Dish dish=new Dish();
        //DTO里面的id已经被拷贝过去了
        BeanUtils.copyProperties(dishDTO,dish);
         dishMapper.update(dish);//用dish比用dishdto 更加合理因为dishDTO里面包含flavor的信息，所以不用直接用dish即可
        //修改菜品基本信息
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //先删除再插入
        List<DishFlavor> flavors = dishDTO.getFlavors();
        dishFlavorMapper.insertBatch(flavors);


    }


}
/*:先插菜拿到 id,再插口味。@Transactional 生效还有个前提:它是通过
Spring 代理调用的(Controller 注入接口调用,"自己调自己"会导致事务失效)。*/