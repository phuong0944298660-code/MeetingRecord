#!/usr/bin/env python3
"""
OpenAI Whisper 语音识别脚本 - 纯净JSON输出版
"""

import sys
import io
import json
import os
import warnings
import argparse
import re

# 尝试导入繁简转换库
try:
    import opencc
    CONVERTER_T2S = opencc.OpenCC('t2s')  # 繁体转简体
    HAS_OPENCC = True
except ImportError:
    HAS_OPENCC = False
    print("WARN: opencc-python 未安装，跳过繁简转换", file=sys.stderr)

# 设置UTF-8编码
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

# 立即重定向stdout到stderr，防止任何库输出污染JSON
_original_stdout = sys.stdout
sys.stdout = sys.stderr

# 添加FFmpeg到PATH（从环境变量读取，如果未设置则使用系统PATH）
ffmpeg_path = os.environ.get("FFMPEG_PATH", "")
if ffmpeg_path and os.path.exists(ffmpeg_path):
    os.environ["PATH"] = ffmpeg_path + os.pathsep + os.environ.get("PATH", "")

# 设置模型缓存路径（从环境变量读取，如果未设置则使用默认路径）
whisper_cache = os.environ.get("WHISPER_CACHE_DIR", "")
if whisper_cache:
    os.environ["WHISPER_CACHE_DIR"] = whisper_cache

hf_home = os.environ.get("HF_HOME", "")
if hf_home:
    os.environ["HF_HOME"] = hf_home
    os.environ["HUGGINGFACE_HUB_CACHE"] = os.path.join(hf_home, "hub")

# 禁用警告
warnings.filterwarnings('ignore')


def convert_to_simplified(text):
    """繁体中文转简体中文"""
    if HAS_OPENCC and text:
        try:
            return CONVERTER_T2S.convert(text)
        except Exception as e:
            print(f"WARN: 繁简转换失败: {e}", file=sys.stderr)
            return text
    return text


def add_punctuation(text, segments=None):
    """
    为文本添加标点符号
    基于停顿时间和语义简单规则添加标点
    """
    if not text:
        return text

    # 如果文本已经有标点，直接返回
    if any(c in text for c in '。，！？；：""''（）'):
        return text

    # 基于停顿时间添加标点（如果提供了segments）
    if segments and len(segments) > 1:
        result_segments = []
        for i, seg in enumerate(segments):
            seg_text = seg.get('text', '').strip()
            if not seg_text:
                continue

            # 转换简体
            seg_text = convert_to_simplified(seg_text)

            # 根据停顿时间判断标点
            if i < len(segments) - 1:
                current_end = seg.get('end', 0)
                next_start = segments[i + 1].get('start', 0)
                gap = next_start - current_end if isinstance(next_start, (int, float)) and isinstance(current_end, (int, float)) else 0

                # 长停顿加句号，短停顿加逗号
                if gap > 1.5:
                    seg_text = seg_text.rstrip('，') + '。'
                elif gap > 0.5:
                    if not seg_text.endswith('，') and not seg_text.endswith('。'):
                        seg_text += '，'

            result_segments.append(seg_text)

        # 合并段落
        result = ''.join(result_segments)
    else:
        # 无时间信息时，简单按语义分段
        result = convert_to_simplified(text)
        # 每20-30字尝试添加标点（简单策略）
        result = simple_punctuation(result)

    # 确保结尾有句号
    if result and not result[-1] in '。！？；：""''）】':
        result += '。'

    return result


def simple_punctuation(text):
    """简单标点添加：基于常见语气词和长度"""
    # 常见句末词后加句号
    sentence_endings = ['的', '了', '是', '呢', '啊', '吧', '吗', '嘛', '哦', '哈',
                       '好', '行', '可以', '对', '没错', '嗯', '对']

    result = []
    last_cut = 0

    for i, char in enumerate(text):
        result.append(char)

        # 每25-35字左右尝试断句
        if i - last_cut > 25:
            if char in sentence_endings:
                result.append('。')
                last_cut = i + 1
            elif i - last_cut > 35:  # 强制断句
                result.append('，')
                last_cut = i + 1

    return ''.join(result)


def process_segments(segments):
    """处理转录段落：繁简转换 + 基础标点清理"""
    if not segments:
        return segments

    processed = []
    for seg in segments:
        try:
            new_seg = seg.copy()
            text = seg.get('text', '')

            # 繁转简
            text = convert_to_simplified(text)

            # 清理多余的空格和重复的语气词
            text = clean_text(text)

            new_seg['text'] = text
            processed.append(new_seg)
        except Exception as e:
            print(f"WARN: 段落处理失败: {e}", file=sys.stderr)
            processed.append(seg)

    return processed


def clean_text(text):
    """清理文本：去除多余空格、规范重复语气词"""
    if not text:
        return text

    # 去除多余空格
    text = re.sub(r'\s+', '', text)

    # 规范化连续重复的语气词（最多保留2个）
    text = re.sub(r'(嗯|啊|呢|吧|吗|嘛|哦|哈|呃){3,}', r'\1\1', text)

    # 规范化连续重复的标点
    text = re.sub(r'([，。！？；：]){2,}', r'\1', text)

    return text.strip()

def transcribe_audio(audio_path, model_name="base", language="zh", speaker_diarization=True):
    """转录音频文件，支持说话人分离"""
    import whisper
    import torch

    if not os.path.exists(audio_path):
        return {"error": f"文件不存在: {audio_path}"}

    try:
        device = "cuda" if torch.cuda.is_available() else "cpu"
        gpu_name = torch.cuda.get_device_name(0) if torch.cuda.is_available() else None

        print(f"INFO: PyTorch={torch.__version__}, Device={device}", file=sys.stderr)
        if gpu_name:
            print(f"INFO: GPU={gpu_name}, CUDA={torch.version.cuda}", file=sys.stderr)

        # 输出进度：开始加载模型 (10%)
        print("PROGRESS: 10|正在加载语音识别模型...", file=sys.stderr)
        model = whisper.load_model(model_name, device=device)

        # 输出进度：模型加载完成 (30%)
        print("PROGRESS: 30|模型加载完成...", file=sys.stderr)

        # 说话人分离
        speaker_segments = None
        if speaker_diarization:
            try:
                print("PROGRESS: 35|正在加载说话人分离模型...", file=sys.stderr)
                speaker_segments = perform_speaker_diarization(audio_path, device)
                if speaker_segments:
                    print(f"INFO: 检测到 {len(set(s['speaker'] for s in speaker_segments))} 个说话人", file=sys.stderr)
            except Exception as e:
                print(f"WARN: 说话人分离失败: {e}", file=sys.stderr)

        # 输出进度：开始转录 (40%)
        print("PROGRESS: 40|正在转录音频...", file=sys.stderr)

        result = model.transcribe(
            audio_path,
            language=language,
            verbose=False,
            word_timestamps=True  # 需要字级时间戳来对齐说话人
        )

        # 输出进度：转录完成 (80%)
        print("PROGRESS: 80|转录完成，正在处理结果...", file=sys.stderr)

        # 合并说话人信息和转录结果
        segments = merge_speaker_with_transcript(result["segments"], speaker_segments)

        # 处理文本：繁简转换（标点符号由后端大模型处理）
        print("PROGRESS: 85|正在转换文本格式...", file=sys.stderr)
        segments = process_segments(segments)

        # 重新构建完整文本
        full_text = ''.join(seg.get('text', '') for seg in segments)

        # 输出进度：全部完成 (100%)
        print("PROGRESS: 100|语音识别完成", file=sys.stderr)

        return {
            "success": True,
            "text": full_text,
            "segments": segments,
            "language": result.get("language", language),
            "device": device,
            "gpu": gpu_name,
            "speaker_count": len(set(s.get("speaker", "未知") for s in segments)) if segments else 0
        }

    except Exception as e:
        import traceback
        return {
            "success": False,
            "error": str(e),
            "traceback": traceback.format_exc()
        }


def perform_speaker_diarization(audio_path, device="cpu"):
    """
    执行说话人分离（基于音色特征）
    优先使用 pyannote.audio 进行真实的音色分离，备选使用基于停顿的分割
    """
    try:
        import torch
        from pyannote.audio import Pipeline

        print("INFO: 加载 pyannote 说话人分离模型...", file=sys.stderr)
        print("INFO: 使用设备: " + device, file=sys.stderr)

        # 使用 pyannote/speaker-diarization-3.1 模型
        # 注意：首次使用需要下载模型，会从 HuggingFace 下载
        pipeline = Pipeline.from_pretrained(
            "pyannote/speaker-diarization-3.1",
            token=os.environ.get("HF_TOKEN", None)
        )

        if device == "cuda" and torch.cuda.is_available():
            pipeline.to(torch.device("cuda"))
            print("INFO: 说话人分离使用 GPU 加速", file=sys.stderr)
        else:
            pipeline.to(torch.device("cpu"))
            print("INFO: 说话人分离使用 CPU", file=sys.stderr)

        print("INFO: 执行音色分析...", file=sys.stderr)
        diarization = pipeline(audio_path)

        segments = []
        speaker_count = set()

        for turn, _, speaker in diarization.itertracks(yield_label=True):
            segments.append({
                "start": turn.start,
                "end": turn.end,
                "speaker": speaker
            })
            speaker_count.add(speaker)

        print(f"INFO: 说话人分离完成，检测到 {len(speaker_count)} 个说话人", file=sys.stderr)
        return segments

    except ImportError as e:
        print(f"WARN: pyannote.audio 未安装 ({e})，使用基于停顿的简单分割", file=sys.stderr)
        return simple_speaker_segmentation(audio_path)
    except Exception as e:
        print(f"WARN: pyannote 说话人分离失败: {e}", file=sys.stderr)
        print("WARN: 切换到备选方案（基于停顿的分割）", file=sys.stderr)
        return simple_speaker_segmentation(audio_path)


def simple_speaker_segmentation(audio_path, silence_threshold=1.5, min_speech_duration=0.5):
    """
    改进的基于停顿的说话人分割（备选方案）
    基于语音停顿来推测说话人切换，假设会议通常为2-3人轮流发言
    """
    try:
        import librosa
        import numpy as np

        print("INFO: 使用基于停顿的简单分割...", file=sys.stderr)

        # 加载音频
        y, sr = librosa.load(audio_path, sr=None)

        # 使用更敏感的语音活动检测
        intervals = librosa.effects.split(y, top_db=25)

        segments = []
        current_speaker = 0
        last_end = 0
        speaker_change_count = 0

        for i, (start, end) in enumerate(intervals):
            start_sec = start / sr
            end_sec = end / sr
            duration = end_sec - start_sec

            # 过滤过短的语音片段（可能是噪音）
            if duration < min_speech_duration:
                continue

            # 计算停顿时间
            pause_duration = start_sec - last_end if i > 0 else 0

            # 根据停顿时间和发言规律判断说话人切换
            if i > 0 and pause_duration > silence_threshold:
                # 长停顿，大概率是说话人切换
                # 会议场景下，假设2-4人轮流发言
                speaker_change_count += 1
                if speaker_change_count % 2 == 0:
                    current_speaker = (current_speaker + 1) % 2  # 2人会议
                else:
                    current_speaker = (current_speaker + 1) % 3  # 3人会议

            segments.append({
                "start": start_sec,
                "end": end_sec,
                "speaker": f"SPEAKER_{current_speaker:02d}"
            })

            last_end = end_sec

        speaker_count = len(set(s['speaker'] for s in segments))
        print(f"INFO: 简单分割完成，检测到 {speaker_count} 个说话人", file=sys.stderr)

        return segments if segments else None

    except ImportError as e:
        print(f"WARN: librosa 未安装，无法进行简单分割: {e}", file=sys.stderr)
        return None
    except Exception as e:
        print(f"WARN: 简单分割失败: {e}", file=sys.stderr)
        return None


def merge_speaker_with_transcript(whisper_segments, speaker_segments):
    """
    将 Whisper 的转录结果与说话人分离结果合并
    使用基于时间戳重叠的算法匹配说话人
    """
    segments = []

    for seg in whisper_segments:
        seg_start = seg.get("start", 0)
        if isinstance(seg_start, str):
            # 转换时间字符串为秒数
            seg_start = parse_time_to_seconds(seg_start)

        seg_end = seg.get("end", 0)
        if isinstance(seg_end, str):
            seg_end = parse_time_to_seconds(seg_end)

        seg_text = seg.get("text", "").strip()

        if not seg_text:
            continue

        # 确定这段文本属于哪个说话人
        speaker = "说话人"
        best_overlap_ratio = 0  # 初始化变量
        if speaker_segments:
            # 找到与这段时间重叠最多的说话人
            best_speaker = None
            best_overlap = 0

            for spk_seg in speaker_segments:
                spk_start = spk_seg.get("start", 0)
                spk_end = spk_seg.get("end", 0)

                # 计算时间重叠
                overlap_start = max(seg_start, spk_start)
                overlap_end = min(seg_end, spk_end)
                overlap = max(0, overlap_end - overlap_start)

                # 计算重叠比例（相对于转录片段）
                seg_duration = seg_end - seg_start
                overlap_ratio = overlap / seg_duration if seg_duration > 0 else 0

                # 优先选择重叠比例高的，但也要有最小重叠时长
                if overlap > best_overlap and overlap_ratio > 0.3:
                    best_overlap = overlap
                    best_overlap_ratio = overlap_ratio
                    best_speaker = spk_seg["speaker"]

            if best_speaker:
                # 简化说话人名称：SPEAKER_00 -> 说话人1
                speaker_num = best_speaker.replace("SPEAKER_", "").lstrip("0")
                if not speaker_num:
                    speaker_num = "1"
                speaker = f"说话人{speaker_num}"

        segments.append({
            "start": format_time(seg_start),
            "end": format_time(seg_end),
            "text": seg_text,
            "speaker": speaker,
            "speaker_confidence": round(best_overlap_ratio, 2) if best_overlap_ratio > 0 else None
        })

    # 合并相邻的同说话人段落
    return merge_consecutive_speakers(segments)


def parse_time_to_seconds(time_str):
    """将时间字符串转换为秒数"""
    try:
        parts = time_str.split(':')
        if len(parts) == 3:
            hours, minutes, seconds = parts
            return int(hours) * 3600 + int(minutes) * 60 + float(seconds)
        elif len(parts) == 2:
            minutes, seconds = parts
            return int(minutes) * 60 + float(seconds)
        else:
            return float(time_str)
    except:
        return 0


def merge_consecutive_speakers(segments):
    """合并相邻的同说话人段落"""
    if not segments:
        return segments

    merged = []
    current = segments[0].copy()

    for seg in segments[1:]:
        if seg["speaker"] == current["speaker"]:
            # 合并文本和时间
            current["end"] = seg["end"]
            current["text"] += seg["text"]
        else:
            merged.append(current)
            current = seg.copy()

    merged.append(current)
    return merged

def format_time(seconds):
    """将秒数格式化为 HH:MM:SS"""
    hours = int(seconds // 3600)
    minutes = int((seconds % 3600) // 60)
    secs = int(seconds % 60)
    return f"{hours:02d}:{minutes:02d}:{secs:02d}"

if __name__ == "__main__":
    # 解析参数（此时stdout已重定向到stderr）
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", "-i", required=True)
    parser.add_argument("--model", "-m", default="base")
    parser.add_argument("--language", "-l", default="zh")
    parser.add_argument("--output", "-o")
    args = parser.parse_args()

    # 执行转录
    result = transcribe_audio(args.input, args.model, args.language)

    # 恢复stdout以便输出纯净的JSON
    sys.stdout = _original_stdout

    # 输出JSON
    json_output = json.dumps(result, ensure_ascii=False, indent=2)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(json_output)
    else:
        sys.stdout.write(json_output)
        sys.stdout.flush()
