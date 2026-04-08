package com.meeting.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.meeting.dto.MeetingVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kimi API服务 - 会议纪要生成
 * 集成 Whisper 语音识别 + GLM-5 文本生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KimiService {

    @Value("${meeting.kimi.api-key}")
    private String apiKey;

    @Value("${meeting.kimi.base-url}")
    private String baseUrl;

    @Value("${meeting.kimi.model}")
    private String model;

    private final WhisperService whisperService;
    private final MeetingProgressService progressService;
    private RestTemplate restTemplate;

    /**
     * 获取带超时的 RestTemplate（延迟初始化）
     */
    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000); // 连接超时 10 秒
            factory.setReadTimeout(1200000);  // 读取超时 1200 秒（20分钟，GLM-5处理长文本较慢）
            restTemplate = new RestTemplate(factory);
        }
        return restTemplate;
    }

    /**
     * 处理音频文件
     * 流程: Whisper语音识别 -> GLM-5生成会议纪要
     */
    public MeetingResult processAudio(String audioPath) {
        return processAudio(audioPath, null);
    }

    /**
     * 处理音频文件（带进度推送）
     * 流程: Whisper语音识别 -> GLM-5生成会议纪要
     *
     * @param audioPath 音频文件路径
     * @param meetingId 会议ID，用于推送进度
     */
    public MeetingResult processAudio(String audioPath, Long meetingId) {
        log.info("开始处理音频文件: {}, meetingId: {}", audioPath, meetingId);

        try {
            // Step 1: 使用 Whisper 进行语音识别
            log.info("Step 1: 调用 Whisper 进行语音识别...");
            WhisperService.TranscriptionResult transcriptResult = whisperService.transcribe(
                    audioPath, meetingId, null);

            if (!transcriptResult.isSuccess()) {
                log.error("Whisper 转录失败: {}", transcriptResult.getError());
                throw new RuntimeException("Whisper 转录失败: " + transcriptResult.getError());
            }

            String rawTranscript = transcriptResult.getText();
            log.info("Whisper 转录完成，原始文本长度: {} 字符", rawTranscript.length());

            // Step 1.5: 调用 GLM-5 优化转录文本（繁简转换+添加标点+语义断句）
            log.info("Step 1.5: 调用 GLM-5 优化转录文本...");
            if (meetingId != null) {
                progressService.sendAiProgress(meetingId, 45, "正在优化转录文本格式...");
            }
            String transcript = optimizeTranscript(rawTranscript);
            log.info("转录文本优化完成，长度: {} 字符", transcript.length());
            log.debug("优化后内容:\n{}", transcript);

            // 计算音频时长
            String duration = calculateDuration(transcriptResult.getSegments());

            // Step 2: 调用 GLM-5 基于真实转录生成会议纪要
            log.info("Step 2: 调用 GLM-5 生成会议纪要...");

            // 推送 AI 阶段开始
            if (meetingId != null) {
                progressService.sendAiStart(meetingId);
            }

            String summaryPrompt = buildSummaryPrompt(transcript, duration);

            // 推送 AI 进度：正在发送请求 (50%)
            if (meetingId != null) {
                progressService.sendAiProgress(meetingId, 50, "正在请求 AI 生成会议纪要...");
            }

            String aiResponse = chat(summaryPrompt);
            log.info("GLM-5 生成会议纪要完成");

            // 推送 AI 进度：已收到响应 (80%)
            if (meetingId != null) {
                progressService.sendAiProgress(meetingId, 80, "正在解析 AI 响应...");
            }

            // 解析AI返回的JSON
            MeetingResult result = parseAiResponse(aiResponse);
            result.setDuration(duration);

            // 如果没有返回speakers，使用转录的segments构建
            if (result.getSpeakers() == null || result.getSpeakers().isEmpty()) {
                // 使用 GLM-5 优化后的文本（简体中文+标点）更新 segments
                List<WhisperService.Segment> optimizedSegments = applyOptimizedTextToSegments(
                    transcriptResult.getSegments(), transcript);
                result.setSpeakers(buildSpeakersFromTranscript(optimizedSegments));
            }

            // 推送 AI 完成
            if (meetingId != null) {
                progressService.sendAiComplete(meetingId);
                progressService.sendCompleted(meetingId);
            }

            return result;

        } catch (Exception e) {
            log.error("AI处理音频失败", e);
            if (meetingId != null) {
                progressService.sendError(meetingId, e.getMessage());
            }
            throw new RuntimeException("AI处理音频失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建生成会议纪要的 Prompt（飞书风格结构化纪要）
     */
    private String buildSummaryPrompt(String transcript, String duration) {
        return """
            请根据以下真实的会议转录文本，生成详细的结构化会议纪要。

            会议时长: %s

            转录文本：
            %s

            请按以下JSON格式返回结果（不要包含markdown代码块标记）：
            {
                "overview": "会议整体概述，100-200字，概括会议背景、主要讨论内容和整体结论",
                "topics": [
                    {
                        "id": 1,
                        "title": "话题标题，简洁概括讨论主题",
                        "timeRange": "00:05:23 - 00:15:40",
                        "summary": "该话题的详细总结，300-500字，包含讨论的主要内容、各方观点、达成的共识等",
                        "keyPoints": ["关键要点1", "关键要点2", "关键要点3"],
                        "speakers": ["说话人A", "说话人B"],
                        "todos": [
                            {"id": 1, "content": "具体任务描述", "done": false, "assignee": "负责人姓名", "topicId": 1}
                        ],
                        "decisions": ["该话题达成的决策1", "决策2"]
                    }
                ],
                "stats": {
                    "totalSpeakers": 3,
                    "totalDuration": "%s",
                    "speakerStats": [
                        {"name": "说话人A", "speakTime": "00:10:23", "percentage": 35}
                    ]
                }
            }

            详细要求：
            1. **内容必须基于提供的真实转录文本**，不要编造任何信息
            2. **话题分段**：将会议内容划分为3-6个主要话题，每个话题包含明确的标题
            3. **时间范围**：为每个话题标注大致的时间范围（从转录的时间戳推断）
            4. **详细总结**：每个话题的summary要详细，300-500字，包含讨论的主要内容和观点
            5. **关键要点**：每个话题提取3-5条关键要点，用简洁的语言概括
            6. **待办事项**：从转录中提取所有待办任务，关联到对应的话题，标明负责人
            7. **决策记录**：总结每个话题达成的重要决策和共识
            8. **发言统计**：统计各说话人的发言时长占比（从转录的时间戳计算）
            9. **颜色编码**：如果需要颜色，使用Element Plus主题色：#409EFF(蓝)、#67C23A(绿)、#E6A23C(黄)、#F56C6C(红)
            10. **JSON格式**：确保返回的是合法JSON格式，不要包含任何markdown标记

            输出要求：
            - 会议纪要要详细、结构化，便于阅读和理解
            - 每个话题之间要有逻辑顺序
            - 待办事项要具体、可执行
            """
            .formatted(duration.replace("%", "%%"),
                      transcript.replace("%", "%%"),
                      duration.replace("%", "%%"));
    }

    /**
     * 优化转录文本（繁简转换+添加标点+语义断句）
     * 使用 GLM-5 大模型结合规则处理
     */
    private String optimizeTranscript(String rawTranscript) {
        // 先进行规则预处理
        String preprocessed = preProcessWithRules(rawTranscript);

        String prompt = """
            请将以下繁体中文转录文本转换为简体中文，并添加标点符号和段落分隔。

            原始文本：
            %s

            处理要求：
            1. 繁体转简体（台湾/香港用语转为大陆用语）
            2. 添加标点符号（根据语气添加句号、逗号、问号、感叹号）
            3. 语义分段（话题转换处添加空行分隔）
            4. 去除重复口头禅

            输出格式：只返回处理后的纯文本，段落之间用空行分隔，不要添加解释。

            处理后文本：
            """.formatted(preprocessed.replace("%", "%%"));

        try {
            log.info("开始调用 GLM-5 优化转录文本，文本长度: {} 字符", preprocessed.length());
            String optimized = chat(prompt);
            log.info("GLM-5 优化完成，返回长度: {} 字符", optimized != null ? optimized.length() : 0);
            // 清理可能的额外输出
            optimized = optimized.trim();
            if (optimized.startsWith("\"") && optimized.endsWith("\"")) {
                optimized = optimized.substring(1, optimized.length() - 1);
            }
            // 后处理：确保标点和格式规范
            return postProcessWithRules(optimized);
        } catch (Exception e) {
            log.error("GLM-5 转录文本优化失败: {}", e.getMessage(), e);
            throw new RuntimeException("GLM-5 转录文本优化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 规则预处理：基础清理
     */
    private String preProcessWithRules(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 去除多余空格
        text = text.replaceAll("\\s+", "");

        // 规范化连续重复的语气词（最多保留2个）
        text = text.replaceAll("(嗯|啊|呢|吧|吗|嘛|哦|哈|呃|那个|这个|就是){3,}", "$1$1");

        // 规范化连续重复的标点
        text = text.replaceAll("([，。！？；：、]){2,}", "$1");

        return text;
    }

    /**
     * 规则后处理：确保标点和格式规范
     */
    private String postProcessWithRules(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 确保段落末尾有标点
        String[] paragraphs = text.split("\\n");
        StringBuilder result = new StringBuilder();

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) {
                continue;
            }

            // 如果段落末尾没有标点，添加句号
            if (!para.matches(".*[。！？；：\"\")】]$")) {
                para = para + "。";
            }

            if (result.length() > 0) {
                result.append("\n\n");
            }
            result.append(para);
        }

        return result.toString();
    }

    /**
     * 计算音频时长
     */
    private String calculateDuration(List<WhisperService.Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "未知";
        }

        // 获取最后一段的结束时间
        String lastEnd = segments.get(segments.size() - 1).getEnd();
        return lastEnd;
    }

    /**
     * 将GLM-5优化后的文本应用到segments（保持时间戳，替换文本内容）
     */
    private List<WhisperService.Segment> applyOptimizedTextToSegments(
            List<WhisperService.Segment> originalSegments, String optimizedText) {
        if (originalSegments == null || originalSegments.isEmpty()) {
            return originalSegments;
        }

        // 将优化后的文本按段落分割
        String[] paragraphs = optimizedText.split("\n\\s*\n");
        if (paragraphs.length == 0) {
            paragraphs = new String[]{optimizedText};
        }

        List<WhisperService.Segment> optimizedSegments = new ArrayList<>();

        // 简单策略：按段落数量均匀分配到原始segments
        int totalOriginalSegments = originalSegments.size();
        int totalParagraphs = paragraphs.length;

        if (totalParagraphs >= totalOriginalSegments) {
            // 段落数 >= 原始segment数，每个segment分配一个段落
            for (int i = 0; i < totalOriginalSegments; i++) {
                WhisperService.Segment original = originalSegments.get(i);
                WhisperService.Segment optimized = new WhisperService.Segment();
                optimized.setStart(original.getStart());
                optimized.setEnd(original.getEnd());
                optimized.setSpeaker(original.getSpeaker());
                // 如果段落数更多，将多余的段落合并到最后一个segment
                if (i == totalOriginalSegments - 1 && totalParagraphs > totalOriginalSegments) {
                    StringBuilder combined = new StringBuilder();
                    for (int j = i; j < totalParagraphs; j++) {
                        if (combined.length() > 0) combined.append("\n\n");
                        combined.append(paragraphs[j].trim());
                    }
                    optimized.setText(combined.toString());
                } else if (i < totalParagraphs) {
                    optimized.setText(paragraphs[i].trim());
                } else {
                    optimized.setText(original.getText());
                }
                optimizedSegments.add(optimized);
            }
        } else {
            // 段落数 < 原始segment数，将多个原始segment合并
            int segmentsPerParagraph = (int) Math.ceil((double) totalOriginalSegments / totalParagraphs);
            for (int i = 0; i < totalParagraphs; i++) {
                int startIdx = i * segmentsPerParagraph;
                int endIdx = Math.min(startIdx + segmentsPerParagraph, totalOriginalSegments);

                if (startIdx < totalOriginalSegments) {
                    WhisperService.Segment first = originalSegments.get(startIdx);
                    WhisperService.Segment last = originalSegments.get(endIdx - 1);

                    WhisperService.Segment optimized = new WhisperService.Segment();
                    optimized.setStart(first.getStart());
                    optimized.setEnd(last.getEnd());
                    optimized.setSpeaker(first.getSpeaker());
                    optimized.setText(paragraphs[i].trim());
                    optimizedSegments.add(optimized);
                }
            }
        }

        log.info("文本优化应用完成: {} 个原始segments -> {} 个优化后segments",
                totalOriginalSegments, optimizedSegments.size());
        return optimizedSegments;
    }

    /**
     * 从转录segments构建说话人列表（按speaker分组）
     */
    private List<MeetingVO.Speaker> buildSpeakersFromTranscript(List<WhisperService.Segment> segments) {
        List<MeetingVO.Speaker> speakers = new ArrayList<>();

        if (segments == null || segments.isEmpty()) {
            return speakers;
        }

        // 预定义颜色列表
        String[] colors = {"#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399", "#00CED1"};

        // 按 speaker 名称分组
        Map<String, List<MeetingVO.Speaker.Segment>> speakerSegmentsMap = new HashMap<>();
        Map<String, Integer> speakerOrderMap = new HashMap<>();
        int speakerIndex = 0;

        for (WhisperService.Segment seg : segments) {
            String speakerName = seg.getSpeaker();
            if (speakerName == null || speakerName.isEmpty()) {
                speakerName = "说话人";
            }

            // 记录说话人顺序
            if (!speakerOrderMap.containsKey(speakerName)) {
                speakerOrderMap.put(speakerName, speakerIndex++);
            }

            // 构建时间段
            MeetingVO.Speaker.Segment s = new MeetingVO.Speaker.Segment();
            s.setTime(seg.getStart());
            s.setText(seg.getText());

            speakerSegmentsMap.computeIfAbsent(speakerName, k -> new ArrayList<>()).add(s);
        }

        // 创建 Speaker 对象列表，保持原始顺序
        List<String> sortedSpeakers = speakerOrderMap.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .toList();

        int colorIdx = 0;
        for (String speakerName : sortedSpeakers) {
            MeetingVO.Speaker speaker = new MeetingVO.Speaker();
            String speakerId = "speaker_" + (speakerOrderMap.get(speakerName) + 1);
            speaker.setId(speakerId);
            speaker.setName(speakerName);
            speaker.setColor(colors[colorIdx % colors.length]);
            speaker.setSegments(speakerSegmentsMap.get(speakerName));
            speakers.add(speaker);
            colorIdx++;
        }

        return speakers;
    }

    /**
     * 解析AI返回的JSON响应（支持新的飞书风格结构化格式）
     */
    private MeetingResult parseAiResponse(String aiResponse) {
        try {
            // 清理可能的markdown代码块
            String jsonStr = aiResponse.replaceAll("```json\\s*", "")
                                       .replaceAll("```\\s*", "")
                                       .trim();

            JSONObject json = JSON.parseObject(jsonStr);
            MeetingResult result = new MeetingResult();

            // 解析新的结构化格式
            if (json.containsKey("overview")) {
                // 新格式：飞书风格结构化纪要
                result.setOverview(json.getString("overview"));
                result.setSummary(json.getString("overview")); // 兼容旧字段

                // 解析话题列表
                List<TopicResult> topics = new ArrayList<>();
                List<MeetingVO.TodoItem> allTodos = new ArrayList<>();
                List<String> allDecisions = new ArrayList<>();

                if (json.containsKey("topics") && json.getJSONArray("topics") != null) {
                    json.getJSONArray("topics").forEach(item -> {
                        JSONObject obj = (JSONObject) item;
                        TopicResult topic = new TopicResult();
                        topic.setId(obj.getInteger("id"));
                        topic.setTitle(obj.getString("title"));
                        topic.setTimeRange(obj.getString("timeRange"));
                        topic.setSummary(obj.getString("summary"));

                        // 关键要点
                        if (obj.containsKey("keyPoints") && obj.getJSONArray("keyPoints") != null) {
                            List<String> keyPoints = new ArrayList<>();
                            obj.getJSONArray("keyPoints").forEach(kp -> keyPoints.add(kp.toString()));
                            topic.setKeyPoints(keyPoints);
                        }

                        // 参与说话人
                        if (obj.containsKey("speakers") && obj.getJSONArray("speakers") != null) {
                            List<String> speakers = new ArrayList<>();
                            obj.getJSONArray("speakers").forEach(s -> speakers.add(s.toString()));
                            topic.setSpeakers(speakers);
                        }

                        // 话题相关的待办
                        List<MeetingVO.TodoItem> topicTodos = new ArrayList<>();
                        if (obj.containsKey("todos") && obj.getJSONArray("todos") != null) {
                            obj.getJSONArray("todos").forEach(td -> {
                                JSONObject todoObj = (JSONObject) td;
                                MeetingVO.TodoItem todo = new MeetingVO.TodoItem();
                                todo.setId(todoObj.getInteger("id"));
                                todo.setContent(todoObj.getString("content"));
                                todo.setDone(todoObj.getBoolean("done") != null ? todoObj.getBoolean("done") : false);
                                todo.setAssignee(todoObj.getString("assignee"));
                                todo.setTopicId(topic.getId());
                                topicTodos.add(todo);
                                allTodos.add(todo);
                            });
                        }
                        topic.setTodos(topicTodos);

                        // 话题相关的决策
                        List<String> topicDecisions = new ArrayList<>();
                        if (obj.containsKey("decisions") && obj.getJSONArray("decisions") != null) {
                            obj.getJSONArray("decisions").forEach(d -> {
                                topicDecisions.add(d.toString());
                                allDecisions.add(d.toString());
                            });
                        }
                        topic.setDecisions(topicDecisions);

                        topics.add(topic);
                    });
                }
                result.setTopics(topics);
                result.setTodos(allTodos); // 兼容旧字段
                result.setDecisions(allDecisions); // 兼容旧字段

                // 解析会议统计
                if (json.containsKey("stats")) {
                    JSONObject statsObj = json.getJSONObject("stats");
                    MeetingStatsResult stats = new MeetingStatsResult();
                    stats.setTotalSpeakers(statsObj.getInteger("totalSpeakers"));
                    stats.setTotalDuration(statsObj.getString("totalDuration"));

                    if (statsObj.containsKey("speakerStats") && statsObj.getJSONArray("speakerStats") != null) {
                        List<SpeakerStatResult> speakerStats = new ArrayList<>();
                        statsObj.getJSONArray("speakerStats").forEach(ss -> {
                            JSONObject ssObj = (JSONObject) ss;
                            SpeakerStatResult stat = new SpeakerStatResult();
                            stat.setName(ssObj.getString("name"));
                            stat.setSpeakTime(ssObj.getString("speakTime"));
                            stat.setPercentage(ssObj.getInteger("percentage"));
                            speakerStats.add(stat);
                        });
                        stats.setSpeakerStats(speakerStats);
                    }
                    result.setStats(stats);
                }
            } else {
                // 旧格式：兼容处理
                result.setDuration(json.getString("duration"));
                result.setSummary(json.getString("summary"));

                // 解析待办事项
                List<MeetingVO.TodoItem> todos = new ArrayList<>();
                if (json.containsKey("todos") && json.getJSONArray("todos") != null) {
                    json.getJSONArray("todos").forEach(item -> {
                        JSONObject obj = (JSONObject) item;
                        MeetingVO.TodoItem todo = new MeetingVO.TodoItem();
                        todo.setId(obj.getInteger("id"));
                        todo.setContent(obj.getString("content"));
                        todo.setDone(obj.getBoolean("done") != null ? obj.getBoolean("done") : false);
                        todos.add(todo);
                    });
                }
                result.setTodos(todos);

                // 解析决策
                List<String> decisions = new ArrayList<>();
                if (json.containsKey("decisions") && json.getJSONArray("decisions") != null) {
                    json.getJSONArray("decisions").forEach(item -> decisions.add(item.toString()));
                }
                result.setDecisions(decisions);

                // 解析说话人
                List<MeetingVO.Speaker> speakers = new ArrayList<>();
                if (json.containsKey("speakers") && json.getJSONArray("speakers") != null) {
                    json.getJSONArray("speakers").forEach(item -> {
                        JSONObject obj = (JSONObject) item;
                        MeetingVO.Speaker speaker = new MeetingVO.Speaker();
                        speaker.setId(obj.getString("id"));
                        speaker.setName(obj.getString("name"));
                        speaker.setColor(obj.getString("color"));

                        List<MeetingVO.Speaker.Segment> segments = new ArrayList<>();
                        if (obj.containsKey("segments") && obj.getJSONArray("segments") != null) {
                            obj.getJSONArray("segments").forEach(seg -> {
                                JSONObject segObj = (JSONObject) seg;
                                MeetingVO.Speaker.Segment s = new MeetingVO.Speaker.Segment();
                                s.setTime(segObj.getString("time"));
                                s.setText(segObj.getString("text"));
                                segments.add(s);
                            });
                        }
                        speaker.setSegments(segments);
                        speakers.add(speaker);
                    });
                }
                result.setSpeakers(speakers);
            }

            return result;
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", aiResponse, e);
            throw new RuntimeException("解析AI响应失败: " + e.getMessage(), e);
        }
    }


    /**
     * 调用Kimi Chat API
     */
    public String chat(String prompt) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("开始调用 GLM-5 API, model={}, prompt长度={}", model, prompt.length());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", List.of(message));
            body.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.debug("请求URL: {}/chat/completions", baseUrl);
            ResponseEntity<String> response = getRestTemplate().postForEntity(
                    baseUrl + "/chat/completions",
                    entity,
                    String.class
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("GLM-5 API 调用完成, 耗时: {}ms, 状态: {}", duration, response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject json = JSON.parseObject(response.getBody());
                String content = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                log.debug("API 响应内容长度: {} 字符", content != null ? content.length() : 0);
                return content;
            }

            throw new RuntimeException("API调用失败: " + response.getStatusCode());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("GLM-5 API调用失败, 耗时: {}ms, 错误: {}", duration, e.getMessage(), e);
            throw new RuntimeException("AI处理失败", e);
        }
    }

    private MeetingVO.TodoItem createTodo(Integer id, String content, boolean done) {
        MeetingVO.TodoItem todo = new MeetingVO.TodoItem();
        todo.setId(id);
        todo.setContent(content);
        todo.setDone(done);
        return todo;
    }

    private MeetingVO.Speaker createSpeaker(String id, String name, String color, String[][] segments) {
        MeetingVO.Speaker speaker = new MeetingVO.Speaker();
        speaker.setId(id);
        speaker.setName(name);
        speaker.setColor(color);

        List<MeetingVO.Speaker.Segment> segmentList = new ArrayList<>();
        for (String[] seg : segments) {
            MeetingVO.Speaker.Segment s = new MeetingVO.Speaker.Segment();
            s.setTime(seg[0]);
            s.setText(seg[1]);
            segmentList.add(s);
        }
        speaker.setSegments(segmentList);
        return speaker;
    }

    @Data
    public static class MeetingResult {
        private String duration;
        private String summary; // 兼容旧格式：会议整体概述
        private List<MeetingVO.TodoItem> todos; // 兼容旧格式：所有待办事项的扁平列表
        private List<String> decisions; // 兼容旧格式：所有决策的扁平列表
        private List<MeetingVO.Speaker> speakers;

        // 新增：飞书风格结构化会议纪要
        private String overview; // 会议整体概述
        private List<TopicResult> topics; // 按话题分段
        private MeetingStatsResult stats; // 会议统计
    }

    @Data
    public static class TopicResult {
        private Integer id;
        private String title; // 话题标题
        private String timeRange; // 时间范围
        private String summary; // 详细总结
        private List<String> keyPoints; // 关键要点
        private List<String> speakers; // 参与该话题的说话人
        private List<MeetingVO.TodoItem> todos; // 该话题相关的待办
        private List<String> decisions; // 该话题相关的决策
    }

    @Data
    public static class MeetingStatsResult {
        private Integer totalSpeakers;
        private String totalDuration;
        private List<SpeakerStatResult> speakerStats;
    }

    @Data
    public static class SpeakerStatResult {
        private String name;
        private String speakTime;
        private Integer percentage;
    }
}
