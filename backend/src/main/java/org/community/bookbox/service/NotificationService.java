package org.community.bookbox.service;

/** 提醒发送接口。生产环境可替换为短信/微信实现，当前为本地模拟实现。 */
public interface NotificationService {

    void send(String target, String message);
}
