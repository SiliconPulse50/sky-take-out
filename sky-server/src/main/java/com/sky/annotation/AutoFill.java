package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * 自定义注解，用于标识某个方法需要进行字段功能的填充处理
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
//这是定义一个注解，既不是普通类也不是普通接口
public @interface AutoFill {
    //数据库操作类型：UPDATE INSERT, 公共字段
    OperationType value();
    //注解有一个叫value的值类型是OperationType(使用时必须穿入一个值）
}
