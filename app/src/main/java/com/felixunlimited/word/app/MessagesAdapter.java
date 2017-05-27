package com.felixunlimited.word.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

/**
 * {@link MessagesAdapter} exposes a list of message messages
 * from a {@link Cursor} to a {@link android.widget.ListView}.
 */
public class MessagesAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_COUNT = 2;
    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    // Flag to determine if we want to use a separate view for "today".
    private boolean mUseTodayLayout = true;

    /**
     * Cache of the children views for a message list item.
     */
    public static class ViewHolder {
        public final ImageView preacherPicture;
        public final TextView dateView;
        public final TextView preacherView;
        public final TextView titleView;
        public final TextView downloadedView;
        public final TextView priceView;

        public ViewHolder(View view) {
            preacherPicture = (ImageView) view.findViewById(R.id.list_item_preacher_picture);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            preacherView = (TextView) view.findViewById(R.id.list_item_preacher_textview);
            titleView = (TextView) view.findViewById(R.id.list_item_title_textview);
            downloadedView = (TextView) view.findViewById(R.id.list_item_downloaded_textview);
            priceView = (TextView) view.findViewById(R.id.list_item_price_textview);
        }
    }

    public MessagesAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                layoutId = R.layout.message_list_item_latest;
                break;
            }
            case VIEW_TYPE_FUTURE_DAY: {
                layoutId = R.layout.message_list_item;
                break;
            }
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        if (cursor == null)
            return;
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        int viewType = getItemViewType(cursor.getPosition());
        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                // Get message icon
//                viewHolder.preacherPicture.setImageResource(R.drawable.wf);
                break;
            }
            case VIEW_TYPE_FUTURE_DAY: {
                // Get message icon
//                viewHolder.preacherPicture.setImageResource(R.drawable.wf);
                break;
            }
        }

        // Read date from cursor
//        long dateInMillis = cursor.getLong(MessageDetailFragment1.COL_MESSAGE_DATE);
        String date = cursor.getString(MessageDetailFragment.COL_MESSAGE_DATE);
        // Find TextView and set formatted date on it
//        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis));
        viewHolder.dateView.setText(date);

        // Read message message from cursor
        String preacher_name = cursor.getString(MessageDetailFragment.COL_PREACHER_NAME);
        // Find TextView and set message message on it
        viewHolder.preacherView.setText(preacher_name);

        int preacher_id = cursor.getInt(MessageDetailFragment.COL_MESSAGE_PREACHER_KEY);
//        Utility.setPreachericture(viewHolder.preacherPicture, preacher_id, context);
//        viewHolder.preacherPicture.setImageResource(preacher_id);

        // For accessibility, add a content overview to the icon field
        File dir = Utility.choosePreferredDir(context);
//        if (!dir.exists())
//        {
//            dir.mkdirs();
//        }

        String preachersDir = dir.getAbsolutePath();
        String fileName = "p"+preacher_id+".png";
        File preacherFile = new File(preachersDir, fileName);
        Utility.setImage(preachersDir,fileName,viewHolder.preacherPicture);

        viewHolder.preacherPicture.setContentDescription(preacher_name);

        // Read title temperature from cursor
        String title = cursor.getString(MessageDetailFragment.COL_MESSAGE_TITLE);
        viewHolder.titleView.setText(title);

        // Read preacher temperature from cursor
//        String preacher = cursor.getString(MessageDetailFragment1.COL_MESSAGE_PREACHER_KEY);
//        viewHolder.preacherView.setText(preacher);

        int downloaded = cursor.getInt(MessageDetailFragment.COL_MESSAGE_DOWNLOADED);
        if (downloaded == 0)
            viewHolder.downloadedView.setBackgroundColor(context.getResources().getColor(R.color.wordapp_dark_orange));
        else
            viewHolder.downloadedView.setText(" ");
//        viewHolder.downloadedView.setBackgroundColor(255);

        int price = cursor.getInt(MessageDetailFragment.COL_PRICE);
//        viewHolder.priceView.setText(""+price);

        int purchased = cursor.getInt(MessageDetailFragment.COL_MESSAGE_PURCHASED);
        if (purchased == 1)
            viewHolder.priceView.setText("PAID");
//        viewHolder.priceView.setBackgroundColor(context.getResources().getColor(R.color.wordapp_dark_orange));
        else
            viewHolder.priceView.setBackgroundColor(context.getResources().getColor(R.color.black));
    }

    public void setUseLatestLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
}