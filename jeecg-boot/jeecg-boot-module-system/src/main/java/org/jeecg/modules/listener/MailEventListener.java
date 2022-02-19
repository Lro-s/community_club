package org.jeecg.modules.listener;

import cn.hutool.core.map.MapUtil;
import org.jeecg.modules.event.SendActivationMailEvent;
import org.jeecg.modules.system.util.MailClient;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年02月19日 19:17
 */
@Component
public class MailEventListener  {

    @Resource
    private MailClient mailClient;

    @EventListener(SendActivationMailEvent.class)
    public void onEvent(SendActivationMailEvent event) {
        Object source = event.getSource();
        if (source instanceof Map) {
            Map<String,Object> map = (Map) source;
            String email = MapUtil.getStr(map, "email");
            String subject = MapUtil.getStr(map, "subject");
            String content = MapUtil.getStr(map, "content");
            mailClient.sendMail(email,subject,content);
        }
    }

}
