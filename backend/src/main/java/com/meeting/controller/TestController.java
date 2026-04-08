package com.meeting.controller;

import com.meeting.service.WhisperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 诊断测试接口
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final WhisperService whisperService;
    private final Environment env;

    /**
     * 测试Whisper配置
     */
    @GetMapping("/whisper-config")
    public Map<String, Object> testWhisperConfig() {
        Map<String, Object> result = new HashMap<>();

        // 使用反射读取WhisperService中的配置值
        try {
            java.lang.reflect.Field pythonPathField = WhisperService.class.getDeclaredField("pythonPath");
            pythonPathField.setAccessible(true);
            result.put("pythonPathFromService", pythonPathField.get(whisperService));

            java.lang.reflect.Field scriptPathField = WhisperService.class.getDeclaredField("scriptPath");
            scriptPathField.setAccessible(true);
            result.put("scriptPathFromService", scriptPathField.get(whisperService));

            java.lang.reflect.Field modelField = WhisperService.class.getDeclaredField("model");
            modelField.setAccessible(true);
            result.put("modelFromService", modelField.get(whisperService));
        } catch (Exception e) {
            result.put("reflectionError", e.getMessage());
        }

        // 从Environment读取
        result.put("pythonPathFromEnv", env.getProperty("whisper.pythonPath", "NOT_FOUND"));
        result.put("scriptPathFromEnv", env.getProperty("whisper.scriptPath", "NOT_FOUND"));
        result.put("modelFromEnv", env.getProperty("whisper.model", "NOT_FOUND"));

        // 检查文件是否存在
        String scriptPath = env.getProperty("whisper.scriptPath", "whisper/transcribe.py");
        File scriptFile = new File(scriptPath);
        result.put("scriptExists", scriptFile.exists());
        result.put("scriptAbsolutePath", scriptFile.getAbsolutePath());

        // 检查Python版本
        String pythonPath = env.getProperty("whisper.pythonPath", "python");
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonPath, "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            result.put("pythonExitCode", exitCode);
        } catch (Exception e) {
            result.put("pythonError", e.getMessage());
        }

        // 检查CUDA
        try {
            ProcessBuilder pb = new ProcessBuilder(
                pythonPath,
                "-c",
                "import torch; print(f'CUDA: {torch.cuda.is_available()}, GPU: {torch.cuda.get_device_name(0) if torch.cuda.is_available() else \"None\"}')"
            );
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            process.waitFor();
            result.put("cudaInfo", output.toString());
        } catch (Exception e) {
            result.put("cudaError", e.getMessage());
        }

        return result;
    }

    /**
     * 测试Whisper转录（使用测试音频）
     */
    @GetMapping("/whisper-test")
    public Map<String, Object> testWhisper() {
        Map<String, Object> result = new HashMap<>();

        // 创建一个简单的测试音频文件
        String testAudioPath = createTestAudioFile();
        result.put("testAudioPath", testAudioPath);

        if (testAudioPath == null) {
            result.put("error", "无法创建测试音频文件");
            return result;
        }

        // 调用Whisper转录
        try {
            log.info("开始测试Whisper转录...");
            var transcribeResult = whisperService.transcribe(testAudioPath);

            result.put("success", transcribeResult.isSuccess());
            result.put("text", transcribeResult.getText());
            result.put("error", transcribeResult.getError());
            result.put("language", transcribeResult.getLanguage());
            result.put("segmentCount", transcribeResult.getSegments() != null ? transcribeResult.getSegments().size() : 0);

            // 删除测试文件
            new File(testAudioPath).delete();
        } catch (Exception e) {
            log.error("Whisper测试失败", e);
            result.put("error", e.getMessage());
            result.put("trace", e.getStackTrace());
        }

        return result;
    }

    /**
     * 创建测试音频文件（静音）
     */
    private String createTestAudioFile() {
        try {
            String uploadDir = System.getProperty("user.home") + "/meeting-record/uploads";
            new File(uploadDir).mkdirs();
            String testFile = uploadDir + "/test_silence.wav";

            // 使用ffmpeg创建一个1秒的静音wav文件
            ProcessBuilder pb = new ProcessBuilder(
                "C:/Users/PC/ffmpeg-8.1-essentials_build/bin/ffmpeg.exe",
                "-f", "lavfi",
                "-i", "anullsrc=r=16000:cl=mono",
                "-t", "1",
                "-acodec", "pcm_s16le",
                "-y",
                testFile
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && new File(testFile).exists()) {
                return testFile;
            }
        } catch (Exception e) {
            log.error("创建测试音频失败", e);
        }
        return null;
    }
}
