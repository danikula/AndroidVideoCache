package com.danikula.videocache;

import android.provider.BaseColumns;

/**
 * Defines the constants used by the {@link ContentInfoDbHelper).
 *
 * @author Gareth Blake Hall (gbhall.com).
 */
public final class ContentInfoContract {

    public ContentInfoContract() {}

    public static abstract class ContentInfoEntry implements BaseColumns {
        public static final String TABLE_NAME = "ContentInfo";
        public static final String COLUMN_NAME_URL= "url";
        public static final String COLUMN_NAME_MIME = "mime";
        public static final String COLUMN_NAME_LENGTH = "length";
    }
}
