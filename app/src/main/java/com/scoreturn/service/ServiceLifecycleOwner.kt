package com.scoreturn.service

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class ServiceLifecycleOwner : LifecycleOwner {

    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    fun start() {
        // LifecycleRegistry DEBE modificarse en el main thread
        Handler(Looper.getMainLooper()).post {
            registry.currentState = Lifecycle.State.CREATED
            registry.currentState = Lifecycle.State.STARTED
            registry.currentState = Lifecycle.State.RESUMED
        }
    }

    fun stop() {
        Handler(Looper.getMainLooper()).post {
            if (registry.currentState != Lifecycle.State.DESTROYED) {
                registry.currentState = Lifecycle.State.DESTROYED
            }
        }
    }
}