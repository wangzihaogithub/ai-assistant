package com.github.aiassistant.dao;

import com.github.aiassistant.entity.KnSettingWebsearchBlacklist;

import java.util.List;

/**
 * 黑名单问题配置
 */
public interface KnSettingWebsearchBlacklistMapper {
    List<KnSettingWebsearchBlacklist> selectBlackList();
}
