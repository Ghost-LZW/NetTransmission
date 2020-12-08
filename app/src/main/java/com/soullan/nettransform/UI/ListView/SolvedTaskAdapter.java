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

import com.soullan.nettransform.Item.SolvedTaskItem;
import com.soullan.nettransform.R;

import java.util.List;

public class SolvedTaskAdapter  extends ArrayAdapter<SolvedTaskItem> {
    private int resourceId;

    public SolvedTaskAdapter(@NonNull Context context, int resource, List<SolvedTaskItem> objects) {
        super(context, resource, objects);
        resourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        SolvedTaskItem item = getItem(position);
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        if (convertView == null)
            convertView = layoutInflater.inflate(resourceId, parent, false);
        if (item == null) return convertView;
        ImageView statue = convertView.findViewById(R.id.statue);
        TextView fileName = convertView.findViewById(R.id.fileName);

        fileName.setText(item.getFileName());
        return convertView;
    }
}