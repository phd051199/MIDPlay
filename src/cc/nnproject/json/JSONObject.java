/*
Copyright (c) 2021-2025 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package cc.nnproject.json;

import java.util.Enumeration;
import java.util.Hashtable;

public class JSONObject extends AbstractJSON {

  protected Hashtable table;

  public JSONObject() {
    table = new Hashtable();
  }

  Object get(String name) throws JSONException {
    try {
      if (has(name)) {
        Object o = table.get(name);
        if (o instanceof String[]) {
          table.put(name, o = JSON.parseJSON(((String[]) o)[0]));
        }
        if (o == JSON.json_null) {
          return null;
        }
        return o;
      }
    } catch (JSONException e) {
      throw e;
    } catch (Exception e) {
    }
    throw new JSONException("No value for name: " + name);
  }

  Object get(String name, Object def) {
    if (!has(name)) {
      return def;
    }
    try {
      return get(name);
    } catch (Exception e) {
      return def;
    }
  }

  public String getString(String name) throws JSONException {
    Object o = get(name);
    if (o == null || o instanceof String) {
      return (String) o;
    }
    return String.valueOf(o);
  }

  public String getString(String name, String def) {
    try {
      Object o = get(name, def);
      if (o == null || o instanceof String) {
        return (String) o;
      }
      return String.valueOf(o);
    } catch (Exception e) {
      return def;
    }
  }

  public JSONObject getObject(String name) throws JSONException {
    try {
      return (JSONObject) get(name);
    } catch (ClassCastException e) {
      throw new JSONException("Not object: " + name);
    }
  }

  public JSONArray getArray(String name) throws JSONException {
    try {
      return (JSONArray) get(name);
    } catch (ClassCastException e) {
      throw new JSONException("Not array: " + name);
    }
  }

  public int getInt(String name) throws JSONException {
    return JSON.getInt(get(name));
  }

  public int getInt(String name, int def) {
    if (!has(name)) {
      return def;
    }
    try {
      return getInt(name);
    } catch (Exception e) {
      return def;
    }
  }

  long getLong(String name) throws JSONException {
    return JSON.getLong(get(name));
  }

  public long getLong(String name, long def) {
    if (!has(name)) {
      return def;
    }
    try {
      return getLong(name);
    } catch (Exception e) {
      return def;
    }
  }

  boolean getBoolean(String name) throws JSONException {
    Object o = get(name);
    if (o == JSON.TRUE) {
      return true;
    }
    if (o == JSON.FALSE) {
      return false;
    }
    if (o instanceof Boolean) {
      return ((Boolean) o).booleanValue();
    }
    if (o instanceof String) {
      String s = (String) o;
      s = s.toLowerCase();
      if (s.equals("true")) {
        return true;
      }
      if (s.equals("false")) {
        return false;
      }
    }
    throw new JSONException("Not boolean: " + o);
  }

  public boolean getBoolean(String name, boolean def) {
    if (!has(name)) {
      return def;
    }
    try {
      return getBoolean(name);
    } catch (Exception e) {
      return def;
    }
  }

  // hasKey
  public boolean has(String name) {
    return table.containsKey(name);
  }

  public void put(String name, AbstractJSON json) {
    table.put(name, json == null ? JSON.json_null : json);
  }

  public void put(String name, String s) {
    table.put(name, s == null ? JSON.json_null : s);
  }

  public void put(String name, int i) {
    table.put(name, new Integer(i));
  }

  public void put(String name, long l) {
    table.put(name, new Long(l));
  }

  public void put(String name, boolean b) {
    table.put(name, (b ? Boolean.TRUE : Boolean.FALSE));
  }

  public int size() {
    return table.size();
  }

  public String toString() {
    return build();
  }

  public String build() {
    if (size() == 0) {
      return "{}";
    }
    StringBuffer s = new StringBuffer(size() * 24 + 2);
    s.append("{");
    Enumeration keys = table.keys();
    while (true) {
      String k = (String) keys.nextElement();
      s.append("\"").append(k).append("\":");
      Object v = table.get(k);
      if (v instanceof AbstractJSON) {
        s.append(((AbstractJSON) v).build());
      } else if (v instanceof String) {
        s.append("\"").append(JSON.escape_utf8((String) v)).append("\"");
      } else if (v instanceof String[]) {
        s.append(((String[]) v)[0]);
      } else if (v == JSON.json_null) {
        s.append((String) null);
      } else {
        s.append(v);
      }
      if (!keys.hasMoreElements()) {
        break;
      }
      s.append(",");
    }
    s.append("}");
    return s.toString();
  }

  void _put(String name, Object obj) {
    table.put(name, obj);
  }
}
