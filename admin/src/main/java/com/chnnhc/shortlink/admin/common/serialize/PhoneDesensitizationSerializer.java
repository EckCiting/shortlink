package com.chnnhc.shortlink.admin.common.serialize;

import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class PhoneDesensitizationSerializer extends JsonSerializer<String> {

  /** 手机号脱敏反序列化 */
  @Override
  public void serialize(String phone, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    String phoneDesensitization = DesensitizedUtil.mobilePhone(phone);
    gen.writeString(phoneDesensitization);
  }
}
