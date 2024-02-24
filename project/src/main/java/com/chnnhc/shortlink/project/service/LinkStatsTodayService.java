package com.chnnhc.shortlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chnnhc.shortlink.project.dao.entity.LinkStatsTodayDO;
import org.springframework.stereotype.Service;

/** 短链接今日统计接口层 */
@Service
public interface LinkStatsTodayService extends IService<LinkStatsTodayDO> {}
