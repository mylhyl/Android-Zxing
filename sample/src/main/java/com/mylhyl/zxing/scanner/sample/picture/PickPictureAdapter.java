package com.mylhyl.zxing.scanner.sample.picture;


import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.mylhyl.cygadapter.CygAdapter;
import com.mylhyl.cygadapter.CygViewHolder;
import com.mylhyl.zxing.scanner.sample.R;

import java.util.List;

/**
 * 照片浏览
 * Created by hupei on 2016/7/7.
 */
class PickPictureAdapter extends CygAdapter<String> {

    public PickPictureAdapter(Context context, List<String> datas) {
        super(context, R.layout.activity_pick_picture_grid_item, datas);
    }

    @Override
    public void onBindData(CygViewHolder viewHolder, String item, int position) {
        ImageView imageView = viewHolder.findViewById(R.id.activity_pick_picture_grid_item_image);
        Glide.with(mContext).load(item).into(imageView);
    }
}
