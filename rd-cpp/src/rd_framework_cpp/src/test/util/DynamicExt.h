//
// Created by jetbrains on 01.10.2018.
//

#ifndef RD_CPP_DYNAMICEXT_H
#define RD_CPP_DYNAMICEXT_H


#include "RdExtBase.h"
#include "RdProperty.h"

class DynamicExt : public RdExtBase, public ISerializable {
public:
    RdProperty<std::wstring> bar{L""};
    std::wstring debugName;

    DynamicExt();

    DynamicExt(RdProperty<std::wstring> bar, std::wstring debugName);

    DynamicExt(std::wstring const &bar, std::wstring const &debugName);


    void init(Lifetime lifetime) const override;

    void identify(const IIdentities& identities, RdId const& id) const override;

    static DynamicExt read(SerializationCtx const &ctx, Buffer const &buffer);

    void write(SerializationCtx const &ctx, Buffer const &buffer) const override;

    static void create(IProtocol *protocol);
};


#endif //RD_CPP_DYNAMICEXT_H
