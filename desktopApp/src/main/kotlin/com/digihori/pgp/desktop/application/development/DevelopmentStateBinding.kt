package com.digihori.pgp.desktop.application.development

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import java.util.concurrent.atomic.AtomicReference

/** Read Compose state during composition so changes invalidate this binding, then publish after commit. */
@Composable
internal fun PublishDevelopmentReadState(
    target: AtomicReference<DevelopmentReadState?>,
    read: () -> DevelopmentReadState,
) {
    val state = read()
    SideEffect { target.set(state) }
}
