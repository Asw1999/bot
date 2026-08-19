package com.cocbot.core

enum class GameState {
    MAIN_VILLAGE,
    SEARCH_OPPONENT,
    OPPONENT_BASE,
    IN_BATTLE,
    BATTLE_END,
    UNKNOWN
}

enum class BotState {
    IDLE,
    NAVIGATE_TO_ATTACK,
    SEARCHING,
    CHECK_LOOT,
    DEPLOY_TROOPS,
    BATTLING,
    BATTLE_END,
    RETURN_HOME,
    ERROR
}