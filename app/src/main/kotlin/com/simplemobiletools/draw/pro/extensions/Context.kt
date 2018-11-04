package com.simplemobiletools.draw.pro.extensions

import android.content.Context
import com.simplemobiletools.draw.pro.helpers.Config

val Context.config: Config get() = Config.newInstance(applicationContext)
