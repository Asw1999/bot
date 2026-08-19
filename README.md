# CoC Bot - Auto Farm

Android bot for Clash of Clans auto farming.

## Architecture
- **100% on-device** — Accessibility Service + MediaProjection
- **Hybrid design** — simple auto farm now, AI-swappable interfaces for future ML/LLM modules
- **Tech:** Kotlin, OpenCV 4.x, ML Kit OCR, Material3

## Features
- Auto search opponents
- Loot threshold filtering (Gold/Elixir/Dark Elixir)
- Configurable troop deployment (Line/Point pattern)
- Deploy side selection (Top/Bottom/Left/Right)
- Floating overlay with live status & log
- Anti-detection: random delays + tap offsets
- Session limits (max attacks, timeout)

## Setup
1. Build & install APK
2. Screenshot CoC UI buttons and crop them into `app/src/main/assets/templates/`:
   - `btn_attack.png` — Attack button on main village
   - `btn_find_match.png` — "Find a Match" button
   - `btn_next.png` — "Next" button when viewing opponent
   - `btn_return_home.png` — "Return Home" after battle
   - `btn_end_battle.png` — surrender/end battle button
   - `star_result.png` — star rating on battle results screen
3. Enable Accessibility Service for CoC Bot
4. Grant Screen Capture permission
5. Grant Overlay permission
6. Configure loot targets, deploy side/pattern
7. Launch overlay → open CoC → press START

## AI Extension Points
- `IGameStateDetector` — swap `TemplateDetector` for ML classifier
- `IAttackStrategy` — swap `SimpleDeployer` for LLM-powered attack planner
- `IStructureDetector` — future building/turret detection
- `IBaseAnalyzer` — future weak side + entry point analysis

## ⚠️ Disclaimer
This bot violates Supercell Terms of Service. Use at your own risk on alt accounts only.