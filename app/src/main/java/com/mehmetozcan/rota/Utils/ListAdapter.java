package com.mehmetozcan.rota.Utils;

import android.content.Context;
import android.graphics.Color;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mehmetozcan.rota.MarkerListActivity;
import com.mehmetozcan.rota.R;

import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Created by razor on 2.06.2018.
 */


public class ListAdapter extends BaseAdapter {
    ArrayList<JSONObject> list;

    Context context;
    private static LayoutInflater inflater = null;
    public  ListAdapter(Context activity, ArrayList<JSONObject> list){
        this.context = activity;
        this.list = list;
        inflater = LayoutInflater.from(context);

    }

    @Override
    public int getCount() {
        return this.list.size();
    }

    @Override
    public Object getItem(int position) {
        return this.list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }



    public  class Holder{

        TextView title;
        ImageView drag;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;

        View rowView;
        rowView = inflater.inflate(R.layout.marker_list_item,null);
        Holder holder = new Holder();
        rowView.setTag(holder);

        final JSONObject title = this.list.get(pos);

        holder.drag = (ImageView)rowView.findViewById(R.id.drag);

        holder.drag.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    MarkerListActivity.mSortable = true;
                    MarkerListActivity.mDragString = title;
                    MarkerListActivity.mPosition = -1;
                    notifyDataSetChanged();
                    return true;
                }
                return false;
            }
        });

        if (MarkerListActivity.mDragString != null && MarkerListActivity.mDragString.equals(title)) {
            rowView.setBackgroundColor(context.getResources().getColor(R.color.primary));
        } else {
            rowView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.title = (TextView)rowView.findViewById(R.id.marker_title);
        try{
            holder.title.setText(title.getString("title"));
        }catch (Exception e){

        }



        return rowView;
    }
}