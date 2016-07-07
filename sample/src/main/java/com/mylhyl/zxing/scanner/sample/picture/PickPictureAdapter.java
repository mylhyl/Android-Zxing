package com.mylhyl.zxing.scanner.sample.picture;


import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.joanzapata.android.BaseAdapterHelper;
import com.joanzapata.android.QuickAdapter;
import com.mylhyl.zxing.scanner.sample.R;

import java.util.List;

/**
 * 照片浏览
 * Created by hupei on 2016/7/7.
 */
public class PickPictureAdapter extends QuickAdapter<String> {

    public PickPictureAdapter(Context context, List<String> datas) {
        super(context, R.layout.activity_pick_picture_grid_item, datas);
    }

    @Override
    protected void convert(BaseAdapterHelper helper, String item) {
        ImageView imageView = helper.getView(R.id.activity_pick_picture_grid_item_image);
        Glide.with(context).load(item).into(imageView);
    }
}
