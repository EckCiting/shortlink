package com.chnnhc.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrBuilder;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chnnhc.shortlink.project.common.convention.exception.ClientException;
import com.chnnhc.shortlink.project.common.convention.exception.ServiceException;
import com.chnnhc.shortlink.project.common.enums.VailDateTypeEnum;
import com.chnnhc.shortlink.project.dao.entity.*;
import com.chnnhc.shortlink.project.dao.mapper.*;
import com.chnnhc.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.chnnhc.shortlink.project.dto.resp.*;
import com.chnnhc.shortlink.project.service.LinkStatsTodayService;
import com.chnnhc.shortlink.project.service.ShortLinkService;

import com.chnnhc.shortlink.project.toolkit.HashUtil;
import com.chnnhc.shortlink.project.toolkit.LinkUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.redisson.api.RBloomFilter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.chnnhc.shortlink.project.common.constant.RedisCacheConstant.RedisKeyConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO>
    implements ShortLinkService {

  private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
  private final ShortLinkGotoMapper shortLinkGotoMapper;
  private final StringRedisTemplate stringRedisTemplate;
  private final RedissonClient redissonClient;
  private final LinkAccessStatsMapper linkAccessStatsMapper;
  private final LinkLocaleStatsMapper linkLocaleStatsMapper;
  private final LinkOsStatsMapper linkOsStatsMapper;
  private final LinkBrowserStatsMapper linkBrowserStatsMapper;
  private final LinkAccessLogsMapper linkAccessLogsMapper;
  private final LinkDeviceStatsMapper linkDeviceStatsMapper;
  private final LinkNetworkStatsMapper linkNetworkStatsMapper;
  private final LinkStatsTodayMapper linkStatsTodayMapper;
  private final LinkStatsTodayService linkStatsTodayService;
  @Value("${short-link.domain.default}")
  private String createShortLinkDefaultDomain;

  @Value("${short-link.stats.locale.amap-key}")
  private String statsLocaleAmapKey; // 高德地图插件

  @Override
  public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
    // 生成短链接后缀
    String shortLinkSuffix = generateSuffix(requestParam);
    // 构造完整短链接URL
    String fullShortUrl =
        StrBuilder.create(createShortLinkDefaultDomain)
            .append("/")
            .append(shortLinkSuffix)
            .toString();
    // 创建短链接数据对象
    ShortLinkDO shortLinkDO =
        ShortLinkDO.builder()
            .domain(createShortLinkDefaultDomain) // 设置域名
            .originUrl(requestParam.getOriginUrl()) // 设置原始URL
            .gid(requestParam.getGid()) // 设置全局唯一标识符
            .createdType(requestParam.getCreatedType()) // 设置创建类型
            .validDateType(requestParam.getValidDateType()) // 设置有效日期类型
            .validDate(requestParam.getValidDate()) // 设置有效日期
            .describe(requestParam.getDescribe()) // 设置描述
            .shortUri(shortLinkSuffix) // 设置短链接后缀
            .enableStatus(0) // 设置启用状态
            .totalPv(0) // 设置总页面访问量
            .totalUv(0) // 设置总用户访问量
            .totalUip(0) // 设置总独立IP访问量
            .delTime(0L) // 设置删除时间
            .fullShortUrl(fullShortUrl) // 设置完整短链接URL
            .favicon(getFavicon(requestParam.getOriginUrl())) // 设置网页图标
            .build();

    ShortLinkGotoDO linkGotoDO =
        ShortLinkGotoDO.builder().fullShortUrl(fullShortUrl).gid(requestParam.getGid()).build();

    // 数据库操作
    try {
      baseMapper.insert(shortLinkDO); // 尝试插入短链接数据对象
      shortLinkGotoMapper.insert(linkGotoDO); // 尝试插入跳转信息
    } catch (DuplicateKeyException ex) { // 捕获键值重复异常
      // 查询数据库判断是否已存在相同的短链接
      LambdaQueryWrapper<ShortLinkDO> queryWrapper =
          Wrappers.lambdaQuery(ShortLinkDO.class).eq(ShortLinkDO::getFullShortUrl, fullShortUrl);
      ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
      if (hasShortLinkDO != null) {
        log.warn("短链接：{} 重复入库", fullShortUrl); // 记录警告
        throw new ServiceException("短链接生成重复"); // 抛出异常
      }
    }
    // 将完整短链接URL和原始URL存入Redis缓存，并设置过期时间
    stringRedisTemplate
        .opsForValue()
        .set(
            String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
            requestParam.getOriginUrl(),
            LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()),
            TimeUnit.MILLISECONDS);
    // 更新布隆过滤器，添加新生成的短链接，避免将来的缓存穿透问题
    shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);

    // 构造并返回创建短链接的响应对象，包含完整的短链接URL、原始URL和GID
    return ShortLinkCreateRespDTO.builder()
        .fullShortUrl(
            "http://" + shortLinkDO.getFullShortUrl()) // 注意：这里假设使用http协议，根据实际情况可能需要调整为https
        .originUrl(requestParam.getOriginUrl())
        .gid(requestParam.getGid())
        .build();
  }

  @Override
  public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
    QueryWrapper<ShortLinkDO> queryWrapper =
        Wrappers.query(new ShortLinkDO())
            .select("gid as gid, count(*) as shortLinkCount")
            .in("gid", requestParam)
            .eq("enable_status", 0)
            .eq("del_flag", 0)
            .eq("del_time", 0L)
            .groupBy("gid");
    List<Map<String, Object>> shortLinkDOList = baseMapper.selectMaps(queryWrapper);
    return BeanUtil.copyToList(shortLinkDOList, ShortLinkGroupCountQueryRespDTO.class);
  }

  @Override
  public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
    // 使用请求参数中的分页信息进行数据库查询
    IPage<ShortLinkDO> resultPage = baseMapper.pageLink(requestParam);
    // 使用IPage接口的convert方法将查询结果中的每一个ShortLinkDO对象转换为ShortLinkPageRespDTO对象
    // 在转换过程中，将domain字段修改为包含"http://"前缀的完整URL
    return resultPage.convert(
        each -> {
          ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
          result.setDomain("http://" + result.getDomain());
          return result;
        });
  }

  @Override
  public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
    // 从请求参数中获取原始URL列表
    List<String> originUrls = requestParam.getOriginUrls();
    // 从请求参数中获取描述信息列表
    List<String> describes = requestParam.getDescribes();
    // 初始化用于存储短链接基本信息的列表
    List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
    // 遍历原始URL列表
    for (int i = 0; i < originUrls.size(); i++) {
      // 复制 gid, createdType, validDateType, validDate
      ShortLinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(requestParam, ShortLinkCreateReqDTO.class);
      // 设置当前遍历到的原始URL
      shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
      // 设置当前遍历到的描述信息
      shortLinkCreateReqDTO.setDescribe(describes.get(i));
      try { // 这里的单个短链接创建失败，不会影响其他短链接的创建
        // 调用创建短链接的方法，并获取返回结果
        ShortLinkCreateRespDTO shortLink = createShortLink(shortLinkCreateReqDTO);
        // 构建短链接基本信息对象，并添加到结果列表中
        ShortLinkBaseInfoRespDTO linkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
            .fullShortUrl(shortLink.getFullShortUrl())
            .originUrl(shortLink.getOriginUrl())
            .describe(describes.get(i))
            .build();
        result.add(linkBaseInfoRespDTO);
      } catch (Throwable ex) {
        // 如果创建短链接过程中发生异常，则记录错误日志
        log.error(String.valueOf(ex));
        log.error("批量创建短链接失败，原始参数：{}", originUrls.get(i));
      }
    }
    // 构建并返回批量创建短链接的响应对象
    return ShortLinkBatchCreateRespDTO.builder()
        .total(result.size())
        .baseLinkInfos(result)
        .build();
  }


  // 生成短链接后缀
  private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
    int customGenerateCount = 0; // 尝试生成短链接的次数
    String shorUri; // 最终生成的短链接后缀
    while (true) {
      // 如果尝试次数超过10次，则抛出异常
      if (customGenerateCount > 10) {
        throw new ServiceException("短链接频繁生成，请稍后再试");
      }
      String originUrl = requestParam.getOriginUrl(); // 获取原始URL
      originUrl += System.currentTimeMillis(); // 添加当前时间戳以确保唯一性
      shorUri = HashUtil.hashToBase62(originUrl); // 将带时间戳的URL转换为62进制哈希值作为短链接后缀
      // 检查布隆过滤器中是否已存在该短链接，以防缓存穿透
      if (!shortUriCreateCachePenetrationBloomFilter.contains(
          createShortLinkDefaultDomain + "/" + shorUri)) {
        break; // 如果不存在，结束循环
      }
      customGenerateCount++; // 递增尝试次数并重新尝试
    }
    return shorUri; // 返回生成的短链接后缀
  }

  @SneakyThrows
  private String getFavicon(String url) {
    // 将字符串URL转换为URL对象
    URL targetUrl = new URL(url);
    // 打开到URL的连接
    HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
    // 设置请求方法为GET
    connection.setRequestMethod("GET");
    // 建立到URL的连接
    connection.connect();
    // 获取从连接返回的响应码
    int responseCode = connection.getResponseCode();
    // 检查响应码是否表示成功（HTTP 200 OK）
    if (HttpURLConnection.HTTP_OK == responseCode) {
      // 使用Jsoup连接到URL并解析其HTML内容
      Document document = Jsoup.connect(url).get();
      // 选择第一个具有'icon'或'shortcut icon'关系的链接元素
      // 正则表达式允许对'icon'或'shortcut icon'进行不区分大小写的匹配
      Element faviconLink = document.select("link[rel~=(?i)^(shortcut )?icon]").first();
      // 如果找到了favicon链接，则返回其绝对URL
      if (faviconLink != null) {
        return faviconLink.attr("abs:href");
      }
    }
    // 如果没有找到favicon，或响应不是HTTP OK，则返回null
    return null;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
    LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
        .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
        .eq(ShortLinkDO::getDelFlag, 0)
        .eq(ShortLinkDO::getEnableStatus, 0);
    ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
    if (hasShortLinkDO == null) {
      throw new ClientException("短链接记录不存在");
    }
    if (Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())) {
      LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
          .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
          .eq(ShortLinkDO::getGid, requestParam.getGid())
          .eq(ShortLinkDO::getDelFlag, 0)
          .eq(ShortLinkDO::getEnableStatus, 0)
          .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
      ShortLinkDO shortLinkDO = ShortLinkDO.builder()
          .domain(hasShortLinkDO.getDomain())
          .shortUri(hasShortLinkDO.getShortUri())
          .favicon(hasShortLinkDO.getFavicon())
          .createdType(hasShortLinkDO.getCreatedType())
          .gid(requestParam.getGid())
          .originUrl(requestParam.getOriginUrl())
          .describe(requestParam.getDescribe())
          .validDateType(requestParam.getValidDateType())
          .validDate(requestParam.getValidDate())
          .build();
      baseMapper.update(shortLinkDO, updateWrapper);
    } else {
      RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
      RLock rLock = readWriteLock.writeLock();
      if (!rLock.tryLock()) {
        throw new ServiceException("短链接正在被访问，请稍后再试...");
      }
      try {
        LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
            .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
            .eq(ShortLinkDO::getDelFlag, 0)
            .eq(ShortLinkDO::getDelTime, 0L)
            .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
            .delTime(System.currentTimeMillis())
            .build();
        delShortLinkDO.setDelFlag(1);
        baseMapper.update(delShortLinkDO, linkUpdateWrapper);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
            .domain(createShortLinkDefaultDomain)
            .originUrl(requestParam.getOriginUrl())
            .gid(requestParam.getGid())
            .createdType(hasShortLinkDO.getCreatedType())
            .validDateType(requestParam.getValidDateType())
            .validDate(requestParam.getValidDate())
            .describe(requestParam.getDescribe())
            .shortUri(hasShortLinkDO.getShortUri())
            .enableStatus(hasShortLinkDO.getEnableStatus())
            .totalPv(hasShortLinkDO.getTotalPv())
            .totalUv(hasShortLinkDO.getTotalUv())
            .totalUip(hasShortLinkDO.getTotalUip())
            .fullShortUrl(hasShortLinkDO.getFullShortUrl())
            .favicon(getFavicon(requestParam.getOriginUrl()))
            .delTime(0L)
            .build();
        baseMapper.insert(shortLinkDO);
        LambdaQueryWrapper<LinkStatsTodayDO> statsTodayQueryWrapper = Wrappers.lambdaQuery(LinkStatsTodayDO.class)
            .eq(LinkStatsTodayDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(LinkStatsTodayDO::getGid, hasShortLinkDO.getGid())
            .eq(LinkStatsTodayDO::getDelFlag, 0);
        List<LinkStatsTodayDO> linkStatsTodayDOList = linkStatsTodayMapper.selectList(statsTodayQueryWrapper);
        if (CollUtil.isNotEmpty(linkStatsTodayDOList)) {
          linkStatsTodayMapper.deleteBatchIds(linkStatsTodayDOList.stream()
              .map(LinkStatsTodayDO::getId)
              .toList()
          );
          linkStatsTodayDOList.forEach(each -> each.setGid(requestParam.getGid()));
          linkStatsTodayService.saveBatch(linkStatsTodayDOList);
        }
        LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
            .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(ShortLinkGotoDO::getGid, hasShortLinkDO.getGid());
        ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
        shortLinkGotoMapper.deleteById(shortLinkGotoDO.getId());
        shortLinkGotoDO.setGid(requestParam.getGid());
        shortLinkGotoMapper.insert(shortLinkGotoDO);
        LambdaUpdateWrapper<LinkAccessStatsDO> linkAccessStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessStatsDO.class)
            .eq(LinkAccessStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(LinkAccessStatsDO::getGid, hasShortLinkDO.getGid())
            .eq(LinkAccessStatsDO::getDelFlag, 0);
        LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
            .gid(requestParam.getGid())
            .build();
        linkAccessStatsMapper.update(linkAccessStatsDO, linkAccessStatsUpdateWrapper);
        LambdaUpdateWrapper<LinkLocaleStatsDO> linkLocaleStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkLocaleStatsDO.class)
            .eq(LinkLocaleStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(LinkLocaleStatsDO::getGid, hasShortLinkDO.getGid())
            .eq(LinkLocaleStatsDO::getDelFlag, 0);
        LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
            .gid(requestParam.getGid())
            .build();
        linkLocaleStatsMapper.update(linkLocaleStatsDO, linkLocaleStatsUpdateWrapper);
        LambdaUpdateWrapper<LinkOsStatsDO> linkOsStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkOsStatsDO.class)
            .eq(LinkOsStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(LinkOsStatsDO::getGid, hasShortLinkDO.getGid())
            .eq(LinkOsStatsDO::getDelFlag, 0);
        LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
            .gid(requestParam.getGid())
            .build();
        linkOsStatsMapper.update(linkOsStatsDO, linkOsStatsUpdateWrapper);
        LambdaUpdateWrapper<LinkBrowserStatsDO> linkBrowserStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkBrowserStatsDO.class)
            .eq(LinkBrowserStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(LinkBrowserStatsDO::getGid, hasShortLinkDO.getGid())
            .eq(LinkBrowserStatsDO::getDelFlag, 0);
        LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
            .gid(requestParam.getGid())
            .build();
        linkBrowserStatsMapper.update(linkBrowserStatsDO, linkBrowserStatsUpdateWrapper);
        LambdaUpdateWrapper<LinkDeviceStatsDO> linkDeviceStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkDeviceStatsDO.class)
            .eq(LinkDeviceStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(LinkDeviceStatsDO::getGid, hasShortLinkDO.getGid())
            .eq(LinkDeviceStatsDO::getDelFlag, 0);
        LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
            .gid(requestParam.getGid())
            .build();
        linkDeviceStatsMapper.update(linkDeviceStatsDO, linkDeviceStatsUpdateWrapper);
        LambdaUpdateWrapper<LinkNetworkStatsDO> linkNetworkStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkNetworkStatsDO.class)
            .eq(LinkNetworkStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(LinkNetworkStatsDO::getGid, hasShortLinkDO.getGid())
            .eq(LinkNetworkStatsDO::getDelFlag, 0);
        LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
            .gid(requestParam.getGid())
            .build();
        linkNetworkStatsMapper.update(linkNetworkStatsDO, linkNetworkStatsUpdateWrapper);
        LambdaUpdateWrapper<LinkAccessLogsDO> linkAccessLogsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessLogsDO.class)
            .eq(LinkAccessLogsDO::getFullShortUrl, requestParam.getFullShortUrl())
            .eq(LinkAccessLogsDO::getGid, hasShortLinkDO.getGid())
            .eq(LinkAccessLogsDO::getDelFlag, 0);
        LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
            .gid(requestParam.getGid())
            .build();
        linkAccessLogsMapper.update(linkAccessLogsDO, linkAccessLogsUpdateWrapper);
      } finally {
        rLock.unlock();
      }
    }
    if (!Objects.equals(hasShortLinkDO.getValidDateType(), requestParam.getValidDateType())
        || !Objects.equals(hasShortLinkDO.getValidDate(), requestParam.getValidDate())) {
      stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
      if (hasShortLinkDO.getValidDate() != null && hasShortLinkDO.getValidDate().before(new Date())) {
        if (Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()) || requestParam.getValidDate().after(new Date())) {
          stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
        }
      }
    }
  }

}
