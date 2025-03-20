package com.github.aiassistant.serviceintercept;

import com.github.aiassistant.entity.model.chat.MemoryIdVO;
import com.github.aiassistant.entity.model.chat.Message;
import com.github.aiassistant.entity.model.chat.RequestTrace;
import com.github.aiassistant.entity.model.user.AiAccessUserVO;
import com.github.aiassistant.service.text.memory.AiMemoryMessageServiceImpl;

import java.util.Date;
import java.util.List;

public interface AiMemoryMessageServiceIntercept extends ServiceIntercept {

    default AiMemoryMessageServiceImpl.AiMemoryMessageVO afterMessage(AiMemoryMessageServiceImpl.AiMemoryMessageVO source, Date now, RequestTrace<MemoryIdVO, AiAccessUserVO> requestTrace,
                                                                      String againUserQueryTraceNumber,
                                                                      Boolean websearch,
                                                                      Message<AiAccessUserVO> message) {

//            if (jobList != null) {
//                for (KnJobVO knJobVO : jobList) {
//                    AiMemoryMessageToolJob jobVO = new AiMemoryMessageToolJob();
//                    jobVO.setToolRequestId(Objects.toString(toolRequestId, ""));
//                    jobVO.setJobId(Integer.valueOf(knJobVO.getId()));
//                    jobVO.setJobScore(knJobVO.scoreLong());
//                    jobVO.setJobIndexUpdatedTime(knJobVO.getIndexUpdatedTime());
//                    jobVO.setJobName(AiUtil.limit(knJobVO.getName(), 512, true));
//                    jobVO.setAiMemoryId(memoryId);
//                    jobVO.setJobIndexName(AiUtil.limit(knJobVO.getIndexName(), 128, true));
//                    vo.getJobList().add(jobVO);
//                }
//            }
        return source;
    }


    default AiMemoryMessageServiceImpl.AiMemoryVO afterMemory(AiMemoryMessageServiceImpl.AiMemoryVO source, Date now, RequestTrace<MemoryIdVO, AiAccessUserVO> requestTrace,
                                                              String againUserQueryTraceNumber, Boolean websearch) {
        return source;
    }

    default void afterInsert(List<AiMemoryMessageServiceImpl.AiMemoryVO> list) {
        // 插入推荐岗位
//      List<AiMemoryMessageToolJob> jobList = messageVOList.stream().flatMap(e -> e.getJobList().stream()).collect(Collectors.toList());
//      aiMemoryMessageToolJobMapper.insertBatchSomeColumnDefault(jobList);
    }

}
