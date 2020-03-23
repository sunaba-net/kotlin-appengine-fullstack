using System;
using System.Collections.Generic;
using PeterO.Cbor;
using Piisu.CBOR;
namespace model {
class StringMessage: model.Message {
    public string value {set; get;} 

    public override string ToString() {
        return $"value:{value}";
    }
}

class StringMessageConverter: ICBORToFromConverter<StringMessage> {
    public static readonly StringMessageConverter Instance = new StringMessageConverter();
    public StringMessage FromCBORObject(CBORObject obj) => new StringMessage {
        value = PrimitiveConverter<string>.Instance.FromCBORObject(obj["value"])
    };
    public CBORObject ToCBORObject(StringMessage model) {
        CBORObject obj = CBORObject.NewMap();
        obj.Add("value", PrimitiveConverter<string>.Instance.ToCBORObject(model.value));
        return obj;
    }
}
}
