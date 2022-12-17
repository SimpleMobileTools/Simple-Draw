package com.simplemobiletools.draw.rec.extensions

import android.content.Context
import com.simplemobiletools.draw.rec.helpers.Config

val Context.config: Config get() = Config.newInstance(applicationContext)
