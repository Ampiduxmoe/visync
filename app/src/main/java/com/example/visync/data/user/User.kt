package com.example.visync.data.user

data class User(
    val id: Long,
    val name: String,
    val discriminator: String,
)
