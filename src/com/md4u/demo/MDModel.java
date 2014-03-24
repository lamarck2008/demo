package com.md4u.demo;

import java.util.ArrayList;
import java.util.Date;

import org.json.JSONException;

import com.md4u.demo.MDJSON.MDJSONArray;
import com.md4u.demo.MDJSON.MDJSONObject;

public class MDModel {
    // model of HTTP response
    public static class Response extends MDJSONObject {
        public final int code;

        private Response(String content) throws JSONException {
            super(content);
            code = getInt("code");
        }

        public static Response create(String content) {
            try {
                return new Response(content);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }
    }

    // model of session
    public static class Session {
        public final String token;
        public final String user;

        public Session(MDJSONObject jo, String user) {
            this.token = jo.getString("token");
            this.user = user;
        }

        public static String formatLoginRequest(
            String userName, String userPwd
        ) {
            MDJSONObject request = MDJSONObject.create();
            request.put("user", userName);
            request.put("password", userPwd);
            return request.toString();
        }

        public String formatBasicRequest() {
            MDJSONObject request = MDJSONObject.create();
            request.put("user", user);
            return request.toString();
        }
    }

    // model of image
    public static class Image {
        public final String pid;

        public Image(String pid) {
            this.pid = pid;
        }

        public Image(MDJSONObject jo) {
            pid = jo.getString("pid");
        }

        public String formatRequest() {
            MDJSONObject request = MDJSONObject.create();
            request.put("pid", pid);
            return request.toString();
        }
    }

    // model of post
    public static class Post {
        public static enum State {
            NEW,
            VOTING,
            CANCELED,
            EXPIRED,
            SHARED,
            REMOVED;
        };

        public static enum Filter {
            ALL_VOTING,
            NEARBY_VOTING,
            FANS_VOTING,
            MY_ALL,
            ALL_SHARED,
            FANS_SHARED,
            MY_SHARED,
            RECOMMENDED_SHARED;
        };

        private State state;
        public final String id;
        public final Date dateTime;
        public final String senderId;
        public final Image senderPhoto;
        public final String description;
        public final int hour;
        public final int minute;
        public final int second;
        public final String group;
        public final int bonus;
        public int likeCount; 
        public final ArrayList<Image> images;

        public Post(
            String description,
            int hour, int minute, int second,
            String group, int bonus, 
            ArrayList<Image> images
        ) {
            this.state = State.NEW;
            this.id = null;
            this.dateTime = null;
            this.senderId = "";
            this.senderPhoto = null;
            this.description = description;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
            this.group = group;
            this.bonus = bonus;
            this.likeCount = 0;
            this.images = images;
        }

        public Post(MDJSONObject jo) {
            this.state = State.valueOf(jo.getString("state"));
            if (this.state != State.NEW) {
                this.id = jo.getString("_id");
                this.dateTime = MDDateFormat.parseServer(jo.getString("datetime"));
                this.senderId = jo.getJSONObject("sender").getString("user");
                this.senderPhoto = new Image(jo.getJSONObject("sender"));
            } else {
                this.id = null;
                this.dateTime = null;
                this.senderId = "";
                this.senderPhoto = null;
            }
            this.description = jo.getString("description");
            this.hour = jo.getInt("hour");
            this.minute = jo.getInt("minute");
            this.second = jo.getInt("second");
            this.group = jo.getString("group");
            this.bonus = jo.getInt("bonus");
            this.likeCount = jo.getInt("likeCount");

            MDJSONArray ja = jo.getJSONArray("pics");
            ArrayList<Image> images = new ArrayList<Image>();
            for(int i = 0; i < ja.length(); ++i) {
                images.add(new Image(ja.getJSONObject(i)));
            }
            this.images = images;
        }

        public String formatPublishRequest(Session session) {
            if (state != State.NEW) {
                return "";
            }
            // add image information
            MDJSONArray pics = MDJSONArray.create();
            for (int i = 0; i < images.size(); ++i) {
                MDJSONObject item = MDJSONObject.create();
                item.put("pid", images.get(i).pid);
                pics.put(item);
            }
            // add other information
            MDJSONObject request = MDJSONObject.create();
            request.put("user", session.user);
            request.put("description", description);
            request.put("hour", hour);
            request.put("minute", minute);
            request.put("second", second);
            request.put("group", group);
            request.put("bonus", bonus);
            request.put("pics", pics);
            return request.toString();
        }

        // pull posts with filter
        public static String formatPullRequest(
            Session session, Post post, Filter filter
        ) {
            if (post != null && post.state == State.NEW) {
                return "";
            }
            MDJSONObject request = MDJSONObject.create();
            request.put("user", session.user);
            request.put("token", session.token);
            request.put("decision_id", (post == null) ? "": post.id);
            request.put("filter", filter.name());
            return request.toString();
        }

        public String formatVoteRequest(
            Session session, ArrayList<Image> selectedImages
        ) {
            if (state != State.VOTING) {
                return "";
            }
            MDJSONArray vote = MDJSONArray.create();
            for (int i = 0; i < selectedImages.size(); ++i) {
                vote.put(selectedImages.get(i).pid);
            }
            MDJSONObject request = MDJSONObject.create();
            request.put("user", session.user);
            request.put("decision_id", id);
            request.put("vote", vote);
            return request.toString();
        }
        
        public String formatLikeRequest(
                Session session
            ) {
                MDJSONObject request = MDJSONObject.create();
                request.put("user", session.user);
                request.put("decision_id", id);
                return request.toString();
            }
        
        public String formatResultRequest(
                Session session
            ) {
                MDJSONObject request = MDJSONObject.create();
                request.put("user", session.user);
                request.put("decision_id", id);
                return request.toString();
            }

        public MDJSONObject toJSONObject() {
            MDJSONObject jo = MDJSONObject.create();
            jo.put("state", state.name());
            if (state != State.NEW) {
                jo.put("_id", id);
                jo.put("datetime", MDDateFormat.formatServer(dateTime));

                MDJSONObject sender = MDJSONObject.create();
                sender.put("user", senderId);
                sender.put("pid", senderPhoto.pid);
                jo.put("sender", sender);
            }
            jo.put("description", description);
            jo.put("hour", hour);
            jo.put("minute", minute);
            jo.put("second", second);
            jo.put("group", group);
            jo.put("bonus", bonus);

            MDJSONArray pics = MDJSONArray.create();
            for (int i = 0; i < images.size(); ++i) {
                MDJSONObject item = MDJSONObject.create();
                item.put("pid", images.get(i).pid);
                pics.put(item);
            }
            jo.put("pics", pics);
            return jo;
        }

        public static ArrayList<Post> toArrayList(MDJSONObject jo) {
            MDJSONArray ja = jo.getJSONArray("decisions");
            ArrayList<Post> postList = new ArrayList<Post>();
            for (int i = 0; i < ja.length(); ++i) {
                postList.add(new Post(ja.getJSONObject(i)));
            }
            return postList;
        }
    }

    // model of bonus
    public static class Bonus {
        public final int total;

        public Bonus(MDJSONObject jo) {
            total = jo.getInt("total_bonus");
        }

        public MDJSONObject toJSONObject() {
            MDJSONObject jo = MDJSONObject.create();
            jo.put("total_bonus", total);
            return jo;
        }

        public static class Record {
            public final int bonus;
            public final Date dateTime;
            public final String forUser;
            public final String type;

            public Record(MDJSONObject jo) {
                bonus = jo.getInt("bonus");
                dateTime = MDDateFormat.parseServer(jo.getString("datetime"));
                forUser = jo.getString("for_user");
                type = jo.getString("type");
            }

            public MDJSONObject toJSONObject() {
                MDJSONObject jo = MDJSONObject.create();
                jo.put("bonus", bonus);
                jo.put("datetime", MDDateFormat.formatServer(dateTime));
                jo.put("for_user", forUser);
                jo.put("type", type);
                return jo;
            }

            public static ArrayList<Record> toArrayList(MDJSONObject jo) {
                MDJSONArray ja = jo.getJSONArray("detail");
                ArrayList<Record> records = new ArrayList<Record>();
                for (int i = 0; i < ja.length(); ++i) {
                    records.add(new Record(ja.getJSONObject(i)));
                }
                return records;
            }
        }
    }

    // model of fan
    public static class Fan {
        public final String user;
        public final Image photo;

        public Fan(MDJSONObject jo) {
            user = jo.getString("user");
            photo = new Image(jo);
        }

        public MDJSONObject toJSONObject() {
            MDJSONObject jo = MDJSONObject.create();
            jo.put("user", user);
            jo.put("pid", photo.pid);
            return jo;
        }

        public static ArrayList<Fan> toArrayList(MDJSONObject jo) {
            MDJSONArray ja = jo.getJSONArray("friends");
            ArrayList<Fan> fansList = new ArrayList<Fan>();
            for (int i = 0; i < ja.length(); ++i) {
                fansList.add(new Fan(ja.getJSONObject(i)));
            }
            return fansList;
        }
    }
}
