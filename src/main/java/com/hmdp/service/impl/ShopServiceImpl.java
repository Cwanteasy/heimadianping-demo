package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        // Shop shop = queryWithPassThrough(id);
        // 互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在!");
        }
        // 返回
        return Result.ok(shop);
    }

    public Shop queryWithMutex(Long id) {
        //1.从redis查询商户缓存
        String shopJason = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
        //2.判断是否存在
        if (StrUtil.isNotBlank(shopJason)) {
            //3.存在,直接返回
            return JSONUtil.toBean(shopJason, Shop.class);
        }
        // 判断空值
        if (shopJason != null) {
            return null;
        }
        //4. 实现缓存重建
        //4.1.获取互斥锁
        Shop shop = null;
        try {
            boolean islock = tryLock("lock:key:" + id);
            //4.2.判断是否获取成功
            if (!islock) {
                //4.3.失败休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //4.4.存在根据Id查询数据库
            shop = getById(id);
            // 模拟重建的延时
            Thread.sleep(200);
            //5.不存在返回错误
            if (shop == null) {
                // 将空值写入redis
                stringRedisTemplate.opsForValue().set("cache:shop:" + id,"", 2L, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            //6.存在,写入redis
            stringRedisTemplate.opsForValue().set("cache:shop:" + id, JSONUtil.toJsonStr(shop), 30L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //7.释放互斥锁
            unlock("lock:key:" + id);
        }
        //8.返回
        return shop;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10L, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete("cache:shop:" + shop.getId());
        return Result.ok();
    }


}
