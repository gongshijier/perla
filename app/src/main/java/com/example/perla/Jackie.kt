package com.example.perla

import com.example.annotation.*

@Man(name = "jackie", age = 1, coutry = JackCountry::class)
interface Jackie : IFigher {

    @Body(weight = 200, height = 200)
    fun body()

    @GetCE(algorithm = Algorithm::class)
    fun ce(): Int

    @GetInstance
    fun instance(): IFigher
}

class Algorithm : IAlgorithm {
    override fun ce(figher: IFigher): Int {
        return -1
    }
}

class JackCountry : ICountry {
    override fun name(): String {
        return "China"
    }

}
