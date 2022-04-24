package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityMapper extends BaseMapper<Community>{

    /*
    查询社团信息
     */
    List<Community> findAllInfo();

    /*
    查询博客标签条数
     */
    List<tag> findtagInfo();

    /*
    查询博客标签条数
     */
    List<gradeinfo> findgradeInfo();

    /*
    查询当前用户是否有社团申请，如果没有返回社团申请前三热度给用户
     */
    List<communityname> findusernameinapplication(String usercode);

    /*
    返回社团申请中的社团名字前三热度给用户
     */
    List<topcommunity> topcommunityname();

    /*
    去个人信息表查是否有社团名称
     */
    String findcommunitynamefromsysuser(String usercode);


}
