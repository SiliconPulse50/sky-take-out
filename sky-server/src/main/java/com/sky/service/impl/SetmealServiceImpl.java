package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired private DishMapper dishMapper;      // ★ 补这个字段

@Transactional
    public void saveWithDish(SetmealDTO setmealDTO){
    //第一步把DTO转化成setmeal 实体插入套餐表
    Setmeal setmeal=new Setmeal();
    BeanUtils.copyProperties(setmealDTO,setmeal);
    setmealMapper.insert(setmeal);
    //拿到套餐自增id 需要insert标签加user GeneratedKeys
    Long setmealId=setmeal.getId();
    //遍历菜品列表，每个设置setmealid 批量插入套餐菜品关系表
    List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
    if(setmealDishes!=null && setmealDishes.size()>0){
        setmealDishes.forEach(setmealDish ->
        setmealDish.setSetmealId(setmealId));
        setmealDishMapper .insertBatch(setmealDishes);
        }
    }

    /**
     * 分页查询套餐
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO){
    int pageNum =setmealPageQueryDTO.getPage();
    int pageSize =setmealPageQueryDTO.getPageSize();

    PageHelper.startPage(pageNum,pageSize);
    Page<SetmealVO> page=setmealMapper.pageQuery(setmealPageQueryDTO);
    return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 根据id删除套餐admin管理端，批量
     * @param ids
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids){
        ids.forEach(id->{
            Setmeal setmeal =setmealMapper.getById(id);
            if(setmeal ==null){
                return;
            }
            if(StatusConstant.ENABLE==setmeal.getStatus()){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        );
        ids.forEach(setmealId->{
            setmealMapper.deleteById(setmealId);//删主表
            setmealDishMapper.deleteBySetmealId(setmealId);//删子表
        });


    }

    /**
     * 回显VO,多表拼装
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id){
        Setmeal setmeal = setmealMapper.getById(id);//查主表
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
                //  查子表
        SetmealVO setmealVO = new SetmealVO();               // ★ SetmealVO 里正好有 setmealDishes 字段
        BeanUtils.copyProperties(setmeal, setmealVO);//主表字段搬过去
        setmealVO.setSetmealDishes(setmealDishes);//子表列表装进去
        return setmealVO;                                    // ★ 返回 VO
    }

    /**
     * 修改 -4步+事务
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO){
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        setmealMapper.update(setmeal);

        Long setmealId=setmealDTO.getId();
        // 删掉旧的关系全部
        setmealDishMapper.deleteBySetmealId(setmealId);
        // 给关系补充外键，整批插入
       List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
       if (setmealDishes != null && setmealDishes.size() > 0) {  //  补判断
           setmealDishes.forEach(sd -> sd.setSetmealId(setmealId));   //  后端补外键(你写对了 ✓)
           setmealDishMapper.insertBatch(setmealDishes);
       }

   }
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if(status == StatusConstant.ENABLE){
            //select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = ?
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList != null && dishList.size() > 0){
                dishList.forEach(dish -> {
                    if(StatusConstant.DISABLE == dish.getStatus()){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }



}
