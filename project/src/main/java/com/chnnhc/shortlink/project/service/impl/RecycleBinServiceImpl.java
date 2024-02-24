package com.chnnhc.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chnnhc.shortlink.project.dao.entity.ShortLinkDO;
import com.chnnhc.shortlink.project.dao.mapper.ShortLinkMapper;
import com.chnnhc.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.chnnhc.shortlink.project.dto.req.RecycleBinRemoveReqDTO;
import com.chnnhc.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.chnnhc.shortlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.chnnhc.shortlink.project.common.constant.RedisCacheConstant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;

@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper,ShortLinkDO> implements RecycleBinService {
  private final StringRedisTemplate stringRedisTemplate;

  @Override
  public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
    LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
        .eq(ShortLinkDO::getGid, requestParam.getGid())
        .eq(ShortLinkDO::getEnableStatus, 0)
        .eq(ShortLinkDO::getDelFlag, 0);
    ShortLinkDO shortLinkDO = ShortLinkDO.builder()
        .enableStatus(1)
        .build();
    baseMapper.update(shortLinkDO, updateWrapper);
    stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
  }

  @Override
  public void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam) {
    LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
        .eq(ShortLinkDO::getGid, requestParam.getGid())
        .eq(ShortLinkDO::getEnableStatus, 1)
        .eq(ShortLinkDO::getDelFlag, 0);
    ShortLinkDO shortLinkDO = ShortLinkDO.builder()
        .enableStatus(0)
        .build();
    baseMapper.update(shortLinkDO, updateWrapper);
    stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
  }

  @Override
  public void removeRecycleBin(RecycleBinRemoveReqDTO requestParam) {
    LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
        .eq(ShortLinkDO::getGid, requestParam.getGid())
        .eq(ShortLinkDO::getEnableStatus, 1)
        .eq(ShortLinkDO::getDelTime, 0L)
        .eq(ShortLinkDO::getDelFlag, 0);
    ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
        .delTime(System.currentTimeMillis())
        .build();
    delShortLinkDO.setDelFlag(1);
    baseMapper.update(delShortLinkDO, updateWrapper);
  }
}
