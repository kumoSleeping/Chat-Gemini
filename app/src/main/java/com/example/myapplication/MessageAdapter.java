package com.example.myapplication;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;
import io.noties.markwon.Markwon;

public class MessageAdapter extends ArrayAdapter<Message> {

    private final Markwon markwon;

    public MessageAdapter(Context context, List<Message> messages) {
        super(context, 0, messages);
        this.markwon = Markwon.create(context); // 初始化 Markdown
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Message message = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_message, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.text_message);
        ImageView userIcon = convertView.findViewById(R.id.user_icon);
        ImageView aiIcon = convertView.findViewById(R.id.ai_icon);

        FrameLayout.LayoutParams textParams = (FrameLayout.LayoutParams) textView.getLayoutParams();
        FrameLayout.LayoutParams userIconParams = (FrameLayout.LayoutParams) userIcon.getLayoutParams();
        FrameLayout.LayoutParams aiIconParams = (FrameLayout.LayoutParams) aiIcon.getLayoutParams();

        assert message != null;
        if (message.isUserMessage()) {
            userIcon.setVisibility(View.VISIBLE);
            aiIcon.setVisibility(View.GONE);
            textParams.gravity = Gravity.END;
            userIconParams.gravity = Gravity.END | Gravity.TOP;
            textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        } else {
            userIcon.setVisibility(View.GONE);
            aiIcon.setVisibility(View.VISIBLE);
            textParams.gravity = Gravity.START;
            aiIconParams.gravity = Gravity.START | Gravity.TOP;
            textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }

        textView.setLayoutParams(textParams);
        userIcon.setLayoutParams(userIconParams);
        aiIcon.setLayoutParams(aiIconParams);

        // 使用 Markdown 渲染 Markdown 内容
        markwon.setMarkdown(textView, message.getContent());

        return convertView;
    }
}
