package com.mobile.shannon.coroutinestaskmodule

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


private const val TAG = "CoroutinesTask"

/**
 * 必须调用[run]才会执行
 * 默认在工作线程执行，结果在主线程响应。（可调接口runOn()/responseOn()修改执行与响应的线程）
 * @param mHeavyFunction  需要异步执行的方法
 */

class CoroutinesTask<T>(private val mHeavyFunction: (scope: CoroutineScope) -> T) {

    companion object {
        @JvmField
        val UI: CoroutineContext = Dispatchers.Main
        @JvmField
        val BG: CoroutineContext = Dispatchers.Default
    }

    private var job: Job? = null
    private var mOnError: ((error: Throwable?) -> Unit)? = null
    private var mOnResponse: ((response: T?) -> Unit)? = null

    private var mErrorContext: CoroutineContext = UI
    private var mResponseContext: CoroutineContext = UI
    private var mRunContext: CoroutineContext = BG

    //设置异常结果所执行的线程
    fun errorOn(contextType: CoroutineContext): CoroutinesTask<T> {
        mErrorContext = contextType
        return this
    }

    //设置响应结果所执行的线程
    fun responseOn(contextType: CoroutineContext): CoroutinesTask<T> {
        mResponseContext = contextType
        return this
    }

    //设置响应结果所执行在主线程
    fun responseOnUI(): CoroutinesTask<T> {
        mResponseContext = UI
        return this
    }

    //设置响应结果所执行在工作线程
    fun responseOnBG(): CoroutinesTask<T> {
        mResponseContext = BG
        return this
    }

    //设置任务执行所在线程
    fun on(contextType: CoroutineContext): CoroutinesTask<T> {
        mRunContext = contextType
        return this
    }

    //设置任务执行在工作线程
    fun onBG(): CoroutinesTask<T> {
        mRunContext = BG
        return this
    }

    //设置任务执行在主线程
    fun onUI(): CoroutinesTask<T> {
        mRunContext = UI
        return this
    }

    //设置异常执行逻辑
    fun onError(onError: (error: Throwable?) -> Unit): CoroutinesTask<T> {
        mOnError = onError
        return this
    }

    //设置响应执行逻辑
    fun onResponse(onResponse: (response: T?) -> Unit): CoroutinesTask<T> {
        mOnResponse = onResponse
        return this
    }

    //执行任务
    fun run(): CoroutinesTask<T>? {
        return runDelay(0)
    }

    //延时执行任务
    fun runDelay(time: Long): CoroutinesTask<T>? {
        try {
            job = GlobalScope.launch(mRunContext) {
                if (isActive) {
                    delay(time)
                    try {
                        val result: T = mHeavyFunction(this)
                        mOnResponse?.run { launch(mResponseContext) { invoke(result) } }
                    } catch (e: Exception) {
                        mOnError?.run { launch(mErrorContext) { invoke(e) } }
                                ?: launch(UI) { throw e }
                    }
                }
            }
        } catch (e: Exception) {
            mOnError?.run { GlobalScope.launch(mErrorContext) { invoke(e) } }
                    ?: GlobalScope.launch(UI) { throw e }
        }
        return this
    }

    //取消尚未执行的任务
    fun cancel() {
        job?.cancel()
    }
}
