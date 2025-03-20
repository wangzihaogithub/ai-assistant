package com.github.aiassistant.service.text.embedding;

import com.github.aiassistant.dao.KnSettingWebsearchBlacklistMapper;
import com.github.aiassistant.util.AiUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 黑名单问题配置
 */
public class KnSettingWebsearchBlacklistServiceImpl {
    // @Resource
    private final KnSettingWebsearchBlacklistMapper knSettingWebsearchBlacklistMapper;

    public KnSettingWebsearchBlacklistServiceImpl(KnSettingWebsearchBlacklistMapper knSettingWebsearchBlacklistMapper) {
        this.knSettingWebsearchBlacklistMapper = knSettingWebsearchBlacklistMapper;
    }

    /**
     * 查询黑名单问题
     *
     * @return 黑名单问题
     */
    public List<ReRankModelClient.QuestionVO> selectBlackList() {
        return knSettingWebsearchBlacklistMapper.selectBlackList().stream()
                .map(e -> new ReRankModelClient.QuestionVO(e.getQuestion(), AiUtil.scoreToDouble(e.getSimilarity())))
                .collect(Collectors.toList());
    }
}
