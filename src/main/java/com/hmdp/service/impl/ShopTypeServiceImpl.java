package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {
        // 第一次进来从redis中查询商铺类别
        String s = stringRedisTemplate.opsForValue().get("shop:type:");
        //判断是否存在,
        if(StrUtil.isNotBlank(s)){
            //存在我们就将查出来的s转为array数组, 查出来数组然后再转为ShopType对象接收。
            JSONArray objects = JSONUtil.parseArray(s);
            List<ShopType> shopTypeList = JSONUtil.toList(objects, ShopType.class);
            //返回
            return Result.ok(shopTypeList);
        }
        //不存在就查数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        //查出来存在，写入redis
        stringRedisTemplate.opsForValue().set("shop:type:", JSONUtil.parseArray(typeList).toString());
        return Result.ok(typeList);
    }
}
