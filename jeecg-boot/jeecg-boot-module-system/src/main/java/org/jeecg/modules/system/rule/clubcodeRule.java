package org.jeecg.modules.system.rule;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年02月13日 20:24
 */

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.jeecg.common.handler.IFillRuleHandler;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 填值规则Demo：生成社团编码值
 * 【测试示例】
 */
public class clubcodeRule implements IFillRuleHandler {

    @Override
    public Object execute(JSONObject params, JSONObject formData) {
        String prefix = "club";
        //博客前缀默认为blob 如果规则参数不为空，则取自定义前缀
        if (params != null) {
            Object obj = params.get("prefix");
            if (obj != null) prefix = obj.toString();
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        int random = RandomUtils.nextInt(90) + 10;
        String value = prefix + format.format(new Date()) + random;
        // 根据formData的值的不同，生成不同的订单号
        String name = formData.getString("id");
        if (!StringUtils.isEmpty(name)) {
            value += name;
        }
        return value;
    }


}

