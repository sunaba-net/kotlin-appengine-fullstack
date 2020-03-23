using System;
using System.Collections.Generic;
using PeterO.Cbor;
using Piisu.CBOR;
namespace model {
interface Message {
    

}

class MessageConverter: ICBORToFromConverter<Message> {
    public static readonly MessageConverter Instance = new MessageConverter();
    public Message FromCBORObject(CBORObject obj) {
        switch(obj[0].AsString()) {
        case "model.StringMessage":
            return model.StringMessageConverter.Instance.FromCBORObject(obj[1]);
        case "model.IntMessage":
            return model.IntMessageConverter.Instance.FromCBORObject(obj[1]);
        }
        return null;
    }
    public CBORObject ToCBORObject(Message model) {
        switch(model) {
        case model.StringMessage v:
            return CBORObject.NewArray().Add("model.StringMessage")
                .Add(StringMessageConverter.Instance.ToCBORObject(v));
        case model.IntMessage v:
            return CBORObject.NewArray().Add("model.IntMessage")
                .Add(IntMessageConverter.Instance.ToCBORObject(v));
        }
        return null;
    }
}
}
