package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.axet.audiorecorder.R;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.callback.IClickItemLanguage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.model.LanguageModel;

import java.util.List;

public class LanguageStartAdapter extends RecyclerView.Adapter<LanguageStartAdapter.LangugeViewHolder> {
    private List<LanguageModel> languageModelList;
    private IClickItemLanguage iClickItemLanguage;
    private Context context;
    public LanguageStartAdapter(List<LanguageModel> languageModelList, IClickItemLanguage listener, Context context) {
        this.languageModelList = languageModelList;
        this.iClickItemLanguage = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public LangugeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language_start,parent,false);
        return new LangugeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LangugeViewHolder holder, int position) {
        LanguageModel  languageModel=languageModelList.get(position);
        if(languageModel==null){
            return;
        }
        holder.tvLang.setText(languageModel.getName());
        if(languageModel.getActive()){
            holder.rdbCheck.setChecked(true);
            holder.tvLang.setTextColor(Color.parseColor("#017A5C"));
        }else{
            holder.rdbCheck.setChecked(false);
            holder.tvLang.setTextColor(Color.parseColor("#000000"));
        }
        holder.rdbCheck.setClickable(false);

        if(languageModel.getCode().equals("fr")){
            Glide.with(context).asBitmap()
                    .load(R.drawable.ic_lang_fren)
                    .into(holder.icLang);
        }else if(languageModel.getCode().equals("es")){
            Glide.with(context).asBitmap()
                    .load(R.drawable.ic_lang_span)
                    .into(holder.icLang);
        }else if(languageModel.getCode().equals("de")){
            Glide.with(context).asBitmap()
                    .load(R.drawable.ic_lang_ger)
                    .into(holder.icLang);
        }else if(languageModel.getCode().equals("pt")){
            Glide.with(context).asBitmap()
                    .load(R.drawable.ic_lang_por)
                    .into(holder.icLang);
        }else if(languageModel.getCode().equals("en")){
            Glide.with(context).asBitmap()
                    .load(R.drawable.ic_lang_eng)
                    .into(holder.icLang);
        }else if(languageModel.getCode().equals("it")){
            Glide.with(context).asBitmap()
                    .load(R.drawable.ic_lang_italy)
                    .into(holder.icLang);
        }else if(languageModel.getCode().equals("hi")){
            Glide.with(context).asBitmap()
                    .load(R.drawable.ic_lang_hidin)
                    .into(holder.icLang);
        }

        holder.layoutItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCheck(languageModel.getCode());
                iClickItemLanguage.onClickItemLanguage(languageModel.getCode());
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public int getItemCount() {
        if(languageModelList!=null){
            return languageModelList.size();
        }else{
            return 0;
        }
    }

    public class LangugeViewHolder extends RecyclerView.ViewHolder{
        private RadioButton rdbCheck;
        private TextView tvLang;
        private RelativeLayout layoutItem;
        private ImageView icLang;
        public LangugeViewHolder(@NonNull View itemView) {
            super(itemView);
            rdbCheck = itemView.findViewById(R.id.rdbCheck);
            icLang = itemView.findViewById(R.id.icLang);
            tvLang = itemView.findViewById(R.id.tvLang);
            layoutItem = itemView.findViewById(R.id.layoutItem);
        }
    }
    public void setCheck(String code){
        for(LanguageModel item :languageModelList){
            if(item.getCode().equals(code)){
                item.setActive(true);
            }else{
                item.setActive(false);
            }

        }
        notifyDataSetChanged();
    }
}
