# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Initial release: 配额管家 — 聚合查询多家 AI 服务额度的 Miuix 风格 Android 应用
- 12 built-in provider presets (NewAPI, OpenAI, DeepSeek, Kimi, GLM, MiniMax, OpenRouter, SiliconFlow, Anthropic OAuth, Codex OAuth, Gemini OAuth, empty template)
- 100+ provider logos bundled from cc-switch
- Generic JSON-DSL extractor with JSONPath + arithmetic + transforms
- Auto-refresh loop driven by user-configurable interval
- Per-service manual refresh + enable/disable toggle
- Theme switching (system / light / dark) + 6 color presets (Graphite, HyperOS Orange, Mint, Sunset, Ocean, Lavender) + Material You dynamic color
- Settings: refresh interval, low-quota threshold, network timeouts, User-Agent, route-warning toggle
- Miuix bottom navigation (总览 / 服务 / 设置) + Secondary pages (preset picker, editor)
- GitHub Actions CI: debug / release / bundle artifacts with ABI splits + universal APK
- release-please automation for GitHub Releases
