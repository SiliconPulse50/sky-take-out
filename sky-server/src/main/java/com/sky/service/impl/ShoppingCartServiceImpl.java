package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO){
        //DTO 实体并且补上后端才知道的信息
        //从拦截器里面存在ThreadLocal里的id
        ShoppingCart shoppingCart=new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);

        Long userId= BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        //查：复用你的XML 里的那条动态条件查询
        //同时按userId+dishId/setmealId+dishflavor过滤

        List<ShoppingCart> list=shoppingCartMapper.list(shoppingCart);
        //如果不为空，已经存在，将数量加一

        if(list !=null && list.size()>0){
            ShoppingCart cart=list.get(0);
            cart.setNumber(cart.getNumber()+1);
            shoppingCartMapper.updateNumberById(cart);
        }
        //如果不存在则插入一条购物车数据
        else{
            Long dishId = shoppingCartDTO.getDishId();

            //判断添加的是菜品还是套餐
            if(dishId !=null){
                //本次添加到购物车的是菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setNumber(1);
                shoppingCart.setCreateTime(LocalDateTime.now());

            }else{
                //添加进来的是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setNumber(1);
                shoppingCart.setCreateTime(LocalDateTime.now());

            }
            shoppingCartMapper.insert(shoppingCart);
        }

    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> listShoppingCart(){
        ShoppingCart shoppingCart=new ShoppingCart();
        //只看自己的
        shoppingCart.setUserId(BaseContext.getCurrentId());
        return shoppingCartMapper.list(shoppingCart);
    }

    /**
     * 清空购物车
     */
    @Override
     public void cleanShoppingCart(){
         shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
     }

    /**
     * 删除购物车中一个商品(sub)
     */
    @Override

    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            //这一行是整个方法的分水岭:从这里开始,cart 才是"要动的那条数据",前面的 shoppingCart 已经完成它的使命了。
            if (cart.getNumber() == 1) {
                shoppingCartMapper.deleteById(cart.getId());     // 只剩1个再减 → 整行删掉
            } else {
                cart.setNumber(cart.getNumber() - 1);//还有多个数量减1
                shoppingCartMapper.updateNumberById(cart);//更新回数据库
            }
        }
        // 车里没有这件商品 → 什么都不做(静默)
    }

}
