#!/usr/bin/env python3
"""
预下载 Whisper 和 pyannote.audio 模型
避免运行时下载等待
"""

import sys
import os

# 添加FFmpeg到PATH
ffmpeg_path = "C:/Users/PC/ffmpeg-8.1-essentials_build/bin"
if os.path.exists(ffmpeg_path):
    os.environ["PATH"] = ffmpeg_path + os.pathsep + os.environ.get("PATH", "")

def download_whisper_model(model_name="base"):
    """下载 Whisper 模型"""
    print(f"=" * 60)
    print(f"下载 Whisper 模型: {model_name}")
    print(f"=" * 60)

    try:
        import whisper
        print(f"正在下载 whisper {model_name} 模型...")
        model = whisper.load_model(model_name)
        print(f"✓ Whisper {model_name} 模型下载完成！")
        return True
    except Exception as e:
        print(f"✗ Whisper 模型下载失败: {e}")
        return False

def download_pyannote_models():
    """下载 pyannote.audio 说话人分离模型"""
    print(f"\n" + "=" * 60)
    print(f"下载 pyannote.audio 说话人分离模型")
    print(f"=" * 60)

    try:
        from pyannote.audio import Pipeline
        import torch

        print("正在下载 pyannote/speaker-diarization-3.1 模型...")
        print("(这可能需要几分钟时间，请耐心等待...)")

        # 下载模型
        pipeline = Pipeline.from_pretrained(
            "pyannote/speaker-diarization-3.1",
            use_auth_token=False
        )

        print("✓ pyannote 说话人分离模型下载完成！")
        return True

    except ImportError:
        print("✗ pyannote.audio 未安装，请先运行: pip install pyannote.audio")
        return False
    except Exception as e:
        print(f"✗ pyannote 模型下载失败: {e}")
        print("提示: 可能需要 HuggingFace token 才能下载")
        print("可以通过环境变量设置: export HF_TOKEN=your_token")
        return False

def download_librosa_data():
    """确保 librosa 依赖可用"""
    print(f"\n" + "=" * 60)
    print(f"检查 librosa 音频处理库")
    print(f"=" * 60)

    try:
        import librosa
        import numpy as np
        print("✓ librosa 和 numpy 已安装")
        return True
    except ImportError as e:
        print(f"✗ 缺少依赖: {e}")
        print("请运行: pip install librosa numpy")
        return False

def main():
    print("=" * 60)
    print("会议纪要系统 - 模型预下载工具")
    print("=" * 60)
    print()

    results = {}

    # 1. 下载 Whisper 模型
    results['whisper'] = download_whisper_model("base")

    # 2. 下载 pyannote 模型
    results['pyannote'] = download_pyannote_models()

    # 3. 检查 librosa
    results['librosa'] = download_librosa_data()

    # 总结
    print(f"\n" + "=" * 60)
    print("下载结果汇总")
    print("=" * 60)

    for name, success in results.items():
        status = "✓ 成功" if success else "✗ 失败"
        print(f"{name:15s}: {status}")

    print()

    # 如果 pyannote 失败，提供备选方案
    if not results['pyannote']:
        print("提示: pyannote 模型下载失败，系统将使用基于停顿的简单分割方案")
        print("      这也能实现说话人分离，但准确度可能略低。")
        print()

    # 总体结果
    if results['whisper'] and results['librosa']:
        print("✓ 核心模型已准备就绪，可以启动项目！")
        return 0
    else:
        print("✗ 部分模型下载失败，请检查错误信息后重试")
        return 1

if __name__ == "__main__":
    sys.exit(main())
