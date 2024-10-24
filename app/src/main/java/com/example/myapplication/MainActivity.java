package com.example.phq9app;

import android.os.Bundle;
import android.util.Log;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ListenBuilder;
import com.aldebaran.qi.sdk.builder.PhraseSetBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.Listen;
import com.aldebaran.qi.sdk.object.conversation.ListenResult;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.PhraseSet;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {

    private static final String TAG = "MainActivity";
    private QiContext qiContext = null;

    private final String[] phq9Questions = {
            "Did you have,  Little interest or pleasure in doing things?",
            "Feeling down, depressed, or hopeless?",
            "Trouble falling or staying asleep,  or sleeping too much?",
            "Feeling tired or having little energy?",
            "Poor appetite or overeating?",
            "Feeling bad about yourself,  or that you're a failure or have let your family down?",
            "Trouble concentrating on things, like reading or watching TV?",
            "Moving or speaking slowly, or being fidgety or restless?",
            "Thoughts of being better off dead or hurting yourself?"
    };

    private int currentQuestionIndex = 0;
    private int totalScore = 0;

    private Map<String, Integer> answerScoreMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        QiSDK.register(this, this);

        // Define the score mapping for each answer (store keys in lowercase)
        answerScoreMap = new HashMap<>();
        answerScoreMap.put("not at all", 0);
        answerScoreMap.put("several days", 1);
        answerScoreMap.put("more than half the days", 2);
        answerScoreMap.put("nearly every day", 3);
    }
    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        startIntroduction();
    }

    @Override
    public void onRobotFocusLost() {
        qiContext = null;
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        Log.e(TAG, "Robot focus refused: " + reason);
    }

    private void startIntroduction() {
        if (qiContext != null) {
            // Say the introduction message
            SayBuilder.with(qiContext)
                    .withText("Hello, I am Pepper, and we will conduct a happiness test. I will ask you questions, and you can answer verbally. Let's start. " +
                            "Please after hearing the questions answer with either with not at all, several days, more than half the days, or nearly every day.")
                    .buildAsync()
                    .andThenConsume(say -> say.async().run().thenConsume(future -> {
                        if (future.isDone()) {
                            askNextQuestion();
                        }
                    }));
        }
    }

    private void askNextQuestion() {
        if (currentQuestionIndex < phq9Questions.length) {
            SayBuilder.with(qiContext)
                    .withText(phq9Questions[currentQuestionIndex])
                    .buildAsync()
                    .andThenConsume(say -> say.async().run().thenConsume(future -> {
                        if (future.isDone()) {
                            listenForAnswer();
                        }
                    }));
        } else {
            calculateResult();
        }
    }

    private void listenForAnswer() {
        if (qiContext != null) {
            // Define possible answers
            PhraseSet notAtAll = PhraseSetBuilder.with(qiContext)
                    .withPhrases(new Phrase("Not at all")).build();
            PhraseSet severalDays = PhraseSetBuilder.with(qiContext)
                    .withPhrases(new Phrase("Several days")).build();
            PhraseSet moreThanHalfDays = PhraseSetBuilder.with(qiContext)
                    .withPhrases(new Phrase("More than half the days")).build();
            PhraseSet nearlyEveryDay = PhraseSetBuilder.with(qiContext)
                    .withPhrases(new Phrase("Nearly every day")).build();

            // Create Listen action
            Listen listen = ListenBuilder.with(qiContext)
                    .withPhraseSets(notAtAll, severalDays, moreThanHalfDays, nearlyEveryDay)
                    .build();

            // Run the Listen action
            listen.async().run().thenConsume(listenFuture -> {
                if (listenFuture.isSuccess()) {
                    ListenResult listenResult = listenFuture.get();
                    PhraseSet matchedPhraseSet = listenResult.getMatchedPhraseSet();
                    String matchedText = matchedPhraseSet.getPhrases().get(0).getText();

                    // Normalize and trim the recognized text
                    matchedText = matchedText.toLowerCase().trim();

                    // Log the recognized text for debugging
                    Log.d(TAG, "Recognized (trimmed): '" + matchedText + "'");

                    // Get the corresponding score from the map
                    Integer score = answerScoreMap.get(matchedText); // Use Integer here

                    // Log the score for debugging
                    Log.d(TAG, "Score for '" + matchedText + "': " + score);

                    if (score != null) { // Change from score != -1 to score != null
                        handleAnswer(score);
                    } else {
                        Log.e(TAG, "Unrecognized answer: '" + matchedText + "'");
                        SayBuilder.with(qiContext)
                                .withText("Sorry, I didn't catch that. Please repeat your answer.")
                                .buildAsync()
                                .andThenConsume(say -> say.async().run().thenConsume(future -> listenForAnswer()));
                    }
                } else {
                    Log.e(TAG, "Listening failed", listenFuture.getError());
                }
            });
        }
    }


    private void handleAnswer(int score) {
        totalScore += score;
        currentQuestionIndex++;

        if (currentQuestionIndex < phq9Questions.length) {
            askNextQuestion();
        } else {
            calculateResult();
        }
    }

    private void calculateResult() {
        String resultMessage = "Your total score is " + totalScore + " out of 27. Would you like to take the test again? Please say yes or no.";

        // Say the result message and ask if the user wants to retake the test
        SayBuilder.with(qiContext)
                .withText(resultMessage)
                .buildAsync()
                .andThenConsume(say -> say.async().run().thenConsume(future -> {
                    if (future.isDone()) {
                        listenForRetakeChoice();
                    }
                }));
    }

    private void listenForRetakeChoice() {
        if (qiContext != null) {
            // Define possible answers for retake
            PhraseSet yes = PhraseSetBuilder.with(qiContext)
                    .withPhrases(new Phrase("Yes")).build();
            PhraseSet no = PhraseSetBuilder.with(qiContext)
                    .withPhrases(new Phrase("No")).build();

            // Create Listen action
            Listen listen = ListenBuilder.with(qiContext)
                    .withPhraseSets(yes, no)
                    .build();

            // Run the Listen action
            listen.async().run().thenConsume(listenFuture -> {
                if (listenFuture.isSuccess()) {
                    ListenResult listenResult = listenFuture.get();
                    PhraseSet matchedPhraseSet = listenResult.getMatchedPhraseSet();
                    String matchedText = matchedPhraseSet.getPhrases().get(0).getText();

                    // If the user wants to retake the test, restart it
                    if (matchedText.equalsIgnoreCase("Yes")) {
                        resetTest();
                    } else if (matchedText.equalsIgnoreCase("No")) {
                        endSession();
                    }
                } else {
                    Log.e(TAG, "Listening failed", listenFuture.getError());
                }
            });
        }
    }

    private void resetTest() {
        currentQuestionIndex = 0;
        totalScore = 0;
        askNextQuestion();
    }

    private void endSession() {
        SayBuilder.with(qiContext)
                .withText("Thank you for taking the test. Have a great day!")
                .buildAsync()
                .andThenConsume(say -> say.async().run());
    }
    }