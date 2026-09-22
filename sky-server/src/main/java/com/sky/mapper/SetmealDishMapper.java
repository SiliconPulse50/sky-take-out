package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id查询对应的套餐id
     * @param dishIds
     * @return
     */
    //select setmeal id from setmeal dish where dish_id in(1,2,3,4)
   // List<Long> getSetmealIdsByDishIds(List<Long> dishIds);
    List<Long> getSetmealIdsByDishIds(@Param("dishIds") List<Long> dishIds);
    // ← 与 XML 的 collection="dishIds" 对齐

   // void insertBatch(List<SetmealDish> setmealDishes);

    void insertBatch(@Param("setmealDishes") List<SetmealDish> setmealDishes);

    @Delete("delete from setmeal_dish where setmeal_id =#{setmealId}")
    void deleteBySetmealId(Long setmealId);
}
