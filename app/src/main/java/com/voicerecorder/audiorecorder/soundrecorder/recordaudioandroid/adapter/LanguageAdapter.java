package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.axet.audiorecorder.R;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.callback.IClickItemLanguage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.model.LanguageModel;

import java.util.List;


public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LangugeViewHolder> {
    private List<LanguageModel> languageModelList;
    private IClickItemLanguage iClickItemLanguage;

    public LanguageAdapter(List<LanguageModel> languageModelList, IClickItemLanguage listener) {
        this.languageModelList = languageModelList;
        this.iClickItemLanguage = listener;
    }

    @NonNull
    @Override
    public LangugeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
        return new LangugeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LangugeViewHolder holder, int position) {
        LanguageModel languageModel = languageModelList.get(position);
        if (languageModel == null) {
            return;
        }
        holder.txtName.setText(languageModel.getName());
        if (languageModel.getActive()) {
            holder.imgCheck.setVisibility(View.VISIBLE);
        } else {
            holder.imgCheck.setVisibility(View.GONE);
        }

        holder.layoutItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iClickItemLanguage.onClickItemLanguage(languageModel.getCode());
            }
        });

    }

    @Override
    public int getItemCount() {
        if (languageModelList != null) {
            return languageModelList.size();
        } else {
            return 0;
        }
    }

    public class LangugeViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgCheck;
        private TextView txtName;
        private LinearLayout layoutItem;

        public LangugeViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCheck = itemView.findViewById(R.id.imgCheck);
            txtName = itemView.findViewById(R.id.txtName);
            layoutItem = itemView.findViewById(R.id.layoutItem);
        }
    }

    public void setCheck(String code) {
        for (LanguageModel item : languageModelList) {
            if (item.getCode().equals(code)) {
                item.setActive(true);
            } else {
                item.setActive(false);
            }

        }
        notifyDataSetChanged();
    }

}
