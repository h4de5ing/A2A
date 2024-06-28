package com.android.usbp2p.host;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.usbp2p.R;

public abstract class BaseChatActivity extends AppCompatActivity {
    public TextView contentTextView;
    public EditText input;

    public void onButtonClick() {
        final String inputString = input.getText().toString();
        if (inputString.length() == 0) return;
        sendString(inputString);
        printLineToUI("发送:" + inputString);
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

    public void printLineToUI(String line) {
        runOnUiThread(() -> contentTextView.append(line + "\n"));
    }
}
