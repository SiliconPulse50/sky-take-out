package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 批量插入口味数据
     * @param flavors
     */
//@param 要遍历的集合，名字规定
    void insertBatch(@Param("flavors") List<DishFlavor> flavors);

    /**
     * 根据菜品id来删除对应的口味数据
     * @param dishid
     */
    @Delete("delete from dish_flavor where dish_id = #{dish_id}")
    void deleteByDishId(Long dishid);

    /**
     *
     * @return
     */
    @Select("select * from dish_flavor WHERE dish_id= #{dishId}")
    List<DishFlavor> getByDishId(Long dishId);
}
