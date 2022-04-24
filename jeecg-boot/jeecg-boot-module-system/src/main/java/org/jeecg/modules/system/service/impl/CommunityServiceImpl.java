package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.system.entity.*;
import org.jeecg.modules.system.mapper.CommunityMapper;
import org.jeecg.modules.system.mapper.SysUserMapper;
import org.jeecg.modules.system.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @Description: TODO
 * @author: scott
 * @date: 2022年03月28日 19:54
 */

@ContextConfiguration({"classpath:org/jeecg/modules/system/mapper/xml/CommunityMapper.xml"})
@Service
public class CommunityServiceImpl extends ServiceImpl<CommunityMapper,Community> implements CommunityService {


    @Resource
    public CommunityMapper communityMapper;


    /**
     * 查询社团表的信息
     * @return
     */
    public List<Community> findAllinfo() {
        return communityMapper.findAllInfo();
    }

    /**
     * 查询博客标签数
     * @return
     */
    public List<tag> findtagInfo() {
        return communityMapper.findtagInfo();
    }


    @Override
    public List<gradeinfo> findgradeInfo() {
        return communityMapper.findgradeInfo();
    }



    @Override
    public List<topcommunity> topcommunityname() {
        return communityMapper.topcommunityname();
    }

    @Override
    public List<communityname> findusernameinapplication(String usercode) {
        return communityMapper.findusernameinapplication(usercode);
    }

    @Override
    public String findcommunitynamefromsysuser(String usercode) {
        return communityMapper.findcommunitynamefromsysuser(usercode);
    }
}
