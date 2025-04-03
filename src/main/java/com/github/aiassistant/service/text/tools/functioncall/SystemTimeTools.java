package com.github.aiassistant.service.text.tools.functioncall;

import com.github.aiassistant.service.text.tools.Tools;
import com.github.aiassistant.util.DateUtil;
import dev.langchain4j.agent.tool.Tool;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemTimeTools extends Tools {

    @Tool(name = "获取现在的时间", value = {"# 插件功能\n" +
            "返回现在的时间。" +
            "返回yyyy-MM-dd HH:mm:ss格式的时间。"
    })
    public String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    @Tool(name = "获取明天的时间", value = {"# 插件功能\n" +
            "返回明天的时间。" +
            "返回yyyy-MM-dd格式的时间。"
    })
    public String tomorrow() {
        Date dateTime = DateUtil.offsetDay(new Date(), 1);
        return DateUtil.dateformat(dateTime, "yyyy-MM-dd");
    }

    @Tool(name = "获取昨天的时间", value = {"# 插件功能\n" +
            "返回昨天的时间。" +
            "返回yyyy-MM-dd格式的时间。"
    })
    public String yesterday() {
        Date dateTime = DateUtil.offsetDay(new Date(), -1);
        return DateUtil.dateformat(dateTime, "yyyy-MM-dd");
    }

    @Tool(name = "获取一周前的时间", value = {"# 插件功能\n" +
            "返回一周前的时间。" +
            "返回yyyy-MM-dd格式的时间。"
    })
    public String beforeWeek() {
        Date dateTime = DateUtil.offsetWeek(new Date(), -1);
        return DateUtil.dateformat(dateTime, "yyyy-MM-dd");
    }

    @Tool(name = "获取一周后的时间", value = {"# 插件功能\n" +
            "返回一周后的时间。" +
            "返回yyyy-MM-dd格式的时间。"
    })
    public String afterWeek() {
        Date dateTime = DateUtil.offsetWeek(new Date(), 1);
        return DateUtil.dateformat(dateTime, "yyyy-MM-dd");
    }

    @Tool(name = "获取一月前的时间", value = {"# 插件功能\n" +
            "返回一月前的时间。" +
            "返回yyyy-MM-dd格式的时间。"
    })
    public String beforeMonth() {
        Date dateTime = DateUtil.offsetMonth(new Date(), -1);
        return DateUtil.dateformat(dateTime, "yyyy-MM-dd");
    }

    @Tool(name = "获取一月后的时间", value = {"# 插件功能\n" +
            "返回一月后的时间。" +
            "返回yyyy-MM-dd格式的时间。"
    })
    public String afterMonth() {
        Date dateTime = DateUtil.offsetMonth(new Date(), 1);
        return DateUtil.dateformat(dateTime, "yyyy-MM-dd");
    }

    @Tool(name = "获取一年前的时间", value = {"# 插件功能\n" +
            "返回一年前的时间。" +
            "返回yyyy-MM-dd格式的时间。"
    })
    public String beforeYear() {
        Date dateTime = DateUtil.offsetDay(new Date(), -365);
        return DateUtil.dateformat(dateTime, "yyyy-MM-dd");
    }

    @Tool(name = "获取一年后的时间", value = {"# 插件功能\n" +
            "返回一年后的时间。" +
            "返回yyyy-MM-dd格式的时间。"
    })
    public String afterYear() {
        Date dateTime = DateUtil.offsetDay(new Date(), 365);
        return DateUtil.dateformat(dateTime, "yyyy-MM-dd");
    }

}
