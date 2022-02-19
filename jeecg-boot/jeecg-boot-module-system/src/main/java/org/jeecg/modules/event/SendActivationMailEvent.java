package org.jeecg.modules.event;


import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.EventObject;
import java.util.Map;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年02月19日 19:08
 */
@Slf4j
public class SendActivationMailEvent extends EventObject {

    public SendActivationMailEvent(Object obj) {
        super(obj);
        log.info("发布激活邮件事件！");
    }
}
