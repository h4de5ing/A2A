package com.android.host;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseChatActivity extends AppCompatActivity {
    TextView contentTextView;
    EditText input;

    public void onButtonClick() {
        final String inputString = input.getText().toString();
        if (inputString.length() == 0) return;
        sendString(inputString);
        printLineToUI("<![CDATA[host> ]]>" + inputString);
        input.setText("");
    }

    protected abstract void sendString(final String string);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        contentTextView = findViewById(R.id.content_text);
        input = findViewById(R.id.input_edittext);
        findViewById(R.id.send_button).setOnClickListener(v -> onButtonClick());
    }

    protected void printLineToUI(final String line) {
        runOnUiThread(() -> contentTextView.setText(contentTextView.getText() + "\n" + line));
    }

}
