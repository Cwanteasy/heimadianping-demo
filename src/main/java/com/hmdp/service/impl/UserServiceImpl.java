package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexPatterns;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合,返回错误信息
            return Result.fail("手机号格式错误");
        }

        // 3.符合,生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.保存验证码到redis
        stringRedisTemplate.opsForValue().set("login:code:" + phone, code, 2, TimeUnit.MINUTES);

        // 5.发送验证码
        log.debug("发送短信验证码成功,验证码:{}", code);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //不符合,返回错误信息
            return Result.fail("手机号格式错误");
        }
        // 2.校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get("login:code:" + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 3.不一致,报错
            return Result.fail("验证码错误");
        }
        // 4.一致,根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 5.判断用户是否存在
        if (user == null) {
            //6. 不存在,创建用户并保存
            user = CreateUserWithphone(phone);
        }
        //7. 保存用户信息到redis
        //7.1 随机生成token作为登陆令牌
        String token = UUID.randomUUID().toString(true);
        //7.2 将UserDTO对象转换为Hashmap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().
                        setIgnoreNullValue(true).
                        setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //7.3 存储
        stringRedisTemplate.opsForHash().putAll("login:token:" + token,userMap);
        //7.4 设置token有效期
        stringRedisTemplate.expire("login:token:" + token, 30, TimeUnit.MINUTES);
        //8.返回token
        return Result.ok(token) ;
    }

    private User CreateUserWithphone(String phone) {
        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(6));
        //保存用户
        save(user);
        return user;
    }
}
