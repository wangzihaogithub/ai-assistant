package com.github.aiassistant;

import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;

import java.util.Arrays;

public class DocumentBySentenceSplitterTest {
    public static void main(String[] args) {
        DocumentByWordSplitter documentBySentenceSplitter = new DocumentByWordSplitter(10,5);
        String[] split = documentBySentenceSplitter.split("关于您的问题“央国企的招聘是否有地域限制？”，这是一个很实际也很重要的问题呢！一般来说，央国企的招聘确实会存在一定的地域限制，但这个限制并不是绝对的。部分岗位，尤其是总部或者特殊项目岗位，可能会面向全国招聘，而一些地方性的分支机构则更倾向于招聘本地或者周边地区的候选人。这主要是考虑到工作地点、员工稳定性以及成本等因素。\n" +
                "不过，随着近年来央国企的发展和业务拓展，越来越多的岗位开始放宽地域限制，特别是对于一些稀缺专业人才，企业往往愿意突破地域界限来吸引优秀的人才加入。所以，如果您对央国企的某个岗位感兴趣，建议您仔细阅读招聘公告中的具体要求，并且不要轻易因为可能存在的地域限制而放弃申请哦！\n" +
                "如果您还有其他就业相关的问题，我很乐意继续为您提供帮助！\n");
        System.out.println("split = " + Arrays.toString(split));

    }
}
