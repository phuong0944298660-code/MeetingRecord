#!/usr/bin/env python3
import warnings
warnings.filterwarnings('ignore')

print("=" * 60)
print("Download pyannote speaker diarization model")
print("=" * 60)

try:
    import os
    import torch
    print(f"PyTorch version: {torch.__version__}")
    print(f"CUDA available: {torch.cuda.is_available()}")

    from pyannote.audio import Pipeline

    print("\nDownloading pyannote/speaker-diarization-3.1 model...")
    print("(This may take 5-10 minutes, please wait...)\n")

    # 从环境变量读取 HuggingFace token
    hf_token = os.environ.get("HF_TOKEN", "")

    if not hf_token:
        print("[ERROR] HF_TOKEN environment variable is not set!")
        print("\nPlease set HF_TOKEN environment variable:")
        print("  Windows: set HF_TOKEN=your_token_here")
        print("  Linux/Mac: export HF_TOKEN=your_token_here")
        print("\nGet your token at: https://huggingface.co/settings/tokens")
        exit(1)

    print("Using HF_TOKEN to download model...")
    pipeline = Pipeline.from_pretrained(
        "pyannote/speaker-diarization-3.1",
        token=hf_token
    )

    print("\n[OK] pyannote model download complete!")

except ImportError as e:
    print(f"[ERROR] Import error: {e}")
except Exception as e:
    print(f"[ERROR] Download failed: {e}")
    print("\nNote: May need HuggingFace token")
    print("Visit https://huggingface.co/settings/tokens")
    import traceback
    traceback.print_exc()
