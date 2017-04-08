package com.simplemobiletools.draw.extensions

import android.content.Context
import com.simplemobiletools.draw.helpers.Config

val Context.config: Config get() = Config.newInstance(this)
