package com.designlife.justdo_provider.common

import com.designlife.justdo.common.data.network.ProviderWeatherService
import com.designlife.justdo.common.data.network.retrofit.ProviderRetrofitBuilder
import com.designlife.justdo_provider.domain.WeatherUpdateRepository

object ProviderServiceLocator {
    private var weatherUpdateRepository : WeatherUpdateRepository? = null

    public fun provideWeatherUpdateRepository() : WeatherUpdateRepository{
        return weatherUpdateRepository ?: createWeatherUpdateRepository()
    }

    fun createWeatherUpdateRepository(): WeatherUpdateRepository {
        val weatherService = ProviderRetrofitBuilder.networkBuilder().create(ProviderWeatherService::class.java)
        val updateRepository = WeatherUpdateRepository(
            weatherService = weatherService
        )
        return updateRepository
    }

}