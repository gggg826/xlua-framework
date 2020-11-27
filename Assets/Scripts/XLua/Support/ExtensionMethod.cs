using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public static class ExtensionMethod
{
    public static bool Remove(this Dictionary<string, UnityEngine.GameObject> dic, string key, out GameObject value)
    {
        if(dic == null || !dic.ContainsKey(key))
        {
            value = null;
            return false;
        }
        else
        {
            value = dic[key];
            dic.Remove(key);
            return true;
        }
    }

    public static bool TryAdd(this Dictionary<string, UnityEngine.GameObject> dic, string key, GameObject value)
    {
        if (dic == null || dic.ContainsKey(key))
        {
            return false;
        }
        else
        {
            dic.Add(key, value);
            return true;
        }
    }
}
