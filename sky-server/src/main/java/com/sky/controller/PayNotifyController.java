package com.sky.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.service.OrderService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付回调
 * 真实微信支付:微信服务器会往这个地址推加密报文,需要用 apiV3Key 解密
 * 模拟/自测:直接 POST 明文 {"out_trade_no":"订单号"} 就能推进订单状态
 */
@RestController
@RequestMapping("/notify")
@Api(tags = "支付回调接口")
@Slf4j
public class PayNotifyController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/paySuccess")
    public String paySuccess(@RequestBody String body) {
        log.info("收到支付成功回调:{}", body);
        JSONObject jsonObject = JSON.parseObject(body);
        String outTradeNo = jsonObject.getString("out_trade_no");
        orderService.paySuccess(outTradeNo);
        return "{\"code\": \"SUCCESS\", \"message\": \"成功\"}";
    }
}