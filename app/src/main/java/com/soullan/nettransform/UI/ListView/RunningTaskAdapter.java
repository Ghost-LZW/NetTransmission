package com.soullan.nettransform.UI.ListView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.soullan.nettransform.Item.RunningTaskItem;
import com.soullan.nettransform.R;
import com.soullan.nettransform.UI.CircleProgressBar;

import java.util.List;

public class RunningTaskAdapter extends ArrayAdapter<RunningTaskItem> {
    private int resourceId;

    public RunningTaskAdapter(@NonNull Context context, int resource, List<RunningTaskItem> objects) {
        super(context, resource, objects);
        resourceId = resource;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        RunningTaskItem item = getItem(position);
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(resourceId, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.statue = convertView.findViewById(R.id.statue);
            viewHolder.fileName = convertView.findViewById(R.id.fileName);
            viewHolder.progressBar = convertView.findViewById(R.id.circleProgressbar);
            viewHolder.progress = convertView.findViewById(R.id.progress_statue);
            convertView.setTag(viewHolder);
        }
        if (item == null) return convertView;
        viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.progressBar.setMaxProgress((int) item.getFileSize());
        viewHolder.progressBar.setProgress((int) item.getProSize());

        viewHolder.progress.setText(item.getProSize() + "/" + item.getFileSize());

        viewHolder.fileName.setText(item.getFileName());
        if (item.getStatue()) viewHolder.statue.setImageResource(R.drawable.ic_stop);
        else viewHolder.statue.setImageResource(R.drawable.ic_run);
        return convertView;
    }

    public class ViewHolder {
        public ImageView statue;
        public TextView fileName;
        public CircleProgressBar progressBar;
        public TextView progress;
    }
}
