package com.fioiu8.linkbot.model

data class SavedNote(
    val id: String,
    val title: String,
    val userMessage: String,
    val aiMessage: String,
    val createdAt: Long = System.currentTimeMillis()
)