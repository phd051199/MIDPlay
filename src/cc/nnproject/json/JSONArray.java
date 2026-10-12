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

public class JSONArray extends AbstractJSON {

  protected Object[] elements;
  protected int count;

  public JSONArray() {
    elements = new Object[10];
  }

  public JSONArray(int capacity) {
    elements = new Object[capacity < 1 ? 1 : capacity];
  }

  public Object get(int index) throws JSONException {
    if (index < 0 || index >= count) {
      throw new JSONException("Index out of bounds: " + index);
    }
    try {
      Object o = elements[index];
      if (o instanceof String[]) {
        o = elements[index] = JSON.parseJSON(((String[]) o)[0]);
      }
      if (o == JSON.json_null) {
        return null;
      }
      return o;
    } catch (Exception e) {
    }
    throw new JSONException("No value at " + index);
  }

  public JSONObject getObject(int index) throws JSONException {
    try {
      return (JSONObject) get(index);
    } catch (ClassCastException e) {
      throw new JSONException("Not object at " + index);
    }
  }

  public String getString(int index) throws JSONException {
    Object value = get(index);
    return value == null || value instanceof String ? (String) value : String.valueOf(value);
  }

  public JSONArray getArray(int index) throws JSONException {
    try {
      return (JSONArray) get(index);
    } catch (ClassCastException e) {
      throw new JSONException("Not array at " + index);
    }
  }

  public void put(int index, Object value) {
    if (index < 0 || index >= count) {
      throw new JSONException("Index out of bounds: " + index);
    }
    elements[index] = value == null ? JSON.json_null : value;
  }

  public void remove(int index) {
    if (index < 0 || index >= count) {
      throw new JSONException("Index out of bounds: " + index);
    }
    count--;
    System.arraycopy(elements, index + 1, elements, index, count - index);
    elements[count] = null;
  }

  public static JSONArray parseArray(String text) {
    return JSON.getArray(text);
  }

  public void add(AbstractJSON json) {
    if (json == this) {
      throw new JSONException();
    }
    addElement(json);
  }

  public void add(Object value) {
    if (value == this) {
      throw new JSONException();
    }
    addElement(value == null ? JSON.json_null : value);
  }

  public boolean has(Object value) {
    for (int i = 0; i < count; i++) {
      Object item = elements[i];
      if (item == value || (item != null && item.equals(value))) {
        return true;
      }
    }
    return false;
  }

  public String format(int indent) {
    return build();
  }

  public int size() {
    return count;
  }

  public String toString() {
    return build();
  }

  public String build() {
    int size = count;
    if (size == 0) {
      return "[]";
    }
    StringBuffer s = new StringBuffer(count * 24 + 2);
    s.append("[");
    int i = 0;
    while (i < size) {
      Object v = elements[i];
      if (v instanceof AbstractJSON) {
        s.append(((AbstractJSON) v).build());
      } else if (v instanceof String) {
        s.append("\"").append(JSON.escape_utf8((String) v)).append("\"");
      } else if (v instanceof String[]) {
        s.append(((String[]) v)[0]);
      } else if (v == JSON.json_null) {
        s.append((String) null);
      } else {
        s.append(String.valueOf(v));
      }
      i++;
      if (i < size) {
        s.append(",");
      }
    }
    s.append("]");
    return s.toString();
  }

  void addElement(Object object) {
    if (count == elements.length) {
      grow();
    }
    elements[count++] = object;
  }

  private void grow() {
    Object[] tmp = new Object[elements.length * 2];
    System.arraycopy(elements, 0, tmp, 0, count);
    elements = tmp;
  }
}
