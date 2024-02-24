package com.chnnhc.shortlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;

public interface UrlTitleService {

  /**
   * 根据 URL 获取标题
   *
   * @param url 目标网站地址
   * @return 网站标题
   */
  String getTitleByUrl(String url);
}
