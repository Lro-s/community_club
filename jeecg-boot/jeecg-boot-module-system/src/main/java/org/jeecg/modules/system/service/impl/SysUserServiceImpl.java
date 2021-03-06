package org.jeecg.modules.system.service.impl;

import java.util.Date;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CacheConstant;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.system.vo.SysUserCacheInfo;
import org.jeecg.common.util.*;
import org.jeecg.modules.base.service.BaseCommonService;
import org.jeecg.modules.event.SendActivationMailEvent;
import org.jeecg.modules.system.constant.UserConstants;
import org.jeecg.modules.system.dto.UserRegisterDTO;
import org.jeecg.modules.system.entity.*;
import org.jeecg.modules.system.mapper.*;
import org.jeecg.modules.system.model.SysUserSysDepartModel;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecg.modules.system.util.CommonUtil;
import org.jeecg.modules.system.util.MailClient;
import org.jeecg.modules.system.util.SecurityUtil;
import org.jeecg.modules.system.vo.SysUserDepVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;


import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @Author: scott
 * @Date: 2018-12-20
 */
@Service
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysUserDepartMapper sysUserDepartMapper;
    @Autowired
    private ISysBaseAPI sysBaseAPI;
    @Autowired
    private SysDepartMapper sysDepartMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysDepartRoleUserMapper departRoleUserMapper;
    @Autowired
    private SysDepartRoleMapper sysDepartRoleMapper;
    @Resource
    private BaseCommonService baseCommonService;

    @Resource
    private MailClient mailClient;

    @Resource
    private RedisUtil redisUtil;

    /**
     * 域名
     */
    @Value("${community.path.domain}")
    private String domain;

    /**
     * 项目名
     */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private ApplicationContext applicationContext;


    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    public Result<?> resetPassword(String username, String oldpassword, String newpassword, String confirmpassword) {
        SysUser user = userMapper.getUserByName(username);
        String passwordEncode = PasswordUtil.encrypt(username, oldpassword, user.getSalt());
        if (!user.getPassword().equals(passwordEncode)) {
            return Result.error("旧密码输入错误!");
        }
        if (oConvertUtils.isEmpty(newpassword)) {
            return Result.error("新密码不允许为空!");
        }
        if (!newpassword.equals(confirmpassword)) {
            return Result.error("两次输入密码不一致!");
        }
        String password = PasswordUtil.encrypt(username, newpassword, user.getSalt());
        this.userMapper.update(new SysUser().setPassword(password), new LambdaQueryWrapper<SysUser>().eq(SysUser::getId, user.getId()));
        return Result.ok("密码重置成功!");
    }

    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    public Result<?> changePassword(SysUser sysUser) {
        String salt = oConvertUtils.randomGen(8);
        sysUser.setSalt(salt);
        String password = sysUser.getPassword();
        String passwordEncode = PasswordUtil.encrypt(sysUser.getUsername(), password, salt);
        sysUser.setPassword(passwordEncode);
        this.userMapper.updateById(sysUser);
        return Result.ok("密码修改成功!");
    }

    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(String userId) {
        //1.删除用户
        this.removeById(userId);
        return false;
    }

    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatchUsers(String userIds) {
        //1.删除用户
        this.removeByIds(Arrays.asList(userIds.split(",")));
        return false;
    }

    @Override
    public SysUser getUserByName(String username) {
        return userMapper.getUserByName(username);
    }


    @Override
    @Transactional
    public void addUserWithRole(SysUser user, String roles) {
        this.save(user);
        if (oConvertUtils.isNotEmpty(roles)) {
            String[] arr = roles.split(",");
            for (String roleId : arr) {
                SysUserRole userRole = new SysUserRole(user.getId(), roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    @Transactional
    public void editUserWithRole(SysUser user, String roles) {
        this.updateById(user);
        //先删后加
        sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().lambda().eq(SysUserRole::getUserId, user.getId()));
        if (oConvertUtils.isNotEmpty(roles)) {
            String[] arr = roles.split(",");
            for (String roleId : arr) {
                SysUserRole userRole = new SysUserRole(user.getId(), roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }


    @Override
    public List<String> getRole(String username) {
        return sysUserRoleMapper.getRoleByUserName(username);
    }

    /**
     * 通过用户名获取用户角色集合
     *
     * @param username 用户名
     * @return 角色集合
     */
    @Override
    public Set<String> getUserRolesSet(String username) {
        // 查询用户拥有的角色集合
        List<String> roles = sysUserRoleMapper.getRoleByUserName(username);
        log.info("-------通过数据库读取用户拥有的角色Rules------username： " + username + ",Roles size: " + (roles == null ? 0 : roles.size()));
        return new HashSet<>(roles);
    }

    /**
     * 通过用户名获取用户权限集合
     *
     * @param username 用户名
     * @return 权限集合
     */
    @Override
    public Set<String> getUserPermissionsSet(String username) {
        Set<String> permissionSet = new HashSet<>();
        List<SysPermission> permissionList = sysPermissionMapper.queryByUser(username);
        for (SysPermission po : permissionList) {
//			// TODO URL规则有问题？
//			if (oConvertUtils.isNotEmpty(po.getUrl())) {
//				permissionSet.add(po.getUrl());
//			}
            if (oConvertUtils.isNotEmpty(po.getPerms())) {
                permissionSet.add(po.getPerms());
            }
        }
        log.info("-------通过数据库读取用户拥有的权限Perms------username： " + username + ",Perms size: " + (permissionSet == null ? 0 : permissionSet.size()));
        return permissionSet;
    }

    @Override
    public SysUserCacheInfo getCacheUser(String username) {
        SysUserCacheInfo info = new SysUserCacheInfo();
        info.setOneDepart(true);
//		SysUser user = userMapper.getUserByName(username);
//		info.setSysUserCode(user.getUsername());
//		info.setSysUserName(user.getRealname());


        LoginUser user = sysBaseAPI.getUserByName(username);
        if (user != null) {
            info.setSysUserCode(user.getUsername());
            info.setSysUserName(user.getRealname());
            info.setSysOrgCode(user.getOrgCode());
        }

        //多部门支持in查询
        List<SysDepart> list = sysDepartMapper.queryUserDeparts(user.getId());
        List<String> sysMultiOrgCode = new ArrayList<String>();
        if (list == null || list.size() == 0) {
            //当前用户无部门
            //sysMultiOrgCode.add("0");
        } else if (list.size() == 1) {
            sysMultiOrgCode.add(list.get(0).getOrgCode());
        } else {
            info.setOneDepart(false);
            for (SysDepart dpt : list) {
                sysMultiOrgCode.add(dpt.getOrgCode());
            }
        }
        info.setSysMultiOrgCode(sysMultiOrgCode);

        return info;
    }

    // 根据部门Id查询
    @Override
    public IPage<SysUser> getUserByDepId(Page<SysUser> page, String departId, String username) {
        return userMapper.getUserByDepId(page, departId, username);
    }

    @Override
    public IPage<SysUser> getUserByDepIds(Page<SysUser> page, List<String> departIds, String username) {
        return userMapper.getUserByDepIds(page, departIds, username);
    }

    @Override
    public Map<String, String> getDepNamesByUserIds(List<String> userIds) {
        List<SysUserDepVo> list = this.baseMapper.getDepNamesByUserIds(userIds);

        Map<String, String> res = new HashMap<String, String>();
        list.forEach(item -> {
                    if (res.get(item.getUserId()) == null) {
                        res.put(item.getUserId(), item.getDepartName());
                    } else {
                        res.put(item.getUserId(), res.get(item.getUserId()) + "," + item.getDepartName());
                    }
                }
        );
        return res;
    }

    @Override
    public IPage<SysUser> getUserByDepartIdAndQueryWrapper(Page<SysUser> page, String departId, QueryWrapper<SysUser> queryWrapper) {
        LambdaQueryWrapper<SysUser> lambdaQueryWrapper = queryWrapper.lambda();

        lambdaQueryWrapper.eq(SysUser::getDelFlag, CommonConstant.DEL_FLAG_0);
        lambdaQueryWrapper.inSql(SysUser::getId, "SELECT user_id FROM sys_user_depart WHERE dep_id = '" + departId + "'");

        return userMapper.selectPage(page, lambdaQueryWrapper);
    }

    @Override
    public IPage<SysUserSysDepartModel> queryUserByOrgCode(String orgCode, SysUser userParams, IPage page) {
        List<SysUserSysDepartModel> list = baseMapper.getUserByOrgCode(page, orgCode, userParams);
        Integer total = baseMapper.getUserByOrgCodeTotal(orgCode, userParams);

        IPage<SysUserSysDepartModel> result = new Page<>(page.getCurrent(), page.getSize(), total);
        result.setRecords(list);

        return result;
    }

    // 根据角色Id查询
    @Override
    public IPage<SysUser> getUserByRoleId(Page<SysUser> page, String roleId, String username) {
        return userMapper.getUserByRoleId(page, roleId, username);
    }


    @Override
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, key = "#username")
    public void updateUserDepart(String username, String orgCode) {
        baseMapper.updateUserDepart(username, orgCode);
    }


    @Override
    public SysUser getUserByPhone(String phone) {
        return userMapper.getUserByPhone(phone);
    }


    @Override
    public SysUser getUserByEmail(String email) {
        return userMapper.getUserByEmail(email);
    }

    @Override
    @Transactional
    public void addUserWithDepart(SysUser user, String selectedParts) {
//		this.save(user);  //保存角色的时候已经添加过一次了
        if (oConvertUtils.isNotEmpty(selectedParts)) {
            String[] arr = selectedParts.split(",");
            for (String deaprtId : arr) {
                SysUserDepart userDeaprt = new SysUserDepart(user.getId(), deaprtId);
                sysUserDepartMapper.insert(userDeaprt);
            }
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    public void editUserWithDepart(SysUser user, String departs) {
        this.updateById(user);  //更新角色的时候已经更新了一次了，可以再跟新一次
        String[] arr = {};
        if (oConvertUtils.isNotEmpty(departs)) {
            arr = departs.split(",");
        }
        //查询已关联部门
        List<SysUserDepart> userDepartList = sysUserDepartMapper.selectList(new QueryWrapper<SysUserDepart>().lambda().eq(SysUserDepart::getUserId, user.getId()));
        if (userDepartList != null && userDepartList.size() > 0) {
            for (SysUserDepart depart : userDepartList) {
                //修改已关联部门删除部门用户角色关系
                if (!Arrays.asList(arr).contains(depart.getDepId())) {
                    List<SysDepartRole> sysDepartRoleList = sysDepartRoleMapper.selectList(
                            new QueryWrapper<SysDepartRole>().lambda().eq(SysDepartRole::getDepartId, depart.getDepId()));
                    List<String> roleIds = sysDepartRoleList.stream().map(SysDepartRole::getId).collect(Collectors.toList());
                    if (roleIds != null && roleIds.size() > 0) {
                        departRoleUserMapper.delete(new QueryWrapper<SysDepartRoleUser>().lambda().eq(SysDepartRoleUser::getUserId, user.getId())
                                .in(SysDepartRoleUser::getDroleId, roleIds));
                    }
                }
            }
        }
        //先删后加
        sysUserDepartMapper.delete(new QueryWrapper<SysUserDepart>().lambda().eq(SysUserDepart::getUserId, user.getId()));
        if (oConvertUtils.isNotEmpty(departs)) {
            for (String departId : arr) {
                SysUserDepart userDepart = new SysUserDepart(user.getId(), departId);
                sysUserDepartMapper.insert(userDepart);
            }
        }
    }


    /**
     * 校验用户是否有效
     *
     * @param sysUser
     * @return
     */
    @Override
    public Result<?> checkUserIsEffective(SysUser sysUser) {
        Result<?> result = new Result<Object>();
        //情况1：根据用户信息查询，该用户不存在
        if (sysUser == null) {
            result.error500("该用户不存在，请注册");
            baseCommonService.addLog("用户登录失败，用户不存在！", CommonConstant.LOG_TYPE_1, null);
            return result;
        }
        //情况2：根据用户信息查询，该用户已注销
        //update-begin---author:王帅   Date:20200601  for：if条件永远为falsebug------------
        if (CommonConstant.DEL_FLAG_1.equals(sysUser.getDelFlag())) {
            //update-end---author:王帅   Date:20200601  for：if条件永远为falsebug------------
            baseCommonService.addLog("用户登录失败，用户名:" + sysUser.getUsername() + "已注销！", CommonConstant.LOG_TYPE_1, null);
            result.error500("该用户已注销");
            return result;
        }
        //情况3：根据用户信息查询，该用户已冻结
        if (CommonConstant.USER_FREEZE.equals(sysUser.getStatus())) {
            baseCommonService.addLog("用户登录失败，用户名:" + sysUser.getUsername() + "已冻结！", CommonConstant.LOG_TYPE_1, null);
            result.error500("该用户已冻结");
            return result;
        }
        return result;
    }

    @Override
    public List<SysUser> queryLogicDeleted() {
        return this.queryLogicDeleted(null);
    }

    @Override
    public List<SysUser> queryLogicDeleted(LambdaQueryWrapper<SysUser> wrapper) {
        if (wrapper == null) {
            wrapper = new LambdaQueryWrapper<>();
        }
        wrapper.eq(SysUser::getDelFlag, CommonConstant.DEL_FLAG_1);
        return userMapper.selectLogicDeleted(wrapper);
    }

    @Override
    public boolean revertLogicDeleted(List<String> userIds, SysUser updateEntity) {
        String ids = String.format("'%s'", String.join("','", userIds));
        return userMapper.revertLogicDeleted(ids, updateEntity) > 0;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateNullPhoneEmail() {
        userMapper.updateNullByEmptyString("email");
        userMapper.updateNullByEmptyString("phone");
        return true;
    }


    @Override
    public List<SysUser> queryByDepIds(List<String> departIds, String username) {
        return userMapper.queryByDepIds(departIds, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUser(SysUser user, String selectedRoles, String selectedDeparts) {
        //step.1 保存用户
        this.save(user);
        //step.2 保存角色
        if (oConvertUtils.isNotEmpty(selectedRoles)) {
            String[] arr = selectedRoles.split(",");
            for (String roleId : arr) {
                SysUserRole userRole = new SysUserRole(user.getId(), roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
        //step.3 保存所属部门
        if (oConvertUtils.isNotEmpty(selectedDeparts)) {
            String[] arr = selectedDeparts.split(",");
            for (String deaprtId : arr) {
                SysUserDepart userDeaprt = new SysUserDepart(user.getId(), deaprtId);
                sysUserDepartMapper.insert(userDeaprt);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {CacheConstant.SYS_USERS_CACHE}, allEntries = true)
    public void editUser(SysUser user, String roles, String departs) {
        //step.1 修改用户基础信息
        this.updateById(user);
        //step.2 修改角色
        //处理用户角色 先删后加
        sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().lambda().eq(SysUserRole::getUserId, user.getId()));
        if (oConvertUtils.isNotEmpty(roles)) {
            String[] arr = roles.split(",");
            for (String roleId : arr) {
                SysUserRole userRole = new SysUserRole(user.getId(), roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }

        //step.3 修改部门
        String[] arr = {};
        if (oConvertUtils.isNotEmpty(departs)) {
            arr = departs.split(",");
        }
        //查询已关联部门
        List<SysUserDepart> userDepartList = sysUserDepartMapper.selectList(new QueryWrapper<SysUserDepart>().lambda().eq(SysUserDepart::getUserId, user.getId()));
        if (userDepartList != null && userDepartList.size() > 0) {
            for (SysUserDepart depart : userDepartList) {
                //修改已关联部门删除部门用户角色关系
                if (!Arrays.asList(arr).contains(depart.getDepId())) {
                    List<SysDepartRole> sysDepartRoleList = sysDepartRoleMapper.selectList(
                            new QueryWrapper<SysDepartRole>().lambda().eq(SysDepartRole::getDepartId, depart.getDepId()));
                    List<String> roleIds = sysDepartRoleList.stream().map(SysDepartRole::getId).collect(Collectors.toList());
                    if (roleIds != null && roleIds.size() > 0) {
                        departRoleUserMapper.delete(new QueryWrapper<SysDepartRoleUser>().lambda().eq(SysDepartRoleUser::getUserId, user.getId())
                                .in(SysDepartRoleUser::getDroleId, roleIds));
                    }
                }
            }
        }
        //先删后加
        sysUserDepartMapper.delete(new QueryWrapper<SysUserDepart>().lambda().eq(SysUserDepart::getUserId, user.getId()));
        if (oConvertUtils.isNotEmpty(departs)) {
            for (String departId : arr) {
                SysUserDepart userDepart = new SysUserDepart(user.getId(), departId);
                sysUserDepartMapper.insert(userDepart);
            }
        }
        //step.4 修改手机号和邮箱
        // 更新手机号、邮箱空字符串为 null
        userMapper.updateNullByEmptyString("email");
        userMapper.updateNullByEmptyString("phone");

    }

    @Override
    public List<String> userIdToUsername(Collection<String> userIdList) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SysUser::getId, userIdList);
        List<SysUser> userList = super.list(queryWrapper);
        return userList.stream().map(SysUser::getUsername).collect(Collectors.toList());
    }

    //实现邮箱注册接口类
    @Override
    @Transactional
    public Result<JSONObject> emailRegister(UserRegisterDTO userRegisterDTO) {
        Result<JSONObject> result = new Result<>();
        //校驗前端傳值
        checkRegisterInfo(userRegisterDTO, result);


        //註冊用戶邏輯
        registerUser(userRegisterDTO);
        result.setMessage("操作成功！");
        result.setSuccess(true);
        return result;
    }

    private void checkRegisterInfo(UserRegisterDTO userRegisterDTO, Result<JSONObject> result) {
        //校驗用戶名是否已存在
        checkUserName(userRegisterDTO, result);
        //校驗郵箱是否已存在
        checkEmail(userRegisterDTO, result);
    }

    //注册用户
    private void registerUser(UserRegisterDTO userRegisterDTO) {
        SysUser sysUser = null;
        try {
            //創建用戶
            sysUser = createUser(userRegisterDTO);
            saveActivationCodeInRedis(sysUser);
        }catch (Exception e) {
            throw e;
        }
        publishEvent(sysUser);

        //这里不能用@async来做，这样就相当于同步了，这里可以用spring的事件监听机制来实现监听发送邮件
        //發送郵件
//        sendMessage(sysUser);

    }

    @Override
    public Boolean studentRoleInsert(String userid) {
        Boolean aBoolean = userMapper.studentRoleInsert(userid);
        return aBoolean;
    }


    //邮箱内容
    private void publishEvent(SysUser sysUser) {
        Map<String,Object> map = new HashMap<>();
        String url = domain + contextPath + "/sys/user/activation/" + sysUser.getId() + "/" + sysUser.getActivationCode();
//		String activationCode = sysUser.getActivationCode();
        String username = sysUser.getUsername();

        String content = "亲爱的 " + username + " 你好，请点击下方的链接对您的账号进行激活";
        String turn = "<br/>";
        String htmlUrl = "<a href=" + url + ">激活链接</a>";

        map.put("email",sysUser.getEmail());
        map.put("subject","激活账号链接");
        map.put("content",content + turn + htmlUrl);
        applicationContext.publishEvent(new SendActivationMailEvent(map));
    }




    //邮箱内容 忘记密码发送邮箱验证码的
    public Boolean publishEvent1(SysUser sysUser,String captcha) {
        Map<String,Object> map = new HashMap<>();

        String username = sysUser.getUsername();

        String content = "亲爱的 " + username + " 你好，你的验证码为";
        String turn = "<br/>";

        map.put("email",sysUser.getEmail());
        map.put("subject","更改账号密码");
        map.put("content",content + turn + captcha);
        applicationContext.publishEvent(new SendActivationMailEvent(map));
        return true;

    }

    private void saveActivationCodeInRedis(SysUser sysUser) {
        String activationCode = sysUser.getActivationCode();
        //十分钟过期
        redisUtil.set(sysUser.getId(), activationCode, 600);
    }

    /**
     * 异步发送消息
     *
     * @param sysUser
     */
    protected void sendMessage(SysUser sysUser) {
        String url = domain + contextPath + "/sys/user/activation/" + sysUser.getId() + "/" + sysUser.getActivationCode();
//		String activationCode = sysUser.getActivationCode();
        String username = sysUser.getUsername();

        String content = "亲爱的 " + username + " 你好，请点击下方的链接对您的账号进行激活";
        String turn = "<br/>";
        String htmlUrl = "<a href=" + url + ">激活链接</a>";

        mailClient.sendMail(sysUser.getEmail(), "激活账号链接", content + turn + htmlUrl);
    }

    //新增用户方法
    private SysUser createUser(UserRegisterDTO userRegisterDTO) {
        SysUser sysUser = new SysUser();
        sysUser.setEmail(userRegisterDTO.getEmail());
        sysUser.setUsername(userRegisterDTO.getUsername());
        String salt = oConvertUtils.randomGen(8);
        String passwordEncode = PasswordUtil.encrypt(userRegisterDTO.getUsername(), userRegisterDTO.getPassword(), salt);
        //生成5位的随机数作为盐值
        sysUser.setSalt(salt);
        //密码盐值加密
        sysUser.setPassword(passwordEncode);
        //发送随机字符串作为激活码
        sysUser.setActivationCode(IdUtil.fastUUID());
        //设置随机头像  这里使用牛客网的随机头像
        sysUser.setAvatar(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        //设置账号默认为冻结状态
        sysUser.setStatus(2);
        //由超级管理员创建
        sysUser.setCreateBy(UserConstants.ADMIN);
        sysUser.setCreateTime(new Date());
        //普通人
        sysUser.setUserIdentity(0);
        baseMapper.insert(sysUser);
        return sysUser;
    }

    /**
     * 校验邮箱
     *
     * @param userRegisterDTO
     * @param result
     */
    private void checkEmail(UserRegisterDTO userRegisterDTO, Result<JSONObject> result) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper();
        wrapper.eq("email", userRegisterDTO.getEmail());
        SysUser sysUser = baseMapper.selectOne(wrapper);
        if (Objects.nonNull(sysUser)) {
            result.setMessage("邮箱已注册！");
            result.setSuccess(false);
        }
    }

    /**
     * 校验用户名
     *
     * @param userRegisterDTO
     * @param result
     */
    private void checkUserName(UserRegisterDTO userRegisterDTO, Result<JSONObject> result) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper();
        wrapper.eq("username", userRegisterDTO.getUsername());
        List<SysUser> sysUserList = baseMapper.selectList(wrapper);
        if (CollectionUtil.isNotEmpty(sysUserList)) {
            result.setMessage("用户名已存在！");
            result.setSuccess(false);
        }
    }

    @Override
    public int activation(String userId, String activationCode) {
        SysUser sysUser = new SysUser();
        checkActivation(userId);
        sysUser = checkUser(userId, sysUser);
        return activation(sysUser, activationCode);
    }

    private int activation(SysUser sysUser, String activationCode) {
        if (Objects.nonNull(sysUser.getStatus()) & sysUser.getStatus() == 1) {
            return UserConstants.ACTIVATION_REPEAT;
        }else if (sysUser.getActivationCode().equals(activationCode)) {
            baseMapper.activationEmail(sysUser);
//			baseMapper.update(sysUser,new UpdateWrapper<SysUser>()
//					.lambda()
//					.set(SysUser::getStatus, 1)
//					.set(SysUser::getDelFlag, 0)
//					.eq(SysUser::getId,sysUser.getId()) );
            baseMapper.activationEmail(sysUser);
            return UserConstants.ACTIVATION_SUCCESS;
        }
        return UserConstants.ACTIVATION_FAILURE;
    }

    private SysUser checkUser(String userId, SysUser sysUser) {
        sysUser = baseMapper.selectByUserId(userId);
        Objects.requireNonNull(sysUser, "用户不存在！");
        return sysUser;
    }

    private void checkActivation(String userId) {
        Object redisActivationCode = redisUtil.get(userId);
        Objects.requireNonNull(redisActivationCode, "验证码已过期！");
    }

    @Override
    public SysUser findByEmail(String email) {
        SysUser sysUser = baseMapper.findUserByEmail(email);
        return sysUser;
    }

    @Override
    public boolean removeLogicDeleted(List<String> userIds) {
        return false;
    }

}
