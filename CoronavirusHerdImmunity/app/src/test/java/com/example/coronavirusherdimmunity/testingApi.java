package com.example.coronavirusherdimmunity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

import static org.junit.Assert.assertEquals;

public class testingApi {
        private static final String baseEndoint = "http://ec2-52-53-189-28.us-west-1.compute.amazonaws.com";

        private static final MediaType JSONContentType = MediaType.parse("application/json; charset=utf-8");

        public static String stringifyRequestBody(Request request) {
            if (request.body() != null) {
                try {
                    final Request copy = request.newBuilder().build();
                    final Buffer buffer = new Buffer();
                    copy.body().writeTo(buffer);
                    return buffer.readUtf8();
                } catch (final IOException e) {
                    System.out.println("Failed to stringify request body: " + e.getMessage());
                }
            }
            return "";
        }

        public static JSONObject registerDevice(String deviceId, String challenge){

            Map device = new HashMap();
            device.put("manufacturer", "Google");
            device.put("model", "Nexus4");

            Map os = new HashMap();
            os.put("name", "Android");
            os.put("version", "10");

            Map body = new HashMap();
            body.put("id", "a9a76dda2b-565e-4d4ge-8b45-5f40f6b0d307");
            body.put("os", os);
            body.put("device", device);
            body.put("challenge", "03AGdBq25v-JzE6chjyE86et7c8V_qt4UtvIApjFLGCSiM85T1OduNVH-jm6m8YXnywadb0rAZjwyB4-nFp_8jDAN2gVyZk2CaI1nkdinRpEDxI72BF1QuLs0AQXFn-zw30b7DPlKsJupyFP-z6FPQ8izl_znzLgeWb7FmoOMrES_O3B1YajSaiLN5jfwvPNjEMNV11jdazureOtTinLr-eHrPq40o0oGdWRmBioK0NetVbzx_aIghQuAksDJG37I44cn2YhqMuq266JrD1rOUS-2K9ysFYkBeSUgsMr3DuqMQHEdvVH5mWZokEwJp3pgdSjlLyfsOoITL");

            OkHttpClient client = new OkHttpClient();

            RequestBody rq = RequestBody.create(JSONContentType, JSONValue.toJSONString(body));
            Request request = new Request.Builder()
                    .url(baseEndoint + "/device/handshake")
                    .post(rq)
                    .build();
            System.out.println(stringifyRequestBody(request));
            try {
                Response response = client.newCall(request).execute();
                String strResponse = response.body().string();
                JSONObject obj = new JSONObject(strResponse);
                return obj;
            }catch(Exception e){
                System.out.println("EXCEPTION on registering device");
            }
            return null;
        }


    @Test
    public void addition_isCorrect() {
        System.out.println("were the fuck");
        registerDevice("crep", "blah");

        assertEquals(4, 2 + 2);
    }
}
