package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DishService {
    /**
     * 新增菜品和对应口味
     * @param dishDTO
     */
    public void savewithflavor(DishDTO dishDTO);

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteBatch(List<Long> ids);

    /**
     *
     * 根据id来查询菜品和口味
     * @param id
     * @return
     */

    DishVO getByIdwithFlavor(Long id);

    /**
     * 根据id修改菜品基本信息和其对应的口味信息
     */
    void updatewithFlavor(DishDTO dishDTO);

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    /**
     *
     * 根据分类id查询菜品
     * @param dish
     * @return
     */
    List<Dish> list(Dish dish);
}
