package com.soullan.nettransform.UI.ListView;

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

import java.util.List;

public class RunningTaskAdapter extends ArrayAdapter<RunningTaskItem> {
    private int resourceId;

    public RunningTaskAdapter(@NonNull Context context, int resource, List<RunningTaskItem> objects) {
        super(context, resource, objects);
        resourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        RunningTaskItem item = getItem(position);
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        if (convertView == null)
            convertView = layoutInflater.inflate(resourceId, parent, false);
        if (item == null) return convertView;
        ImageView statue = convertView.findViewById(R.id.statue);
        TextView fileName = convertView.findViewById(R.id.fileName);

        fileName.setText(item.getFileName());
        if (item.getStatue()) statue.setImageResource(R.drawable.ic_stop);
        else statue.setImageResource(R.drawable.ic_run);
        return convertView;
    }
}
