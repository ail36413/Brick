package com.ail.brick.net.http.di

import com.ail.brick.net.http.auth.TokenProvider
import com.ail.brick.net.http.auth.UnauthorizedHandler
import com.ail.brick.net.http.annotations.AppInterceptor
import com.ail.brick.net.http.annotations.INetLogger
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor

@Module
@InstallIn(SingletonComponent::class)
abstract class OptionalBindingsModule {

    // Optional extension points: app can provide any subset as needed.
    // Minimal required initialization remains only NetworkConfig.

    @BindsOptionalOf
    abstract fun optionalTokenProvider(): TokenProvider

    @BindsOptionalOf
    abstract fun optionalNetLogger(): INetLogger

    @BindsOptionalOf
    @AppInterceptor
    abstract fun optionalAppInterceptors(): Map<Int, Interceptor>

    @BindsOptionalOf
    abstract fun optionalUnauthorizedHandler(): UnauthorizedHandler
}
