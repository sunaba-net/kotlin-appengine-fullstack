using System;
using System.Collections.Generic;
using PeterO.Cbor;
using UnityEngine;

namespace Piisu.CBOR
{
    public static class ConverterExtension
    {
        public static T FromBytes<T>(this ICBORToFromConverter<T> converter, byte[] bytes)
        {
            return converter.FromCBORObject(CBORObject.DecodeFromBytes(bytes));
        }

        public static byte[] ToBytes<T>(this ICBORConverter<T> converter, T obj)
        {
            return converter.ToCBORObject(obj).EncodeToBytes();
        }
    }

    class PrimitiveConverter<T> : ICBORToFromConverter<T>
    {
        public static readonly PrimitiveConverter<T> Instance = new PrimitiveConverter<T>();

        public CBORObject ToCBORObject(T obj)
        {
            return CBORObject.FromObject(obj);
        }

        public T FromCBORObject(CBORObject obj)
        {
            return obj.ToObject<T>();
        }
    }

    class ReferenceListConverter<T> : ICBORToFromConverter<List<T>>
    {
        private ICBORToFromConverter<T> itemConverter;

        public ReferenceListConverter(ICBORToFromConverter<T> itemConverter)
        {
            this.itemConverter = itemConverter;
        }

        public CBORObject ToCBORObject(List<T> obj)
        {
            var len = obj.Count;
            var array = CBORObject.NewArray();
            for (int i = 0; i < len; i++)
            {
                array.Add(itemConverter.ToCBORObject(obj[i]));
            }

            return array;
        }

        public List<T> FromCBORObject(CBORObject obj)
        {
            var len = obj.Count;
            var list = new List<T>(len);
            for (int i = 0; i < len; i++)
            {
                list[i] = itemConverter.FromCBORObject(obj[i]);
            }

            return list;
        }
    }

    class ReferenceArrayConverter<T> : ICBORToFromConverter<T[]>
    {
        private ICBORToFromConverter<T> itemConverter;

        public ReferenceArrayConverter(ICBORToFromConverter<T> itemConverter)
        {
            this.itemConverter = itemConverter;
        }

        public CBORObject ToCBORObject(T[] obj)
        {
            var len = obj.Length;
            var array = CBORObject.NewArray();
            for (int i = 0; i < len; i++)
            {
                array.Add(itemConverter.ToCBORObject(obj[i]));
            }

            return array;
        }

        public T[] FromCBORObject(CBORObject obj)
        {
            var len = obj.Count;
            var array = new T[len];
            for (int i = 0; i < len; i++)
            {
                array[i] = itemConverter.FromCBORObject(obj[i]);
            }

            return array;
        }
    }

    public class DateTimeConverter : ICBORToFromConverter<DateTime>
    {
        public static readonly DateTimeConverter Instance = new DateTimeConverter();

        public DateTime FromCBORObject(CBORObject obj)
        {
            long unixTime = obj.ToObject<long>();
            return new DateTime(unixTime);
        }

        public CBORObject ToCBORObject(DateTime obj)
        {
            long unixTime = (long) (obj - new DateTime(1970, 1, 1, 0, 0, 0)).TotalMilliseconds;
            return CBORObject.FromObject(unixTime);
        }
    }
}