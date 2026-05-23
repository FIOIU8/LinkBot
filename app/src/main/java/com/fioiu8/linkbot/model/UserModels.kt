package com.fioiu8.linkbot.model

data class BalanceResponse(
    val is_available: Boolean = false,
    val balance_infos: List<BalanceInfo> = emptyList()
)

data class BalanceInfo(
    val currency: String = "",
    val total_balance: String = "",
    val granted_balance: String = "",
    val topped_up_balance: String = ""
)