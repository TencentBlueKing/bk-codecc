import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.defect.constant.LLMConstants
import com.tencent.bk.codecc.defect.llm.api.*
import com.tencent.bk.codecc.defect.llm.chat.LLMChat
import com.tencent.bk.codecc.defect.llm.chat.LLMChatApi
import com.tencent.bk.codecc.defect.llm.handle.*
import com.tencent.bk.codecc.defect.llm.http.*
import com.tencent.bk.codecc.defect.llm.log.LogLevel
import com.tencent.bk.codecc.defect.llm.log.Timeout
import com.tencent.bk.codecc.defect.pojo.serializable.SerializableChat
import com.tencent.bk.codecc.defect.pojo.serializable.SerializableExecuteKwargs
import com.tencent.bk.codecc.defect.pojo.serializable.chatCompletionRequest
import com.tencent.bk.codecc.defect.vo.ApiAuthVO
import com.tencent.bk.codecc.defect.vo.ApiChatVO
import com.tencent.devops.common.codecc.util.JsonUtil
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.time.Duration.Companion.minutes

class LLMTest {

    val llmConfig: HttpClientConfig = HttpClientConfig(
        token = "",
        logging = LoggingConfig(logLevel = LogLevel.All),
        timeout = Timeout(socket = 1.minutes),
    )

    @Test
    fun testStream() {

        var apiAuthVO = ApiAuthVO(
            bkAppCode = "",
            bkAppSecret = "",
            bkTicket = "",
            host = "",
            apiKey = ""

        )

        var apiChatVO = ApiChatVO(
            userChatPrompt = LLMConstants.DEFECT_SUGGESTIONS_USER_CHAT_PROMPT,
            assistantChatPrompt = LLMConstants.DEFECT_SUGGESTIONS_ASSISTANT_CHAT_PROMPT,
            content = "规则列表为: [ {\\n  \\\"checkerName\\\" : \\\"FORWARD_NULL\\\",\\n  " +
                    "\\\"prompt\\\" : \\\"空指针对象被解引用\\\"\\n}, {\\n  \\\"checkerName\\\" : \\\"DEADCODE\\\",\\n " +
                    " \\\"prompt\\\" : \\\"不可达代码\\\"\\n}, {\\n  \\\"checkerName\\\" : \\\"BAD_LOCK_OBJECT\\\",\\n " +
                    " \\\"prompt\\\" : \\\"checkerName\\\" : \\\"BAD_CHECK_OF_WAIT_COND\\\",\\n  " +
                    "\\\"prompt\\\" : \\\"线程未正确检查等待条件即在互斥锁中调用 wait() 的情况\\\"\\n}, {\\n  " +
                    "\\\"checkerName\\\" : \\\"GUARDED_BY_VIOLATION\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"字段在无锁时进行更新，从而导致潜在竞态条件的情况\\\"\\n}, {\\n " +
                    " TICAL_BRANCHES\\\",\\n  \\\"prompt\\\" : \\\"无论条件为何始终执行相同代码的条件语句和表达式\\\"\\n}, " +
                    "{\\n  \\\"checkerName\\\" : \\\"INFINITE_LOOP\\\",\\n  " +
                    "\\\"prompt\\\" : \\\"循环永不终止的情况\\\"\\n}, {\\n  \\\"checkerName\\\" : " +
                    "\\\"INVALIDATE_ITERATOR\\\",\\n  \\\"prompt\\\" : \\\"使用无效 \\\"LOCK_EVASION\\\",\\n  " +
                    "\\\"prompt\\\" : \\\"代码规避锁获取或充分持有的情况\\\"\\n}, {\\n  " +
                    "\\\"checkerName\\\" : \\\"LOCK_INVERSION\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"程序在不同位置按不同顺序获取锁,互斥锁对的情况\\\"\\n}, {\\n  \\\"checkerName\\\" : " +
                    "\\\"MISSING_BREAK\\\",\\n  \\\"prompt\\\" : \\\"checkerName\\\" : " +
                    "\\\"NON_STATIC_GUARDING_STATIC\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"通过锁定非静态字段来保护静态字段的情况\\\"\\n}, {\\n  " +
                    "\\\"checkerName\\\" : \\\"NULL_RETURNS\\\",\\n  " +
                    "\\\"prompt\\\" : \\\"可能存在危险的方式使用返回的值的情况\\\"\\n}, {\\n  " +
                    "\\\"checkerName\\\" : \\\"RESOURCE_LEAK\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"程序未能保证尽快释放系统资源的情况\\\"\\n}, {\\n  \\\"checkerName\\\" : " +
                    "\\\"REVERSE_INULL\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"在使用了应该已经失败的值（如果该值确实为 null、Nothing、nil 或 undefined）" +
                    "之后执行是否存在 null、Nothing、nil 或 undefinedGER_DIVISION\\\",\\n  " +
                    "\\\"prompt\\\" : \\\"使用整数除法而意外损失了算术运算精度的情况\\\"\\n}, " +
                    "{\\n  \\\"checkerName\\\" : \\\"UNUSED_VALUE\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"值被赋值给变量但从未使用的情况\\\"\\n}, {\\n  \\\"checkerName\\\" : " +
                    "\\\"USE_AFTER_FREE\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"找在内存或资源被释放或关闭后使用这些内存或资源的情况\\\"\\n} ]"
        )

        val request = chatCompletionRequest {
            host = apiAuthVO.host
            sessionId = "codecc_scan"
            stream = true
            sessionHistory = listOf<SerializableChat>()
            executeKwargs = SerializableExecuteKwargs(
                stream = true,
                streamTimeOut = 20
            )
            appCollection = LLMConstants.BKAIDEV_APP_COLLECTION
            llm = LLMConstants.BKAIDEV_LLM_MODEL
            chatPrompts {
                chatPrompt {
                    role = LLMConstants.CHAT_ROLE_USER
                    content = apiChatVO.userChatPrompt
                }
                chatPrompt {
                    role = LLMConstants.CHAT_ROLE_ASSISTANT
                    content = apiChatVO.assistantChatPrompt
                }
                chatPrompt {
                    role = LLMConstants.CHAT_ROLE_USER
                    content = apiChatVO.content
                }
            }
        }


        val http = transport()

        val chat: LLMChat = LLMChatApi(http)
        val authorStr = JsonUtil.toJson(apiAuthVO).replace("\\s+".toRegex(), "")
        runBlocking {
            chat.chatCompletions(request, authorStr).onEach {
                it.split("\n").onEach { result ->
                    println("result: $result")
                }
            }.launchIn(this).join()
        }
    }

    fun transport(config: HttpClientConfig? = null): HttpTransport {
        return HttpTransport(
            createHttpClient(
                config ?: llmConfig
            )
        )
    }

    @Test
    fun testHttpStream() {
        //TODO：以下变量获取的值，后期通过OP获取
        val llmName = "bkaidev"

        //选择大模型接口及结果处理对象
        val (llmApi: LLMApi, llmResultHandle: LLMResultHandle) = when (llmName) {
            LLMConstants.LLMType.HUNYUAN.llmName.lowercase() -> Pair(HunYuanApi, HunYuanResultHandle)
            LLMConstants.LLMType.CHATGPT.llmName.lowercase() -> Pair(ChatGPTApi, ChatGPTResultHandle)
            LLMConstants.LLMType.CHATGLM2.llmName.lowercase() -> Pair(ChatGLM2Api, ChatGLM2ResultHandle)
            LLMConstants.LLMType.QPILOT.llmName.lowercase() -> Pair(QpilotApi, QpilotResultHandle)
            LLMConstants.LLMType.BKAIDEV.llmName.lowercase() -> Pair(BkAIDevApi, BkAIDevResultHandle)
            else -> Pair(HunYuanApi, HunYuanResultHandle)
        }

        var apiAuthVO = com.tencent.bk.codecc.defect.vo.ApiAuthVO(
            bkAppCode = "",
            bkAppSecret = "",
            bkTicket = "",
            host = "",
            apiKey = ""
        )

        var apiChatVO = ApiChatVO(
            stream = true,
            userChatPrompt = LLMConstants.DEFECT_SUGGESTIONS_USER_CHAT_PROMPT,
            assistantChatPrompt = LLMConstants.DEFECT_SUGGESTIONS_ASSISTANT_CHAT_PROMPT,
            content = "规则列表为: [ {\\n  \\\"checkerName\\\" : \\\"FORWARD_NULL\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"空指针对象被解引用\\\"\\n}, {\\n  \\\"checkerName\\\" : \\\"DEADCODE\\\",\\n  " +
                    "\\\"prompt\\\" : \\\"不可达代码\\\"\\n}, {\\n  \\\"checkerName\\\" : \\\"BAD_LOCK_OBJECT\\\",\\n " +
                    " \\\"prompt\\\" : \\\"checkerName\\\" : \\\"BAD_CHECK_OF_WAIT_COND\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"线程未正确检查等待条件即在互斥锁中调用 wait() 的情况\\\"\\n}, {\\n  \\\"checkerName\\\" : " +
                    "\\\"GUARDED_BY_VIOLATION\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"字段在无锁时进行更新，从而导致潜在竞态条件的情况\\\"\\n}, {\\n  TICAL_BRANCHES\\\",\\n " +
                    " \\\"prompt\\\" : \\\"无论条件为何始终执行相同代码的条件语句和表达式\\\"\\n}, {\\n  \\\"checkerName\\\" : " +
                    "\\\"INFINITE_LOOP\\\",\\n  \\\"prompt\\\" : \\\"循环永不终止的情况\\\"\\n}, {\\n  " +
                    "\\\"checkerName\\\" : \\\"INVALIDATE_ITERATOR\\\",\\n  \\\"prompt\\\" : \\\"使用无效 " +
                    "\\\"LOCK_EVASION\\\",\\n  \\\"prompt\\\" : \\\"代码规避锁获取或充分持有的情况\\\"\\n}, {\\n  " +
                    "\\\"checkerName\\\" : \\\"LOCK_INVERSION\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"程序在不同位置按不同顺序获取锁,互斥锁对的情况\\\"\\n}, {\\n  \\\"checkerName\\\" : " +
                    "\\\"MISSING_BREAK\\\",\\n  \\\"prompt\\\" : \\\"checkerName\\\" : " +
                    "\\\"NON_STATIC_GUARDING_STATIC\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"通过锁定非静态字段来保护静态字段的情况\\\"\\n}, {\\n  \\\"checkerName\\\" : " +
                    "\\\"NULL_RETURNS\\\",\\n  \\\"prompt\\\" : \\\"可能存在危险的方式使用返回的值的情况\\\"\\n}, {\\n  " +
                    "\\\"checkerName\\\" : \\\"RESOURCE_LEAK\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"程序未能保证尽快释放系统资源的情况\\\"\\n}, {\\n  \\\"checkerName\\\" : " +
                    "\\\"REVERSE_INULL\\\",\\n  \\\"prompt\\\" : \\\"在使用了应该已经失败的值（如果该值确实为 null、" +
                    "Nothing、nil 或 undefined）之后执行是否存在 null、Nothing、nil 或 undefinedGER_DIVISION\\\",\\n  " +
                    "\\\"prompt\\\" : \\\"使用整数除法而意外损失了算术运算精度的情况\\\"\\n}, {\\n  \\\"checkerName\\\" : " +
                    "\\\"UNUSED_VALUE\\\",\\n  \\\"prompt\\\" : \\\"值被赋值给变量但从未使用的情况\\\"\\n}, {\\n  " +
                    "\\\"checkerName\\\" : \\\"USE_AFTER_FREE\\\",\\n  \\\"prompt\\\" : " +
                    "\\\"找在内存或资源被释放或关闭后使用这些内存或资源的情况\\\"\\n} ]"
        )

        println("start chat....")

        runBlocking {
            llmApi.defectSuggestionApiStream(apiAuthVO, apiChatVO).collect {
                println("result: $it")
            }
        }
    }

    @Test
    fun testTextMessage() {
        var apiAuthVO = ApiAuthVO(
            bkAppCode = "",
            bkAppSecret = "",
            bkTicket = "",
            host = "",
            apiKey = ""

        )
        var arrary = arrayOf(JsonUtil.toJson(apiAuthVO))
        val base64 = Base64.getEncoder().encodeToString(arrary.joinToString().toByteArray(StandardCharsets.UTF_8))
        val objectMapper = ObjectMapper()
        println(base64)
        var code = objectMapper.readValue<Array<String>>(base64, Array<String>::class.java);
        println(code.forEach {
            println(it)
        })
    }

    @Test
    fun testStringEncode() {
        val buffers = mutableListOf<ByteArray>()
        val str = "你好啊"
        val buffer1 = ByteArray(5)
        System.arraycopy(str.toByteArray(), 0, buffer1, 0, 5)
        val trimmed1 = buffer1.dropLastWhile { it == 0.toByte() }.toByteArray()
        val trimed1Str = String(trimmed1, Charsets.ISO_8859_1).trimEnd('\u0000')
        buffers.add(trimed1Str.toByteArray(Charsets.ISO_8859_1))

        val buffer2 = ByteArray(4)
        System.arraycopy(str.toByteArray(), 5, buffer2, 0, 4)
        val trimmed2 = buffer2.dropLastWhile { it == 0.toByte() }.toByteArray()
        val trimed2Str = String(trimmed2, Charsets.ISO_8859_1).trimEnd('\u0000')
        buffers.add(trimed2Str.toByteArray(Charsets.ISO_8859_1))

        val totalSize = buffers.sumOf { it.size }
        var combinedBuffer = ByteArray(totalSize)
        var currentIndex = 0
        buffers.forEach {arr ->
            System.arraycopy(arr, 0, combinedBuffer, currentIndex, arr.size)
            currentIndex += arr.size
        }
        val appendResult =  String(combinedBuffer, Charsets.ISO_8859_1).trimEnd('\u0000')

        println(String(appendResult.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8))

    }
}
