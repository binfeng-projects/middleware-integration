package com.bf.middleware.test.cloudplatform;

import lombok.extern.slf4j.Slf4j;
import org.bf.framework.boot.support.vms.VmsProxy;
import org.bf.framework.common.util.CollectionUtils;
import org.bf.framework.test.base.BaseCoreTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.Serializable;

@Slf4j
public class TestVms implements BaseCoreTest {
    @Autowired
    @Qualifier("bf.vms.gz_VmsProxy")
    private VmsProxy tencentProxy;
    @Autowired
    @Qualifier("bf.vms.hz_VmsProxy")
    private VmsProxy aliyunProxy;
    @Test
    public void testTencent() throws Exception {
        //后台模版配置
        //系统通知。{1}
        tencentProxy.callPhone(CollectionUtils.newHashSet("123456789"),"测试");
    }
    @Test
    public void testAliyun() throws Exception {
        String text = "测试";
        String param = "{name: \"" + text + "\"}";
        //后台模版配置
        //告警通知。${text}
        String result = aliyunProxy.callPhone(CollectionUtils.newHashSet("123465789"),param);
        System.out.println(result);
    }

//    public String phoneNotice(String msg) {
//        if (StringUtils.isBlank(msg)) {
//            return "param empty";
//        }
//        NoticeDto dto = JSON.parseObject(msg, NoticeDto.class);
//        if (StringUtils.isBlank(dto.getMobiles()) || StringUtils.isBlank(dto.getText())) {
//            return "param empty";
//        }
//        if (StringUtils.isBlank(dto.getAppName())) {
//            return "who are you";
//        }
//        List<String> errors = Lists.newArrayList();
//        //最终没限流，可以拨打的电话
//        List<String> finalCalls = Lists.newArrayList();
//        for (String s : dto.getMobiles().split(",")) {
//            String mobile = s.trim();
//            if (lettuceProxy.setIfAbsent(getRedisKey(dto.getAppName(),mobile), "1", noticePhoneLimitTime)) {
//                finalCalls.add(mobile);
//            } else {
//                String errorMsg = String.format("rate limit,mobile %s is reject", mobile);
//                log.warn(errorMsg);
//                errors.add(errorMsg);
//            }
//        }
//        if(!org.springframework.util.CollectionUtils.isEmpty(finalCalls)) {
//            String param = dto.getText();
//            if(!dto.getText().startsWith("{")) {
//                param = "{name: \"" + dto.getText() + "\"}";
//            }
//            String errMsg = vmsProxy.callPhone(Sets.newHashSet(finalCalls),param);
//            if(StringUtils.isNotBlank(errMsg)) {
//                errors.add(errMsg);
//            }
//        }
//        if (!org.springframework.util.CollectionUtils.isEmpty(errors)) {
//            return StringUtils.join(errors, "\r\n");
//        }
//        return null;
//    }
//    public String getRedisKey(String appName,String mobile) {
//        return String.format("%s_%s_%s", appName, active, mobile);
//    }

    public static class NoticeDto implements Serializable {
        private String text;
        private String mobiles;

        public String getMobiles() {
            return mobiles;
        }

        public void setMobiles(String mobiles) {
            this.mobiles = mobiles;
        }

        private String appName;
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }
    }
}
