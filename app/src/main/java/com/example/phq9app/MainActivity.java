package com.example.phq9app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Topic;

public class MainActivity extends RobotActivity implements com.aldebaran.qi.sdk.RobotLifecycleCallbacks {

    private static final String TAG = "MainActivity";
    private QiContext qiContext = null;
    private Chat chat;
    private QiChatbot qiChatbot;
    private Topic topic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register the QiSDK with the RobotLifecycleCallbacks implemented in MainActivity
        QiSDK.register(this, this);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        Log.d(TAG, "Robot focus gained.");

        // Load the topic from the asset file
        try {
            topic = TopicBuilder.with(qiContext)
                    .withResource(R.raw.dialog)
                    .build();

            // Create a new QiChatbot
            qiChatbot = QiChatbotBuilder.with(qiContext)
                    .withTopic(topic)
                    .build();

            // Create a new Chat action
            chat = ChatBuilder.with(qiContext)
                    .withChatbot(qiChatbot)
                    .build();

            // Run the chat action asynchronously
            chat.async().run().thenConsume(future -> {
                if (future.isSuccess()) {
                    Log.i(TAG, "Chat finished successfully");
                } else if (future.hasError()) {
                    Log.e(TAG, "Chat error: " + future.getError().getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing chat", e);
        }
    }

    @Override
    public void onRobotFocusLost() {

    }


    @Override
    public void onRobotFocusRefused(String reason) {
        Log.e(TAG, "Robot focus refused: " + reason);
    }
}