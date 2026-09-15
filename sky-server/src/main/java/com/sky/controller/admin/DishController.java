package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理
 */
@RestController
@Api(tags="菜品相关接口")
@RequestMapping("/admin/dish")     // ★ 类级统一前缀(和 CategoryController 一致)
@Slf4j
public class DishController {
   @Autowired
   private DishService dishService;
   @PostMapping                     // ★ 去掉 "/admin/dish",否则会变成 /admin/dish/admin/dish
   @ApiOperation("新增菜品")
    public Result save( @RequestBody DishDTO  dishDTO){
        log.info("新增菜品",dishDTO);
        dishService.savewithflavor(dishDTO);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    @DeleteMapping
    @ApiOperation("菜品的批量删除")
    public Result DELETE( @RequestParam List<Long> ids){
        //spring mvc List<Long>,帮助解析，，原来是String ids
        log.info("菜品的批量删除: {}",ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getDish(@PathVariable Long id){
        DishVO dishVO=dishService .getByIdwithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品: {}",dishDTO);
        dishService.updatewithFlavor(dishDTO);
        return Result.success();

    }




}
