//
// Created by jetbrains on 23.07.2018.
//

#include "RdReactiveBase.h"

rd::Logger RdReactiveBase::logReceived;
rd::Logger RdReactiveBase::logSend;

RdReactiveBase::RdReactiveBase(RdReactiveBase &&other) : RdBindableBase(std::move(other))/*, async(other.async)*/ {
    async = other.async;
}

RdReactiveBase &RdReactiveBase::operator=(RdReactiveBase &&other) {
    async = other.async;
    static_cast<RdBindableBase &>(*this) = std::move(other);
    return *this;
}

const IWire *const RdReactiveBase::get_wire() const {
    return get_protocol()->wire.get();
}

void RdReactiveBase::assert_threading() const {
    if (!async) {
        get_default_scheduler()->assert_thread();
    }
}

void RdReactiveBase::assert_bound() const {
    if (!is_bound()) {
        throw std::invalid_argument("Not bound");
    }
}

const Serializers &RdReactiveBase::get_serializers() const {
    return get_protocol()->serializers;
}

IScheduler *RdReactiveBase::get_default_scheduler() const {
    return get_protocol()->scheduler;
}

IScheduler *RdReactiveBase::get_wire_scheduler() const {
    return get_default_scheduler();
}


