package com.tencent.bk.codecc.defect.constant

object LLMConstants {

    /**
     * bkticket 列表
     */
    val BK_TICKET_FOR_USER = mutableMapOf<String, String>()

    /**
     * websocket 修复建议信息 订阅
     */
    const val WEBSOCKET_TOPIC_DEFECT_SUGGESTION = "/topic/defect/suggestion"

    /**
     * 大模型选项
     */
    // 以下变量获取的值，后期通过OP获取
    const val LLM_NAME = ""

    /**
     * 大模型鉴权key
     */
    // 以下变量获取的值，后期通过OP获取
    const val LLM_API_KEY = ""

    /**
     * 接口超时
     */
    const val TIMEOUT_SECONDS = 60

    /**
     * 结果随机性
     */
    // 以下变量获取的值，后期通过OP获取
    const val TEMPERATURE = 0.9

    /**
     * 生成结果数量
     */
    const val RESULT_NUM = 1

    /**
     * 大语言模型种类选项
     */
    enum class LLMType(val llmName: String, val llmCnName: String) {
        HUNYUAN("hunyuan", "混元大模型"),
        CHATGPT("chatgpt", "OpenAI大模型"),
        CHATGLM2("chatglm2", "清华大模型"),
        QPILOT("qpilot", "QQ大模型"),
        BKAIDEV("bkaidev", "蓝鲸中心AI")
    }

    /**
     * 问答角色
     */
    const val CHAT_ROLE_USER = "user"
    const val CHAT_ROLE_ASSISTANT = "assistant"

    /**
     * bkai使用OpenAI客户端接入方式，包括基于应用态和用户态访问
     */
    const val LLM_GATEWAY_URL_APPSPACE = "/prod/appspace/gateway/llm/v1/chat/completions"
    const val LLM_GATEWAY_URL_USERSPACE = "/prod/gateway/llm/v1/chat/completions"

    /**
     * bkaidev接口模型
     */
    const val BKAIDEV_API_URL = "/prod/aidev/intelligence/chat_completion/"
    const val BKAIDEV_LLM_MODEL = "hunyuan.ChatCompletion:hunyuan"
    const val BKAIDEV_APP_COLLECTION = "default_chat_completion"

    /**
     * openai接口模型
     */
    const val OPENAI_API_URL = "/prod/aidev/intelligence/raw_service/model-openai-ChatCompletion/execute/"
    const val OPENAI_MODEL = "gpt-3.5-turbo"

    /**
     * 混元接口模型
     */
    const val HUNYUAN_API_URL = "/openapi/v1/chat/completions"
    const val HUNYUAN_MODEL = "hunyuan-176B" // 模型名称, 当前支持"hunyuan-176B", "hunyuan-13B"

    /**
     * 修复建议prompt
     */
    const val DEFECT_SUGGESTIONS_USER_CHAT_PROMPT = "你是一位代码修复建议专家，回答接下来提到的问题"
    const val DEFECT_SUGGESTIONS_ASSISTANT_CHAT_PROMPT = "好的，我将扮演代码修复建议专家跟你聊天,请问你有什么想要聊的？"
    const val DEFECT_SUGGESTIONS_USER_CHAT_ASK_PROMPT = "请根据以下描述和代码内容，提供正确的修复建议，" +
            "请一步一步来，确保得到正确答案！"

    /**
     * 代码扫描prompt
     */
    const val SCAN_USER_CHAT_PROMPT = "请扮演一名静态代码检查分析专家，回答接下来提到的问题"
    const val SCAN_ASSISTANT_CHAT_PROMPT = "好的，我将扮演静态代码检查分析专家跟你聊天,请问你有什么想要聊的？"
    const val SCAN_USER_CHAT_ASK_PROMPT = "请对以下代码进行静态代码检查分析，%s " +
            "请用格式：" +
            "output:{\"defects\": [ {\"checkerName\":\"\${checkerName}\", " +
            "\"line\": n, \"description\": \"\${description}\"} ]} " +
            "返回检查结果, 不要包含注释内容和空白行"

    const val LLM_PROMPT_ANSWER_ASSISTANT_CHAT_CONTEXT_SEPARATE = "output:"

    val LANG_SUFFIX_MAP = mapOf(
        "cs" to listOf(".cs"),
        "cpp" to listOf(".c", ".ec", ".pgc", ".C", ".c++", ".cc", ".CPP", ".cpp", ".cxx", ".inl", ".pcc", ".H", ".h", ".hh", ".hpp", ".hxx"),
        "java" to listOf(".java"),
        "php" to listOf(".php", ".php3", ".php4", ".php5"),
        "objectivec" to listOf(".m", ".mm", ".h"),
        "python" to listOf(".py", ".pyw"),
        "ecmascript" to listOf(".es6", ".js"),
        "vue" to listOf(".vue"),
        "ruby" to listOf(".rb"),
        "lua" to listOf(".lua"),
        "go" to listOf(".go"),
        "swift" to listOf(".swift"),
        "typescript" to listOf(".ts", ".tsx"),
        "kotlin" to listOf(".kt", ".kts"),
        "dart" to listOf(".dart"),
        "solidity" to listOf(".sol"),
        "scala" to listOf(".scala"),
        "css" to listOf(".css"),
        "rust" to listOf(".rs"),
        "protobuf" to listOf(".proto")
    )
}
