package com.meeting.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.meeting.dto.MeetingProgressDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * OpenAI Whisper 语音识别服务
 * 调用本地 Python 脚本进行音频转录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhisperService {

    @Value("${whisper.pythonPath:python}")
    private String pythonPath;

    @Value("${whisper.scriptPath:whisper/transcribe.py}")
    private String scriptPath;

    @Value("${whisper.model:base}")
    private String model;

    private final MeetingProgressService progressService;

    /**
     * 转录音频文件为文字（不带进度推送）
     */
    public TranscriptionResult transcribe(String audioPath) {
        return transcribe(audioPath, null, null);
    }

    /**
     * 转录音频文件为文字（带进度推送）
     *
     * @param audioPath 音频文件绝对路径
     * @param meetingId 会议ID，用于推送进度
     * @param progressCallback 进度回调函数
     * @return TranscriptionResult 转录结果
     */
    public TranscriptionResult transcribe(String audioPath, Long meetingId, Consumer<MeetingProgressDTO> progressCallback) {
        log.info("开始 Whisper 转录: {}, meetingId: {}", audioPath, meetingId);

        try {
            // 推送开始消息
            if (meetingId != null) {
                progressService.sendWhisperStart(meetingId);
            }
            if (progressCallback != null) {
                progressCallback.accept(MeetingProgressDTO.whisper(meetingId, 0, "开始语音识别..."));
            }

            // 构建命令
            List<String> command = new ArrayList<>();
            command.add(getPythonExecutable());
            command.add(getScriptAbsolutePath());
            command.add("--input");
            command.add(audioPath);
            command.add("--model");
            command.add(model);
            command.add("--language");
            command.add("zh");

            log.debug("执行命令: {}", String.join(" ", command));

            // 执行 Python 脚本，并添加 ffmpeg 到 PATH
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            // 设置环境变量，添加 ffmpeg 路径
            Map<String, String> env = pb.environment();
            String currentPath = env.get("PATH");
            String ffmpegPath = "C:/Users/PC/ffmpeg-8.1-essentials_build/bin";
            if (currentPath != null) {
                env.put("PATH", ffmpegPath + ";" + currentPath);
            } else {
                env.put("PATH", ffmpegPath);
            }
            log.debug("PATH 环境变量: {}", env.get("PATH"));

            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 检查是否是进度消息
                    if (line.startsWith("PROGRESS: ")) {
                        String[] parts = line.substring(10).split("\\|", 2);
                        if (parts.length == 2) {
                            try {
                                int progress = Integer.parseInt(parts[0].trim());
                                String message = parts[1].trim();
                                log.debug("转录进度: {}% - {}", progress, message);

                                // 推送进度
                                if (meetingId != null) {
                                    progressService.sendWhisperProgress(meetingId, progress, message);
                                }
                                if (progressCallback != null) {
                                    progressCallback.accept(MeetingProgressDTO.whisper(meetingId, progress, message));
                                }
                            } catch (NumberFormatException e) {
                                log.warn("无法解析进度: {}", line);
                            }
                        }
                    } else {
                        // 普通输出，累积到结果中
                        output.append(line);
                    }
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Whisper 转录失败，退出码: {}", exitCode);
                log.error("Whisper 错误输出: {}", output.toString());
                String error = "转录失败，退出码: " + exitCode + "，输出: " + output.toString();
                if (meetingId != null) {
                    progressService.sendError(meetingId, error);
                }
                return TranscriptionResult.error(error);
            }

            // 推送转录完成
            if (meetingId != null) {
                progressService.sendWhisperComplete(meetingId);
            }
            if (progressCallback != null) {
                progressCallback.accept(MeetingProgressDTO.whisper(meetingId, 100, "语音识别完成"));
            }

            // 解析 JSON 结果
            String outputStr = output.toString();
            log.debug("Whisper 原始输出: {}", outputStr);

            // 从输出中提取 JSON（如果有日志信息混在其中）
            String jsonStr = extractJsonFromOutput(outputStr);
            log.debug("Whisper JSON: {}", jsonStr);

            JSONObject json = JSON.parseObject(jsonStr);

            if (json.containsKey("error")) {
                log.error("Whisper 转录错误: {}", json.getString("error"));
                String error = json.getString("error");
                if (meetingId != null) {
                    progressService.sendError(meetingId, error);
                }
                return TranscriptionResult.error(error);
            }

            // 构建结果
            TranscriptionResult result = new TranscriptionResult();
            result.setSuccess(true);
            result.setText(json.getString("text"));
            result.setLanguage(json.getString("language"));

            // 解析时间段
            List<Segment> segments = new ArrayList<>();
            if (json.containsKey("segments")) {
                for (Object obj : json.getJSONArray("segments")) {
                    JSONObject segJson = (JSONObject) obj;
                    Segment seg = new Segment();
                    seg.setStart(segJson.getString("start"));
                    seg.setEnd(segJson.getString("end"));
                    seg.setText(segJson.getString("text"));
                    seg.setSpeaker(segJson.getString("speaker"));
                    segments.add(seg);
                }
            }
            result.setSegments(segments);

            log.info("Whisper 转录完成，共 {} 段文字", segments.size());
            return result;

        } catch (Exception e) {
            log.error("Whisper 转录异常", e);
            String error = e.getMessage();
            if (meetingId != null) {
                progressService.sendError(meetingId, error);
            }
            return TranscriptionResult.error(error);
        }
    }

    /**
     * 获取 Python 可执行文件路径
     */
    private String getPythonExecutable() {
        // 优先使用配置的 Python 路径
        if (pythonPath != null && !pythonPath.isEmpty()) {
            return pythonPath;
        }

        // 尝试常见路径
        String[] possiblePaths = {
            "python",
            "python3",
            "C:/Python314/python.exe",
            "C:/Users/PC/AppData/Local/Programs/Python/Python314/python.exe"
        };

        for (String path : possiblePaths) {
            if (isCommandAvailable(path)) {
                return path;
            }
        }

        return "python"; // 默认
    }

    /**
     * 获取脚本的绝对路径
     */
    private String getScriptAbsolutePath() {
        // 如果配置的是相对路径，则基于工作目录
        File scriptFile = new File(scriptPath);
        if (scriptFile.isAbsolute()) {
            return scriptPath;
        }

        // 尝试在 backend 目录下查找
        File backendScript = new File("backend/" + scriptPath);
        if (backendScript.exists()) {
            return backendScript.getAbsolutePath();
        }

        // 尝试在 whisper 目录下查找
        File whisperScript = new File("whisper/transcribe.py");
        if (whisperScript.exists()) {
            return whisperScript.getAbsolutePath();
        }

        return scriptPath;
    }

    /**
     * 检查命令是否可用
     */
    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 转录结果
     */
    public static class TranscriptionResult {
        private boolean success;
        private String text;
        private String language;
        private String error;
        private List<Segment> segments;

        public static TranscriptionResult error(String message) {
            TranscriptionResult r = new TranscriptionResult();
            r.setSuccess(false);
            r.setError(message);
            return r;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public List<Segment> getSegments() { return segments; }
        public void setSegments(List<Segment> segments) { this.segments = segments; }
    }

    /**
     * 转录片段
     */
    public static class Segment {
        private String start;
        private String end;
        private String text;
        private String speaker;

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }
        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getSpeaker() { return speaker; }
        public void setSpeaker(String speaker) { this.speaker = speaker; }
    }

    /**
     * 从混合输出中提取 JSON
     */
    private String extractJsonFromOutput(String output) {
        // 找到第一个 { 和最后一个 }
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return output.substring(start, end + 1);
        }

        // 如果没有找到，返回原始输出（可能会解析失败）
        return output;
    }
}
