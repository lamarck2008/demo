package com.md4u.demo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import com.md4u.demo.MDModel.Image;
import com.md4u.demo.MDModel.Post;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.Toast;

public class PostActivity extends Activity {
    private static final String PATH =
        Environment.getExternalStorageDirectory() + "/DCIM";

    // variables used to save post information temporarily
    private int selectedSrc;
    private int selectedImg;
    private String photoName;

    // views in post page
    private int btnImageSize;
    private String[] imgPath;
    private ImageButton[] btnImage;
    private EditText description;
    private EditText hour;
    private EditText minute;
    private EditText second;
    private Spinner group;
    private EditText bonus;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_post);
        getWindow().setFeatureInt(
            Window.FEATURE_CUSTOM_TITLE,
            R.layout.title_post
        );

        // initialize views
        Resources res = getResources();
        int margin = res.getDimensionPixelSize(R.dimen.post_image_margin_h);
        Point outSize = new Point();
        // TODO should avoid calling deprecated functions
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
            outSize.x = getWindowManager().getDefaultDisplay().getWidth();
        // } else {
        //     getWindowManager().getDefaultDisplay().getSize(outSize);
        // }
        btnImageSize = (outSize.x - 3 * margin) / 2;
        imgPath = new String[] { null, null };
        btnImage = new ImageButton[] {
            (ImageButton)findViewById(R.id.post_image_0),
            (ImageButton)findViewById(R.id.post_image_1)
        };
        description = (EditText)findViewById(R.id.post_msg);
        hour = (EditText)findViewById(R.id.post_hour);
        minute = (EditText)findViewById(R.id.post_minute);
        second = (EditText)findViewById(R.id.post_second);
        group = (Spinner)findViewById(R.id.post_group);
        bonus = (EditText)findViewById(R.id.post_bonus);

        // set adapter for group spinner
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<String>(
            this, R.layout.spinner_item_post_group,
            res.getStringArray(R.array.post_groups)
        );
        groupAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        group.setAdapter(groupAdapter);

        // set click listener for image buttons
        for (int i = 0; i < btnImage.length; ++i) {
            ViewGroup.LayoutParams params = btnImage[i].getLayoutParams();
            params.width = btnImageSize;
            params.height = btnImageSize;
            setImageButtonClickListener(btnImage[i]);
        }

        // set click listener for post button
        setPostButtonClickListener(
            (Button)findViewById(R.id.title_post_send)
        );
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                switch (selectedSrc) {
                    case 0: {
                        imgPath[selectedImg] = PATH + "/" + photoName;
                    } break;
                    case 1: {
                        Cursor cursor = getContentResolver().query(
                            data.getData(), null, null, null, null
                        );
                        cursor.moveToFirst();
                        imgPath[selectedImg] = cursor.getString(1);
                        cursor.close();
                    } break;
                }
                // load image to view
                MDImageLoader.loadLocalImage(
                    imgPath[selectedImg], btnImage[selectedImg],
                    btnImageSize, btnImageSize, null
                );
            }
        }
    }

    // set image button click listener
    private void setImageButtonClickListener(ImageButton button) {
        final OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.post_select_camera: {
                        selectedSrc = 0;
                        getPhotoFromCamera();
                    } break;
                    case R.id.post_select_album: {
                        selectedSrc = 1;
                        getPhotoFromAlbum();
                    } break;
                }
            }
        };

        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedImg = Arrays.asList(btnImage).indexOf(view);
                View root = findViewById(android.R.id.content).getRootView();
                View popupView = (View)getLayoutInflater().inflate(
                    R.layout.popup_post_select, null
                );
                SelectPopup select = new SelectPopup(
                    popupView, root.getWidth(), root.getHeight(), listener
                );
                select.showAtLocation(root, Gravity.BOTTOM, 0, 0);
            }
        });
    }

    // set post button click listener
    private void setPostButtonClickListener(Button button) {
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // check if any image is selected
                boolean flag = false;
                for (int i = 0; i < btnImage.length && !flag; ++i) {
                    flag |= (imgPath[i] != null);
                }
                if (!flag) {
                    // none image is selected
                    Toast.makeText(
                        getApplicationContext(),
                        R.string.post_error_none_image,
                        Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
                // show loading message
                View root = findViewById(android.R.id.content).getRootView();
                View popupView = (View)getLayoutInflater().inflate(
                    R.layout.popup_post_loading, null
                );
                loading = new PopupWindow(
                    popupView, root.getWidth(), root.getHeight(), true
                );
                loading.showAtLocation(root, Gravity.BOTTOM, 0, 0);
                // start a new thread for post operation
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        publishPost();
                    }
                };
                t.start();
            }
        });
    }

    // popup window class for image source selection
    private static class SelectPopup extends PopupWindow {
        private OnClickListener onClickListener;
        private Button camera;
        private Button album;

        public SelectPopup(
            View contentView,
            int width, int height,
            OnClickListener baseListener
        ) {
            super(contentView, width, height);

            // initialize
            onClickListener = baseListener;
            camera = (Button)contentView.findViewById(R.id.post_select_camera);
            album = (Button)contentView.findViewById(R.id.post_select_album);

            // set click listener
            camera.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    onClickListener.onClick(view);
                    dismiss();
                }
            });
            album.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    onClickListener.onClick(view);
                    dismiss();
                }
            });

            // set touch listener
            setFocusable(true);
            getContentView().setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    dismiss();
                    return true;
                }
            });
        }
    }

    // two ways to get an image
    private void getPhotoFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoName = "Decision" + SystemClock.currentThreadTimeMillis() + ".jpg";
        intent.putExtra(
            MediaStore.EXTRA_OUTPUT,
            Uri.fromFile(new File(PATH, photoName))
        );
        startActivityForResult(intent, 1);
    }

    private void getPhotoFromAlbum() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    // publish post data to server
    private void publishPost() {
        Message msg;
        // upload photo
        ArrayList<Image> images = new ArrayList<Image>();
        for (int i = 0; i < btnImage.length; ++i) {
            if (imgPath[i] == null) {
                continue;
            }
            Bitmap bitmap = ((BitmapDrawable)btnImage[i].getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] b = baos.toByteArray();
            Image postImage = MDService.MDServiceInstance.uploadImageData(b);
            if (postImage.pid != null) {
                images.add(postImage);
            } else {
                msg = new Message();
                msg.obj = PostActivity.this;
                msg.arg1 = 1;
                postHandler.sendMessage(msg);
                return;
            }
        }

        // publish post
        int code = MDService.MDServiceInstance.publishPost(new Post(
            description.getText().toString(),
            Integer.parseInt(hour.getText().toString()),
            Integer.parseInt(minute.getText().toString()),
            Integer.parseInt(second.getText().toString()),
            group.getSelectedItem().toString(),
            Integer.parseInt(bonus.getText().toString()),
            images
        ));

        msg = new Message();
        msg.obj = PostActivity.this;
        msg.arg1 = code;
        postHandler.sendMessage(msg);
    }

    // popup window shown during the post operation
    private PopupWindow loading;

    // message handler used during the post operation
    private static Handler postHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            PostActivity activity = (PostActivity)msg.obj;
            Toast.makeText(
                activity.getApplicationContext(),
                (msg.arg1 == 0) ? R.string.post_success : R.string.post_failed,
                Toast.LENGTH_SHORT
            ).show();
            activity.loading.dismiss();
            if (msg.arg1 == 0) {
                activity.finish();
            }
        }
    };
}
