package com.danikula.videocache.sample;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@EActivity(R.layout.activity_menu)
public class MenuActivity extends FragmentActivity {

    @ViewById ListView listView;

    @AfterViews
    void onViewInjected() {
        ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, buildListData());
        listView.setAdapter(adapter);
    }

    @NonNull
    private List<ListEntry> buildListData() {
        return Arrays.asList(
                new ListEntry("Single Video", SingleVideoActivity_.class),
                new ListEntry("Multiple Videos", MultipleVideosActivity_.class),
                new ListEntry("Video Gallery with pre-caching", VideoGalleryActivity_.class),
                new ListEntry("Shared Cache", SharedCacheActivity_.class)
        );
    }

    @ItemClick(R.id.listView)
    void onListItemClicked(int position) {
        ListEntry item = (ListEntry) listView.getAdapter().getItem(position);
        startActivity(new Intent(this, item.activityClass));
    }

    @Click(R.id.cleanCacheButton)
    void onClearCacheButtonClick() {
        try {

            Utils.cleanVideoCacheDir(this);
        } catch (IOException e) {
            Log.e(null, "Error cleaning cache", e);
            Toast.makeText(this, "Error cleaning cache", Toast.LENGTH_LONG).show();
        }
    }

    private static final class ListEntry {

        private final String title;
        private final Class activityClass;

        public ListEntry(String title, Class activityClass) {
            this.title = title;
            this.activityClass = activityClass;
        }

        @Override
        public String toString() {
            return title;
        }
    }

}
