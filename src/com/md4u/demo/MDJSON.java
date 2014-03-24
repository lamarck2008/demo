package com.md4u.demo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MDJSON {
    public static class MDJSONObject extends JSONObject {
        public static MDJSONObject create() {
            try {
                return new MDJSONObject();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        public static MDJSONObject create(String content) {
            try {
                return new MDJSONObject(content);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        public static MDJSONObject create(JSONObject jo) {
            try {
                return new MDJSONObject(jo.toString());
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        public MDJSONObject() throws JSONException {
            super();
        }

        public MDJSONObject(String content) throws JSONException {
            super(content);
        }

        @Override
        public MDJSONObject put(String name, Object value) {
            try {
                super.put(name, value);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return this;
        }

        @Override
        public MDJSONObject put(String name, int value) {
            try {
                super.put(name, value);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return this;
        }

        @Override
        public MDJSONObject put(String name, boolean value) {
            try {
                super.put(name, value);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return this;
        }

        @Override
        public int getInt(String name) {
            try {
                return super.getInt(name);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public String getString(String name) {
            try {
                return super.getString(name);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean getBoolean(String name) {
            try {
                return super.getBoolean(name);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public MDJSONObject getJSONObject(String name) {
            try {
                return MDJSONObject.create(
                    super.getJSONObject(name)
                );
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public MDJSONArray getJSONArray(String name) {
            try {
                return MDJSONArray.create(
                    super.getJSONArray(name)
                );
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }
    }

    public static class MDJSONArray extends JSONArray {
        public static MDJSONArray create() {
            try {
                return new MDJSONArray();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        public static MDJSONArray create(JSONArray ja) {
            try {
                MDJSONArray copy = new MDJSONArray();
                for (int i = 0; i < ja.length(); ++i) {
                    copy.put(ja.getJSONObject(i));
                }
                return copy;
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        public MDJSONArray() throws JSONException {
            super();
        }

        @Override
        public MDJSONObject getJSONObject(int index) {
            try {
                return MDJSONObject.create(
                    super.getJSONObject(index)
                );
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }
    }
}
