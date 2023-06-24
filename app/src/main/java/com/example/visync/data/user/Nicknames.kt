package com.example.visync.data.user

fun generateNickname(): String {
    return adjectives.random() + nouns.random()
}