package com.md4u.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.md4u.demo.MDModel.Image;
import com.md4u.demo.MDModel.Post;
import com.md4u.demo.MDModel.Post.Filter;
import com.md4u.demo.MDModel.Response;
import com.md4u.demo.MDModel.Session;

import android.util.Base64;

public class MDService {
    private final String url = "http://115.28.19.49:8888/";
    private Session session;

    public static MDService MDServiceInstance = new MDService();

    private MDService() {
    }

    public interface MDServiceHandler {
        public void onSuccess(Response response);
    }

    // asynchronous HTTP post
    private void post(
        String path, String request, boolean isJSON,
        final MDServiceHandler handler
    ) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(
            null, url + path + "/",
            toStringEntity(request.toString()),
            isJSON ? "application/json" : "text/plain",
            new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(String content) {
                    handler.onSuccess(Response.create(content));
                }
            }
        );
    }

    // synchronous HTTP post
    private InputStream post(
        String path, String request, boolean isJSON
    ) {
        try {
            HttpPost post = new HttpPost();
            post.setURI(new URI(url + path + "/"));
            post.setEntity(toStringEntity(request));
            if (isJSON) {
                post.setHeader("Content-type", "application/json");
            }
            HttpClient client = new DefaultHttpClient();
            return client.execute(post).getEntity().getContent();
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    // help function to convert string to string entity
    private static StringEntity toStringEntity(String request) {
        try {
            return new StringEntity(request, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    // help function to convert HTTP response to JSON response
    private static Response toResponse(InputStream response) {
        try {
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(response)
            );
            String s = reader.readLine();
            while (s != null) {
                builder.append(s);
                s = reader.readLine();
            }
            return Response.create(builder.toString());
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    // help function to convert HTTP response to byte array
    private static byte[] toByteArray(InputStream response) {
        try {
            return IOUtils.toByteArray(response);
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    // network service is provided for each activity as follows

    public void login(
        final String userName, final String userPwd,
        final MDServiceHandler handler
    ) {
        post("login", Session.formatLoginRequest(userName, userPwd), true,
            new MDServiceHandler() {
                @Override
                public void onSuccess(Response response) {
                    if (response.code == 0) {
                        session = new Session(response, userName);
                    }
                    handler.onSuccess(response);
                }
            }
        );
    }

    public int publishPost(Post postContent) {
        return toResponse(post(
            "publish", postContent.formatPublishRequest(session), true
        )).code;
    }

    public void pullPost(
        Post postContent, Filter filter,
        final MDServiceHandler handler
    ) {
        post("pull", Post.formatPullRequest(
            session, postContent, filter
        ), true, handler);
    }

    public void loadBonusHistory(final MDServiceHandler handler) {
        post("bonus", session.formatBasicRequest(), true, handler);
    }

    public void loadFansList(final MDServiceHandler handler) {
        post("friendslist", session.formatBasicRequest(), true, handler);
    }

    public Image uploadImageData(byte[] data) {
        String request = Base64.encodeToString(data, Base64.DEFAULT);
        InputStream response = post("picpush", request, false);
        return new Image(toResponse(response));
    }

    public byte[] loadImageData(Image image) {
        return toByteArray(post("picpull", image.formatRequest(), true));
    }

    public void sendVote(
        Post postContent, ArrayList<Image> selectedImages,
        final MDServiceHandler handler
    ) {
        post("vote", postContent.formatVoteRequest(
            session, selectedImages
        ), true, handler);
    }
    
    public void sendLike(
            Post postContent,
            final MDServiceHandler handler
        ) {
    		post("like", postContent.formatLikeRequest(
                session
            ), true,handler);
        }
    
    public void sendResult(
            Post postContent,
            final MDServiceHandler handler
        ) {
    		post("result", postContent.formatResultRequest(
                session
            ), true,handler);
        }
}
