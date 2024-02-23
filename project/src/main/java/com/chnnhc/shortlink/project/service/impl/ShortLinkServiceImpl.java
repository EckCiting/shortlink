package com.chnnhc.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chnnhc.shortlink.project.common.convention.exception.ServiceException;
import com.chnnhc.shortlink.project.dao.entity.ShortLinkDO;
import com.chnnhc.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.chnnhc.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.chnnhc.shortlink.project.dao.mapper.ShortLinkMapper;
import com.chnnhc.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.chnnhc.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.chnnhc.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.chnnhc.shortlink.project.service.ShortLinkService;

import com.chnnhc.shortlink.project.toolkit.HashUtil;
import com.chnnhc.shortlink.project.toolkit.LinkUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.redisson.api.RBloomFilter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.chnnhc.shortlink.project.common.constant.RedisCacheConstant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO>
    implements ShortLinkService {

  private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
  private final StringRedisTemplate stringRedisTemplate;

  private final ShortLinkGotoMapper shortLinkGotoMapper;

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
    IPage<ShortLinkDO> resultPage = baseMapper.pageLink(requestParam);
    return resultPage.convert(
        each -> {
          ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
          result.setDomain("http://" + result.getDomain());
          return result;
        });
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
}
