package com.lezenford.telegram.chanelthreadbot.cache

import com.lezenford.telegram.chanelthreadbot.extensions.isSuspend
import com.lezenford.telegram.chanelthreadbot.extensions.setLast
import org.aopalliance.intercept.MethodInvocation
import org.springframework.cache.interceptor.CacheInterceptor
import org.springframework.cache.interceptor.CacheOperationInvoker
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resumeWithException

class CoroutineCacheInterceptor : CacheInterceptor() {

    override fun invoke(invocation: MethodInvocation): Any? =
        if (invocation.method.isSuspend) {
            keyGenerator
            invokeCoroutine(invocation)
        } else {
            super.invoke(invocation)
        }

    @Suppress("UNCHECKED_CAST")
    private fun invokeCoroutine(invocation: MethodInvocation): Any? =
        try {
            val args = invocation.arguments
            val target = invocation.`this`!!
            val method = invocation.method

            val cachingContinuation = CachingContinuation(args.last() as Continuation<Any?>) {
                execute({ it }, target, method, args.toList().dropLast(1).toTypedArray())
            }

            executeCoroutine(cachingContinuation, invocation, target, method, args)
        } catch (th: CacheOperationInvoker.ThrowableWrapper) {
            throw th.original
        }

    private fun executeCoroutine(
        cachingContinuation: CachingContinuation,
        invocation: MethodInvocation, target: Any, method: Method, args: Array<Any>
    ): Any? {

        val originalContinuation = args.last()
        args.setLast(cachingContinuation)

        return try {
            execute({
                try {
                    invocation.proceed()
                } catch (ex: Throwable) {
                    throw CacheOperationInvoker.ThrowableWrapper(ex)
                }
            }, target, method, args)
        } catch (e: CoroutineSuspendedException) {
            COROUTINE_SUSPENDED
        } finally {
            args.setLast(originalContinuation)
        }
    }

    override fun invokeOperation(invoker: CacheOperationInvoker): Any? {
        val result = super.invokeOperation(invoker)

        if (result === COROUTINE_SUSPENDED) {
            throw CoroutineSuspendedException()
        } else {
            return result
        }
    }

    private class CachingContinuation(
        private val delegate: Continuation<Any?>,
        private val onResume: (Any?) -> Unit
    ) : Continuation<Any?> {
        override val context = delegate.context

        override fun resumeWith(result: Result<Any?>) {
            if (result.isFailure) {
                delegate.resumeWith(result)
                return
            }
            var resumed = false

            try {
                onResume(result.getOrNull())
            } catch (ex: Throwable) {
                resumed = true
                delegate.resumeWithException(ex)
            }

            if (!resumed) {
                delegate.resumeWith(result)
            }
        }
    }

    private class CoroutineSuspendedException : RuntimeException()
}
