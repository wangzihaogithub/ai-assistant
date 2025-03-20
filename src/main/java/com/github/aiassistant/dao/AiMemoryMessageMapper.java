package com.github.aiassistant.dao;

import com.github.aiassistant.entity.AiMemoryMessage;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface AiMemoryMessageMapper {
    int insertBatchSomeColumn(Collection<? extends AiMemoryMessage> collection);

    List<AiMemoryMessage> selectListByMemoryId(Integer memoryId,
                                               String rootAgainUserQueryTraceNumber);

    int updateDeleteByUserQueryTraceNumber(Date deleteTime,
                                           List<String> againUserQueryTraceNumberList);

    List<String> selectAgainTraceNumberList(List<String> userQueryTraceNumberList);

    List<AiMemoryMessage> selectRootAgainTraceNumberList(List<String> userQueryTraceNumberList);

}
