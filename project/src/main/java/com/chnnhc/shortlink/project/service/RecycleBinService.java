package com.chnnhc.shortlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chnnhc.shortlink.project.dao.entity.ShortLinkDO;
import com.chnnhc.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.chnnhc.shortlink.project.dto.req.RecycleBinRemoveReqDTO;
import com.chnnhc.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import org.springframework.stereotype.Service;

@Service
public interface RecycleBinService extends IService<ShortLinkDO> {
  /**
   * 保存回收站
   *
   * @param requestParam 请求参数
   */
  void saveRecycleBin(RecycleBinSaveReqDTO requestParam);

  /**
   * 从回收站恢复短链接
   *
   * @param requestParam 恢复短链接请求参数
   */
  void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam);

  /**
   * 从回收站移除短链接
   *
   * @param requestParam 移除短链接请求参数
   */
  void removeRecycleBin(RecycleBinRemoveReqDTO requestParam);
}
