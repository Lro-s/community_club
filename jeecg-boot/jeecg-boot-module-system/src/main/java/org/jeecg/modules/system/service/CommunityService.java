package org.jeecg.modules.system.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.system.entity.*;

import java.sql.SQLException;
import java.util.List;


public interface CommunityService extends IService<Community> {

    List<Community> findAllinfo();

    List<tag> findtagInfo();

    List<gradeinfo>findgradeInfo();

    List<topcommunity> topcommunityname();

    List<communityname> findusernameinapplication(String usercode);

    String findcommunitynamefromsysuser(String usercode);

}