package com.jetbrains.rd.generator.nova


import com.jetbrains.rd.generator.nova.Member.Field
import com.jetbrains.rd.generator.nova.Member.Reactive.Signal
import com.jetbrains.rd.generator.nova.Member.Reactive.Stateful.*
import com.jetbrains.rd.generator.nova.Member.Reactive.Stateful.List
import com.jetbrains.rd.generator.nova.Member.Reactive.Stateful.Map
import com.jetbrains.rd.generator.nova.Member.Reactive.Stateful.Set
import com.jetbrains.rd.generator.nova.Member.Reactive.Task
import com.jetbrains.rd.util.PublicApi

fun Struct.field(name : String, type : IScalar) = append(Field(name, type))
fun Class.field(name : String, type : IType) = append(Field(name, type))
fun Ext.field(name : String, type : Aggregate) = append(Field(name, type))

fun BindableDeclaration.signal(name : String, valueType : IScalar) = append(Signal(name, valueType))
fun BindableDeclaration.source(name : String, valueType : IScalar) = append(Signal(name, valueType).write)
fun BindableDeclaration.sink(name : String, valueType : IScalar) = append(Signal(name, valueType).readonly)

@PublicApi
@Suppress("unused")
@Deprecated("", ReplaceWith("signal(name, void)"))
fun BindableDeclaration.voidSignal(name : String) = signal(name, PredefinedType.void)
@Deprecated("", ReplaceWith("source(name, void)"))
fun BindableDeclaration.voidSource(name : String) = source(name, PredefinedType.void)
@Deprecated("", ReplaceWith("sink(name, void)"))
fun BindableDeclaration.voidSink(name : String) = sink(name, PredefinedType.void)

fun BindableDeclaration.call(name : String, paramType : IScalar, resultType : IScalar) = append(Task(name, paramType, resultType).write)
fun BindableDeclaration.callback(name : String, paramType : IScalar, resultType : IScalar) = append(Task(name, paramType, resultType).readonly)


fun BindableDeclaration.property(name : String, valueType : IType) = append(Property(name, valueType))
fun BindableDeclaration.property(name: String, defaultValue: Boolean) = append(Property(name, PredefinedType.bool, defaultValue))
fun BindableDeclaration.property(name: String, defaultValue: Int) = append(Property(name, PredefinedType.int, defaultValue))
fun BindableDeclaration.property(name: String, defaultValue: Double) = append(Property(name, PredefinedType.double, defaultValue))
fun BindableDeclaration.property(name: String, defaultValue: String) = append(Property(name, PredefinedType.string, defaultValue))

fun BindableDeclaration.list(name : String, itemType : IType) = append(List(name, itemType))
fun BindableDeclaration.set(name : String, itemType : INonNullableScalar) = append(Set(name, itemType))
fun BindableDeclaration.map(name : String, keyType : INonNullableScalar, valueType: INonNullable) = append(Map(name, keyType, valueType))

//Following "fake" functions introduced to raise compile-time errors if you add reactive entities into structs.
//Suppose we have struct inside bindable declaration: Ext or Class. Then We must cheat Kotlin resolve
private const val ce_bindable  = "Can't be used inside scalars: structs, enums, etc."
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Struct.field(name : String, type : IBindable) : Nothing = error(ce_bindable)

@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.signal(name : String, valueType : IType) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.source(name : String, valueType : IType) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.sink(name : String, valueType : IType) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.voidSignal(name : String) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.voidSource(name : String) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.voidSink(name : String) : Nothing = error(ce_bindable)


@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.property(name : String, valueType : IType) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.list(name : String, itemType : IType) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.set(name : String, itemType : INonNullableScalar) : Nothing = error(ce_bindable)
@Suppress("unused", "UNUSED_PARAMETER") @Deprecated(ce_bindable, level = DeprecationLevel.ERROR) fun Declaration.map(name : String, keyType : INonNullableScalar, valueType: INonNullable) : Nothing = error(ce_bindable)


fun array(type: IBindable) = ArrayOfBindables(type)
fun array(type: IScalar)   = ArrayOfScalars(type)

fun immutableList(type: IBindable) = ImmutableListOfBindables(type)
fun immutableList(type: IScalar) = ImmutableListOfScalars(type)

//todo support immutableSets and immutableMaps for consistency

val INonNullableScalar.nullable : NullableScalar get() = NullableScalar(this)
val INonNullableBindable.nullable : NullableBindable get() = NullableBindable(this)

val INonNullableScalar.interned : InternedScalar get() = InternedScalar(this)

val Class.internRoot: Unit get() {
    isInternRoot = true
}
