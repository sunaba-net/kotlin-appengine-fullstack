using System;
using System.Collections.Generic;
using PeterO.Cbor;
using Piisu.CBOR;
namespace model {
class IntMessage: model.Message {
    public int value {set; get;} 

    public override string ToString() {
        return $"value:{value}";
    }
}

class IntMessageConverter: ICBORToFromConverter<IntMessage> {
    public static readonly IntMessageConverter Instance = new IntMessageConverter();
    public IntMessage FromCBORObject(CBORObject obj) => new IntMessage {
        value = PrimitiveConverter<int>.Instance.FromCBORObject(obj["value"])
    };
    public CBORObject ToCBORObject(IntMessage model) {
        CBORObject obj = CBORObject.NewMap();
        obj.Add("value", PrimitiveConverter<int>.Instance.ToCBORObject(model.value));
        return obj;
    }
}
}
