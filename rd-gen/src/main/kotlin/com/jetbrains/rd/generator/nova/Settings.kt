@file:Suppress("UNCHECKED_CAST")

package com.jetbrains.rd.generator.nova


//todo until https://youtrack.jetbrains.com/issue/KT-15745 is fixed
@Suppress("unused")
interface ISetting<out T : Any, out S: SettingsHolder>


abstract class SettingWithDefault<out T : Any, out S: SettingsHolder>(val default : T) : ISetting<T, S>

open class SettingsHolder {
    internal val settings = mutableMapOf<ISetting<*, *>, Any>()
}

internal var settingCtx : IGenerator? = null
internal val genInstanceKeys = mutableMapOf<Pair<IGenerator, ISetting<*,*>>, ISetting<*,*>>()

fun <T:Any, S:SettingsHolder> ISetting<T, S>.forGenerator(generator: IGenerator) : ISetting<T, S> =
    genInstanceKeys.getOrPut(generator to this) { object : ISetting<T,S> {} } as ISetting<T, S>


fun <T: Any, S : SettingsHolder> S.setting(key: ISetting<T, S>, value: T) = apply { settings[key] = value }
fun <T: Any, S : SettingsHolder> S.setting(key: SettingWithDefault<T, S>, value: T = key.default) = setting(key as ISetting<T, S>, value)

fun <T: Any, S : SettingsHolder>  S.getSetting(key: ISetting<T, S>) : T? {
    val specializedKey = settingCtx?.let { key.forGenerator(it) }

    return if (this is Declaration) {
        specializedKey?.let { this.getSettingInHierarchy(specializedKey)}
            ?:this.getSettingInHierarchy(key)
    }
    else {
        specializedKey?.let { settings[specializedKey] as T? }
            ?: settings[key] as T?
    }
}



fun <S : SettingsHolder> S.setting(key: ISetting<Unit, S>) = setting(key, Unit)
fun <S : SettingsHolder> S.hasSetting(key: ISetting<Unit, S>) : Boolean = getSetting(key) != null
