#!/usr/bin/env python3
"""测试GPU是否可用"""
import sys
import torch
import whisper

print(f"PyTorch版本: {torch.__version__}", file=sys.stderr)
print(f"CUDA可用: {torch.cuda.is_available()}", file=sys.stderr)

if torch.cuda.is_available():
    print(f"CUDA版本: {torch.version.cuda}", file=sys.stderr)
    print(f"GPU: {torch.cuda.get_device_name(0)}", file=sys.stderr)
    print(f"显存: {torch.cuda.get_device_properties(0).total_memory / 1024**3:.1f} GB", file=sys.stderr)

    # 测试加载Whisper模型到GPU
    print("正在加载Whisper base模型到GPU...", file=sys.stderr)
    model = whisper.load_model("base", device="cuda")
    print("模型加载成功！", file=sys.stderr)

    # 输出成功信息给Java调用方
    print('{"success": true, "device": "cuda", "gpu": "' + torch.cuda.get_device_name(0) + '"}')
else:
    print('{"success": false, "error": "CUDA不可用"}')
